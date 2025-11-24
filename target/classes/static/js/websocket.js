// WebSocket/STOMP Client
let stompClient = null;
let connected = false;
let groupSubscriptions = {};
let pendingGroupIds = [];

function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        connected = true;
        console.log('Connected: ' + frame);
        
        const user = getCurrentUser();
        if (user) {
            // Subscribe to personal message queue
            stompClient.subscribe('/user/' + user.id + '/queue/messages', function(message) {
                const messageData = JSON.parse(message.body);
                handleIncomingMessage(messageData);
            });
            
            // Subscribe to typing indicator
            stompClient.subscribe('/user/' + user.id + '/queue/typing', function(typing) {
                const typingData = JSON.parse(typing.body);
                handleTypingIndicator(typingData);
            });
        }
        if (pendingGroupIds.length > 0) {
            subscribeToGroupChannels(pendingGroupIds);
            pendingGroupIds = [];
        }
    }, function(error) {
        console.log('WebSocket connection error: ' + error);
        connected = false;
        setTimeout(connectWebSocket, 5000); // Retry after 5 seconds
    });
}

function disconnectWebSocket() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    connected = false;
}

function sendMessageViaWebSocket(senderId, receiverId, content, type = 'TEXT') {
    if (stompClient && connected) {
        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify({
            senderId: senderId,
            receiverId: receiverId,
            content: content,
            type: type
        }));
    }
}

function sendGroupMessageViaWebSocket(groupId, senderId, content, type = 'TEXT') {
    if (stompClient && connected) {
        stompClient.send("/app/chat.sendGroupMessage", {}, JSON.stringify({
            groupId: groupId,
            senderId: senderId,
            content: content,
            type: type
        }));
    }
}

function sendTypingIndicator(senderId, receiverId, isTyping) {
    if (stompClient && connected) {
        stompClient.send("/app/chat.typing", {}, JSON.stringify({
            senderId: senderId,
            receiverId: receiverId,
            isTyping: isTyping
        }));
    }
}

function handleIncomingMessage(message) {
    if (typeof onMessageReceived === 'function') {
        onMessageReceived(message);
    }
}

function handleTypingIndicator(typingData) {
    if (typeof onTypingReceived === 'function') {
        onTypingReceived(typingData);
    }
}

function subscribeToGroupChannels(groupIds) {
    if (!stompClient || !connected) {
        pendingGroupIds = groupIds;
        return;
    }
    Object.values(groupSubscriptions).forEach(sub => sub.unsubscribe());
    groupSubscriptions = {};
    groupIds.forEach(id => {
        groupSubscriptions[id] = stompClient.subscribe(`/topic/group/${id}`, function(message) {
            const payload = JSON.parse(message.body);
            if (typeof onGroupMessageReceived === 'function') {
                onGroupMessageReceived(id, payload);
            }
        });
    });
}

window.refreshGroupSubscriptions = function(groupIds) {
    subscribeToGroupChannels(groupIds);
};

// Connect when page loads
window.addEventListener('load', function() {
    if (getAuthToken()) {
        connectWebSocket();
    }
});

// Disconnect when page unloads
window.addEventListener('beforeunload', function() {
    disconnectWebSocket();
});


