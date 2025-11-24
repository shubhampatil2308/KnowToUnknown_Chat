const DEFAULT_AVATAR = '/images/default-avatar.svg';

let currentChatId = null;
let currentChatType = null;
let allUsers = [];
let friends = [];
let groups = [];
let pendingRequests = [];
let sentRequests = [];
let userDirectory = {};
let currentView = 'friends';
let selectedGroup = null;

// Toast notification system for dashboard
function showToast(type, title, message) {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    
    const toast = document.createElement('div');
    toast.className = `toast-notification ${type}`;
    
    const iconMap = {
        success: 'fa-check-circle',
        error: 'fa-exclamation-circle',
        info: 'fa-info-circle'
    };
    
    toast.innerHTML = `
        <i class="fas ${iconMap[type] || iconMap.info}"></i>
        <div class="toast-content">
            <div class="toast-title">${escapeHtml(title)}</div>
            <div class="toast-message">${escapeHtml(message)}</div>
        </div>
        <button class="toast-close" onclick="this.parentElement.remove()">
            <i class="fas fa-times"></i>
        </button>
    `;
    
    container.appendChild(toast);
    
    // Auto remove after 5 seconds
    setTimeout(() => {
        if (toast.parentElement) {
            toast.classList.add('hiding');
            setTimeout(() => toast.remove(), 300);
        }
    }, 5000);
}

document.addEventListener('DOMContentLoaded', function() {
    initDashboard();
    
    // Check for registration success notification
    const urlParams = new URLSearchParams(window.location.search);
    const registered = urlParams.get('registered');
    if (registered === 'true') {
        showToast('success', 'Registration Successful', 'Your registration was successful! Now you can login with your username and password.');
        // Clean URL
        window.history.replaceState({}, document.title, window.location.pathname);
    }
});

function initDashboard() {
    if (!getAuthToken()) {
        window.location.href = '/login.html';
        return;
    }

    loadUserInfo();
    setupMenuItems();
    setupSearch();
    setupMessageInput();

    loadFriends();
    loadFriendRequests();
    loadAllUsers();
    loadGroups();
}

function loadUserInfo() {
    const user = getCurrentUser();
    if (!user) return;

    const nameEl = document.getElementById('userName');
    const statusEl = document.getElementById('userStatus');
    const avatar = document.getElementById('userAvatar');

    if (nameEl) nameEl.textContent = user.name || user.username;
    if (statusEl) statusEl.textContent = user.online ? 'Online' : 'Offline';
    if (avatar) {
        setAvatarElement(avatar, user);
    }
    setTheme(user.theme || 'light');
}

function setupMenuItems() {
    document.querySelectorAll('.menu-item').forEach(item => {
        item.addEventListener('click', () => {
            if (item.dataset.view) {
                document.querySelectorAll('.menu-item').forEach(m => m.classList.remove('active'));
                item.classList.add('active');
                switchView(item.dataset.view);
            }
        });
    });
}

// Debounce function for performance
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

function setupSearch() {
    const searchInput = document.getElementById('searchUsers');
    if (!searchInput) return;
    
    // Debounce search to avoid too many API calls
    const debouncedSearch = debounce((query) => {
        if (!query) {
            if (currentView === 'all-users') displayAllUsers();
            return;
        }
        fetch(`/api/users/search?query=${encodeURIComponent(query)}`, {
            headers: { 'Authorization': 'Bearer ' + getAuthToken() }
        })
            .then(handleJsonResponse)
            .then(data => renderUserList(data))
            .catch(err => console.error('Search error', err));
    }, 300);
    
    searchInput.addEventListener('input', (e) => {
        const query = e.target.value.trim();
        debouncedSearch(query);
    });
}

function setupMessageInput() {
    const messageInput = document.getElementById('messageInput');
    if (!messageInput) return;
    messageInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') sendMessage();
    });
    messageInput.addEventListener('input', () => {
        if (currentChatType === 'user' && currentChatId) {
            const me = getCurrentUser();
            sendTypingIndicator(me.id, currentChatId, true);
            clearTimeout(window.typingTimeout);
            window.typingTimeout = setTimeout(() => {
                sendTypingIndicator(me.id, currentChatId, false);
            }, 1000);
        }
    });
}

function switchView(view) {
    currentView = view;
    const chatMessages = document.getElementById('chatMessages');
    const chatInputArea = document.getElementById('chatInputArea');
    chatMessages.innerHTML = '';
    chatInputArea.style.display = 'none';
    currentChatId = null;
    currentChatType = null;

    if (view === 'friends') {
        displayFriends();
    } else if (view === 'all-users') {
        displayAllUsers();
    } else if (view === 'groups') {
        displayGroups();
    } else if (view === 'profile') {
        showProfileModal();
    }
}

// Cache for API responses
const apiCache = new Map();
const CACHE_DURATION = 30000; // 30 seconds

function getCachedData(key) {
    const cached = apiCache.get(key);
    if (cached && Date.now() - cached.timestamp < CACHE_DURATION) {
        return cached.data;
    }
    return null;
}

function setCachedData(key, data) {
    apiCache.set(key, { data, timestamp: Date.now() });
}

function loadFriends() {
    const cacheKey = 'friends';
    const cached = getCachedData(cacheKey);
    if (cached) {
        friends = cached;
        if (currentView === 'friends') displayFriends();
        return;
    }
    
    fetch('/api/friends/accepted', {
        headers: { 'Authorization': 'Bearer ' + getAuthToken() }
    })
        .then(handleJsonResponse)
        .then(data => {
            friends = data || [];
            setCachedData(cacheKey, friends);
            if (currentView === 'friends') displayFriends();
        })
        .catch(err => console.error('Error loading friends', err));
}

function loadFriendRequests() {
    fetch('/api/friends/pending', {
        headers: { 'Authorization': 'Bearer ' + getAuthToken() }
    })
        .then(handleJsonResponse)
        .then(data => {
            pendingRequests = data || [];
            if (currentView === 'friends') displayFriends();
        })
        .catch(err => console.error('Error loading pending requests', err));

    fetch('/api/friends/sent', {
        headers: { 'Authorization': 'Bearer ' + getAuthToken() }
    })
        .then(handleJsonResponse)
        .then(data => {
            sentRequests = data || [];
            if (currentView === 'friends') displayFriends();
        })
        .catch(err => console.error('Error loading sent requests', err));
}

function buildUserDirectory() {
    userDirectory = {};
    const me = getCurrentUser();
    if (me) userDirectory[me.id] = me;
    allUsers.forEach(user => userDirectory[user.id] = user);
}

function displayFriends() {
    const container = document.getElementById('chatMessages');
    if (!container) return;

    let html = '<div class="friends-section">';
    html += '<div class="section-card"><h6><i class="fas fa-user-friends me-2"></i>Friends</h6>';
    if (!friends.length) {
        html += `<div class="empty-state">
                    <i class="fas fa-user-friends fa-3x mb-3"></i>
                    <p>No friends yet. Send requests from All Users.</p>
                 </div>`;
    } else {
        html += '<div class="list-group">';
        friends.forEach(friend => {
            const friendId = friend.senderId === getCurrentUser().id ? friend.receiverId : friend.senderId;
            const friendUser = userDirectory[friendId];
            const displayName = friendUser?.name || friend.senderName;
            const username = friendUser?.username || friend.senderUsername;
            const isOnline = friendUser?.online || false;
            html += `
                <div class="list-group-item user-list-item">
                    <div class="d-flex align-items-center justify-content-between">
                        <div class="d-flex align-items-center pointer flex-grow-1" onclick="openChat(${friendId}, 'user')">
                            ${getAvatarMarkup(friendUser, displayName)}
                            <div class="ms-3">
                                <div class="d-flex align-items-center">
                                    <div class="fw-bold">${escapeHtml(displayName)}</div>
                                    ${isOnline ? '<span class="badge bg-success ms-2" style="width: 8px; height: 8px; padding: 0; border-radius: 50%;"></span>' : ''}
                                </div>
                                <small class="text-muted">@${escapeHtml(username)}</small>
                            </div>
                        </div>
                        <div class="btn-group ms-3">
                            <button class="btn btn-sm btn-outline-primary" onclick="event.stopPropagation(); openChat(${friendId}, 'user')">
                                <i class="fas fa-comments me-1"></i>Chat
                            </button>
                            <button class="btn btn-sm btn-outline-danger" onclick="event.stopPropagation(); removeFriend(${friendId})">
                                <i class="fas fa-user-slash me-1"></i>Remove
                            </button>
                        </div>
                    </div>
                </div>`;
        });
        html += '</div>';
    }
    html += '</div>';
    html += renderRequestsSection('incoming', pendingRequests);
    html += renderRequestsSection('outgoing', sentRequests);
    html += '</div>';

    container.innerHTML = html;
}

function loadAllUsers() {
    const cacheKey = 'allUsers';
    const cached = getCachedData(cacheKey);
    if (cached) {
        allUsers = cached;
        buildUserDirectory();
        if (currentView === 'all-users') displayAllUsers();
        return;
    }
    
    fetch('/api/users/all', {
        headers: { 'Authorization': 'Bearer ' + getAuthToken() }
    })
        .then(handleJsonResponse)
        .then(data => {
            const me = getCurrentUser();
            allUsers = (data || []).filter(u => me && u.id !== me.id);
            setCachedData(cacheKey, allUsers);
            buildUserDirectory();
            if (currentView === 'all-users') displayAllUsers();
        })
        .catch(err => console.error('Error loading users', err));
}

function displayAllUsers() {
    renderUserList(allUsers);
}

function renderUserList(list) {
    const container = document.getElementById('chatMessages');
    if (!container) return;

    let html = '<div class="section-card"><h6>All Users</h6>';
    if (!list || !list.length) {
        html += `<div class="empty-state">
                    <i class="fas fa-users"></i>
                    <p>No users found.</p>
                 </div>`;
    } else {
        html += '<div class="row g-3">';
        list.forEach(user => {
            const status = getFriendStatus(user.id);
            html += `
                <div class="col-12 col-md-6">
                    <div class="user-card d-flex align-items-center justify-content-between">
                        <div class="d-flex align-items-center">
                            ${getAvatarMarkup(user, user.name || user.username)}
                            <div class="ms-3">
                                <div class="fw-semibold">${escapeHtml(user.name || user.username)}</div>
                                <div class="user-meta">@${escapeHtml(user.username)}</div>
                            </div>
                        </div>
                        ${renderFriendAction(status, user.id)}
                    </div>
                </div>`;
        });
        html += '</div>';
    }
    html += '</div>';
    container.innerHTML = html;
}

function getFriendStatus(userId) {
    const meId = getCurrentUser().id;
    const isFriend = friends.some(f =>
        (f.senderId === meId && f.receiverId === userId) ||
        (f.receiverId === meId && f.senderId === userId)
    );
    if (isFriend) return { status: 'friends' };
    const incoming = pendingRequests.find(r => r.senderId === userId);
    if (incoming) return { status: 'incoming', requestId: incoming.id };
    const outgoing = sentRequests.find(r => r.receiverId === userId);
    if (outgoing) return { status: 'outgoing' };
    return { status: 'none' };
}

function renderFriendAction(status, userId) {
    switch (status.status) {
        case 'friends':
            return `
                <div class="btn-group">
                    <button class="btn btn-sm btn-outline-success" onclick="openChat(${userId}, 'user')">
                        <i class="fas fa-comments"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="removeFriend(${userId})">
                        <i class="fas fa-user-slash"></i>
                    </button>
                </div>`;
        case 'incoming':
            return `
                <div class="btn-group">
                    <button class="btn btn-sm btn-success" onclick="respondToFriendRequest(${status.requestId}, 'accept')">Accept</button>
                    <button class="btn btn-sm btn-outline-secondary" onclick="respondToFriendRequest(${status.requestId}, 'reject')">Decline</button>
                </div>`;
        case 'outgoing':
            return '<span class="badge bg-warning text-dark">Request Sent</span>';
        default:
            return `
                <button class="btn btn-sm btn-primary" onclick="sendFriendRequest(${userId})">
                    <i class="fas fa-user-plus me-1"></i>Add Friend
                </button>`;
    }
}

function loadGroups() {
    fetch('/api/groups/my-groups', {
        headers: { 'Authorization': 'Bearer ' + getAuthToken() }
    })
        .then(handleJsonResponse)
        .then(data => {
            groups = data || [];
            if (currentView === 'groups') displayGroups();
            if (typeof refreshGroupSubscriptions === 'function') {
                refreshGroupSubscriptions(groups.map(g => g.id));
            }
        })
        .catch(err => console.error('Error loading groups', err));
}

function displayGroups() {
    const container = document.getElementById('chatMessages');
    if (!container) return;

    let html = `
        <div class="d-flex justify-content-end mb-3">
            <button class="btn btn-primary" onclick="showCreateGroupModal()">
                <i class="fas fa-plus me-2"></i>Create Group
            </button>
        </div>`;

    if (!groups.length) {
        html += `<div class="empty-state">
                    <i class="fas fa-layer-group"></i>
                    <p>You have not joined any groups yet.</p>
                 </div>`;
    } else {
        html += '<div class="list-group">';
        groups.forEach(group => {
            const avatarStyle = group.groupImageBase64
                ? `style="background-image:url('data:${group.groupImageType};base64,${group.groupImageBase64}')"`
                : `style="background-image:url('${DEFAULT_AVATAR}')"`
            html += `
                <div class="list-group-item user-list-item">
                    <div class="d-flex alignments-center justify-content-between">
                        <div class="d-flex align-items-center pointer" onclick="openChat(${group.id}, 'group')">
                            <div class="user-avatar me-3" ${avatarStyle}></div>
                            <div>
                                <div class="fw-bold">${escapeHtml(group.name)}</div>
                                <small class="text-muted">${escapeHtml(group.description || 'No description')}</small>
                            </div>
                        </div>
                        <div class="btn-group">
                            <button class="btn btn-sm btn-outline-primary" onclick="openChat(${group.id}, 'group')">
                                <i class="fas fa-comments"></i>
                            </button>
                            <button class="btn btn-sm btn-outline-secondary" onclick="openGroupManager(${group.id})">
                                <i class="fas fa-users-cog"></i>
                            </button>
                        </div>
                    </div>
                </div>`;
        });
        html += '</div>';
    }

    container.innerHTML = html;
}


function openChat(id, type, name) {
    currentChatId = id;
    currentChatType = type;

    let displayName = name;
    if (!displayName) {
        if (type === 'user') {
            const user = userDirectory[id];
            displayName = user?.name || user?.username || 'Chat';
        } else if (type === 'group') {
            const group = groups.find(g => g.id === id);
            displayName = group?.name || 'Group Chat';
        }
    }

    document.getElementById('chatHeaderContent').innerHTML = `
        <h5>${escapeHtml(displayName || '')}</h5>
    `;

    document.getElementById('chatInputArea').style.display = 'block';

    if (type === 'user') {
        loadConversation(id);
    } else if (type === 'group') {
        loadGroupMessages(id);
    }
}

function loadConversation(userId) {
    fetch(`/api/chat/conversation/${userId}`, {
        headers: {
            'Authorization': 'Bearer ' + getAuthToken()
        }
    })
    .then(handleJsonResponse)
    .then(messages => {
        displayMessages(messages);
        markConversationAsRead(userId);
    })
    .catch(error => console.error('Error loading conversation:', error));
}

function loadGroupMessages(groupId) {
    fetch(`/api/groups/${groupId}/messages`, {
        headers: {
            'Authorization': 'Bearer ' + getAuthToken()
        }
    })
    .then(handleJsonResponse)
    .then(messages => displayGroupMessages(messages))
    .catch(error => console.error('Error loading group messages:', error));
}

function displayMessages(messages) {
    const chatMessages = document.getElementById('chatMessages');
    chatMessages.innerHTML = '';
    
    const currentUserId = getCurrentUser().id;
    
    if (!messages || messages.length === 0) {
        chatMessages.innerHTML = `
            <div class="text-center text-muted mt-5">
                <i class="fas fa-comments fa-3x mb-3"></i>
                <p>No messages yet. Start the conversation!</p>
            </div>
        `;
        return;
    }
    
    messages.forEach(message => {
        const isSent = message.senderId === currentUserId;
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${isSent ? 'sent' : ''}`;
        
        let content = '';
        if (message.type === 'IMAGE') {
            content = `<img src="data:${message.mediaType};base64,${message.mediaDataBase64}" class="lazy-load" style="max-width: 300px; border-radius: 10px; cursor: pointer;" onclick="window.open(this.src, '_blank')" onload="this.classList.add('loaded')">`;
        } else if (message.type === 'FILE') {
            content = `<a href="data:${message.mediaType};base64,${message.mediaDataBase64}" download="${message.fileName}" class="text-decoration-none">
                <i class="fas fa-file me-2"></i>${escapeHtml(message.fileName)}
            </a>`;
        } else {
            content = escapeHtml(message.content);
        }
        
        messageDiv.innerHTML = `
            <div class="message-content">
                ${content}
                <div class="mt-1" style="font-size: 0.75rem; opacity: 0.7;">
                    ${new Date(message.timestamp).toLocaleTimeString()}
                    ${isSent && message.isRead ? '<i class="fas fa-check-double ms-1"></i>' : isSent ? '<i class="fas fa-check ms-1"></i>' : ''}
                </div>
            </div>
        `;
        
        chatMessages.appendChild(messageDiv);
    });
    
    // Smooth scroll to bottom
    setTimeout(() => {
        chatMessages.scrollTo({
            top: chatMessages.scrollHeight,
            behavior: 'smooth'
        });
    }, 100);
}

function displayGroupMessages(messages) {
    const chatMessages = document.getElementById('chatMessages');
    chatMessages.innerHTML = '';
    
    const currentUserId = getCurrentUser().id;
    
    messages.forEach(message => {
        const isSent = message.senderId === currentUserId;
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${isSent ? 'sent' : ''}`;
        
        let content = '';
        if (message.type === 'IMAGE') {
            content = `<img src="data:${message.mediaType};base64,${message.mediaDataBase64}" style="max-width: 300px; border-radius: 10px;">`;
        } else if (message.type === 'FILE') {
            content = `<a href="data:${message.mediaType};base64,${message.mediaDataBase64}" download="${message.fileName}">
                <i class="fas fa-file me-2"></i>${message.fileName}
            </a>`;
        } else {
            content = message.content;
        }
        
        messageDiv.innerHTML = `
            <div class="message-content">
                ${!isSent ? `<div style="font-weight: bold; margin-bottom: 5px;">${message.senderName}</div>` : ''}
                ${content}
                <div class="mt-1" style="font-size: 0.75rem; opacity: 0.7;">
                    ${new Date(message.timestamp).toLocaleTimeString()}
                </div>
            </div>
        `;
        
        chatMessages.appendChild(messageDiv);
    });
    
    // Smooth scroll to bottom
    setTimeout(() => {
        chatMessages.scrollTo({
            top: chatMessages.scrollHeight,
            behavior: 'smooth'
        });
    }, 100);
}

function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    
    if (!content || !currentChatId) return;
    
    const user = getCurrentUser();
    
    if (currentChatType === 'user') {
        sendMessageViaWebSocket(user.id, currentChatId, content, 'TEXT');
        
        // Add message to UI immediately
        const chatMessages = document.getElementById('chatMessages');
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message sent';
        messageDiv.innerHTML = `
            <div class="message-content">
                ${content}
                <div class="mt-1" style="font-size: 0.75rem; opacity: 0.7;">
                    ${new Date().toLocaleTimeString()} ✓
                </div>
            </div>
        `;
        chatMessages.appendChild(messageDiv);
        // Smooth scroll to bottom
        setTimeout(() => {
            chatMessages.scrollTo({
                top: chatMessages.scrollHeight,
                behavior: 'smooth'
            });
        }, 100);
    } else if (currentChatType === 'group') {
        sendGroupMessageViaWebSocket(currentChatId, user.id, content, 'TEXT');
    }
    
    input.value = '';
}

function onMessageReceived(message) {
    if (currentChatType === 'user' && currentChatId && 
        (message.senderId === currentChatId || message.receiverId === currentChatId)) {
        loadConversation(currentChatId);
    }
}

function onGroupMessageReceived(groupId, messages) {
    if (currentChatType === 'group' && currentChatId === groupId) {
        displayGroupMessages(messages);
    }
}

function onTypingReceived(typingData) {
    if (currentChatType === 'user' && currentChatId === typingData.senderId) {
        const indicator = document.getElementById('typingIndicator');
        if (typingData.isTyping) {
            indicator.textContent = 'Typing...';
            indicator.style.display = 'block';
        } else {
            indicator.style.display = 'none';
        }
    }
}

function markConversationAsRead(userId) {
    fetch(`/api/chat/read/${userId}`, {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + getAuthToken()
        }
    })
    .catch(error => console.error('Error marking as read:', error));
}

function sendFriendRequest(userId) {
    const formData = new FormData();
    formData.append('receiverId', userId);
    
    fetch('/api/friends/request', {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + getAuthToken()
        },
        body: formData
    })
    .then(handleJsonResponse)
        .then(() => {
            showToast('success', 'Friend Request Sent', 'Your friend request has been sent successfully!');
            // Clear cache to force refresh
            apiCache.delete('friends');
            apiCache.delete('allUsers');
            loadFriendRequests();
            loadAllUsers();
        })
    .catch(error => showToast('error', 'Request Failed', error.message || 'Error sending friend request'));
}

function removeFriend(friendId) {
    if (!confirm('Remove this friend?')) return;
    fetch(`/api/friends/remove/${friendId}`, {
        method: 'DELETE',
        headers: { 'Authorization': 'Bearer ' + getAuthToken() }
    })
        .then(handleJsonResponse)
        .then(() => {
            showToast('success', 'Friend Removed', 'Friend has been removed successfully.');
            // Clear cache to force refresh
            apiCache.delete('friends');
            apiCache.delete('allUsers');
            loadFriends();
            loadAllUsers();
        })
        .catch(error => showToast('error', 'Remove Failed', error.message || 'Unable to remove friend'));
}

function showCreateGroupModal() {
    const modal = new bootstrap.Modal(document.getElementById('createGroupModal'));
    modal.show();
}

function createGroup() {
    const formData = new FormData();
    formData.append('name', document.getElementById('groupName').value);
    formData.append('description', document.getElementById('groupDescription').value);
    
    const groupImage = document.getElementById('groupImage');
    if (groupImage.files.length > 0) {
        formData.append('image', groupImage.files[0]);
    }
    
    fetch('/api/groups/create', {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + getAuthToken() },
        body: formData
    })
        .then(handleJsonResponse)
        .then(() => {
            bootstrap.Modal.getInstance(document.getElementById('createGroupModal')).hide();
            showToast('success', 'Group Created', 'Your group has been created successfully!');
            loadGroups();
        })
        .catch(error => showToast('error', 'Creation Failed', error.message || 'Error creating group'));
}

function showProfileModal() {
    const user = getCurrentUser();
    document.getElementById('profileName').value = user.name || '';
    document.getElementById('profileEmail').value = user.email || '';
    document.getElementById('profilePhone').value = user.phone || '';
    document.getElementById('profileStatus').value = user.status || '';
    document.getElementById('profileTheme').value = user.theme || 'light';
    
    const preview = document.getElementById('profilePicturePreview');
    preview.src = user.profilePictureBase64
        ? `data:${user.profilePictureType};base64,${user.profilePictureBase64}`
        : DEFAULT_AVATAR;
    
    const modal = new bootstrap.Modal(document.getElementById('profileModal'));
    modal.show();
}

function previewProfilePicture() {
    const file = document.getElementById('profilePictureInput').files[0];
    if (file) {
        // Validate file size (max 5MB)
        if (file.size > 5 * 1024 * 1024) {
            showToast('error', 'File Too Large', 'Please select an image smaller than 5MB.');
            document.getElementById('profilePictureInput').value = '';
            return;
        }
        
        // Validate file type
        if (!file.type.startsWith('image/')) {
            showToast('error', 'Invalid File', 'Please select a valid image file.');
            document.getElementById('profilePictureInput').value = '';
            return;
        }
        
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('profilePicturePreview');
            preview.src = e.target.result;
            preview.classList.add('loaded');
            showToast('info', 'Image Selected', 'Click "Save Changes" to update your profile picture.');
        };
        reader.readAsDataURL(file);
    }
}

async function saveProfile() {
    const name = document.getElementById('profileName').value.trim();
    const email = document.getElementById('profileEmail').value.trim();
    const phone = document.getElementById('profilePhone').value.trim();
    const status = document.getElementById('profileStatus').value.trim();
    const theme = document.getElementById('profileTheme').value;
    const profilePicture = document.getElementById('profilePictureInput');
    
    // Find save button
    const saveBtn = document.querySelector('#profileModal .btn-primary');
    const originalText = saveBtn.innerHTML;

    // Validate email
    if (email && !email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
        showToast('error', 'Invalid Email', 'Please enter a valid email address.');
        return;
    }

    // Show loading state
    saveBtn.disabled = true;
    saveBtn.innerHTML = '<span class="loading-spinner"></span> Saving...';

    try {
        if (profilePicture.files.length > 0) {
            const photoForm = new FormData();
            photoForm.append('file', profilePicture.files[0]);
            const photoResp = await fetch('/api/users/profile/picture', {
                method: 'POST',
                headers: { 'Authorization': 'Bearer ' + getAuthToken() },
                body: photoForm
            });
            const updatedWithPhoto = await handleJsonResponse(photoResp);
            setCurrentUser(updatedWithPhoto);
        }

        const response = await fetch('/api/users/profile', {
            method: 'PUT',
            headers: {
                'Authorization': 'Bearer ' + getAuthToken(),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ name, email, phone, status, theme })
        });
        const updated = await handleJsonResponse(response);
        setCurrentUser(updated);
        loadUserInfo();
        setTheme(updated.theme || 'light');
        bootstrap.Modal.getInstance(document.getElementById('profileModal')).hide();
        
        // Show success notification
        if (typeof showToast === 'function') {
            showToast('success', 'Profile Updated', 'Your profile has been updated successfully!');
        }
    } catch (error) {
        if (typeof showToast === 'function') {
            showToast('error', 'Update Failed', error.message || 'Error updating profile');
        } else {
            alert(error.message || 'Error updating profile');
        }
    } finally {
        saveBtn.disabled = false;
        saveBtn.innerHTML = originalText;
    }
}

function attachFile() {
    document.getElementById('fileInput').click();
}

function handleFileSelect() {
    const file = document.getElementById('fileInput').files[0];
    if (!file || !currentChatId) return;
    
    const formData = new FormData();
    formData.append('file', file);
    
    const user = getCurrentUser();
    const type = file.type.startsWith('image/') ? 'IMAGE' : 'FILE';
    
    if (currentChatType === 'user') {
        fetch(`/api/chat/send/${currentChatId}`, {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + getAuthToken()
            },
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.id) {
                loadConversation(currentChatId);
            }
        });
    }
}

function searchUsers(query) {
    if (!query) {
        if (currentView === 'all-users') {
            displayAllUsers();
        }
        return;
    }
    
    fetch(`/api/users/search?query=${encodeURIComponent(query)}`, {
        headers: {
            'Authorization': 'Bearer ' + getAuthToken()
        }
    })
    .then(handleJsonResponse)
    .then(data => renderUserList(data))
    .catch(error => console.error('Error searching users:', error));
}

function toggleEmojiPicker() {
    // Simple emoji picker implementation
    alert('Emoji picker - to be implemented');
}

// Command palette
document.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        toggleCommandPalette(true);
    } else if (e.key === 'Escape') {
        toggleCommandPalette(false);
    }
});

function toggleCommandPalette(forceOpen) {
    const palette = document.getElementById('commandPalette');
    const input = document.getElementById('commandInput');
    if (!palette) return;
    const shouldOpen = forceOpen ?? !palette.classList.contains('active');
    if (shouldOpen) {
        palette.classList.add('active');
        input.value = '';
        renderCommandResults(allUsers.slice(0, 5));
        setTimeout(() => input.focus(), 50);
    } else {
        palette.classList.remove('active');
    }
}

const commandInput = document.getElementById('commandInput');
if (commandInput) {
    commandInput.addEventListener('input', (e) => {
        const query = e.target.value.toLowerCase();
        const matches = allUsers.filter(user =>
            user.name.toLowerCase().includes(query) ||
            user.username.toLowerCase().includes(query)
        ).slice(0, 6);
        renderCommandResults(matches);
    });
}

function renderCommandResults(users) {
    const results = document.getElementById('commandResults');
    if (!results) return;
    if (!users.length) {
        results.innerHTML = '<p class="text-muted mb-0">No matches.</p>';
        return;
    }
    results.innerHTML = users.map(user => `
        <div class="command-item" onclick="startDirectChat(${user.id})">
            <div class="d-flex align-items-center">
                ${getAvatarMarkup(user, user.name || user.username)}
                <div>
                    <div class="fw-bold">${escapeHtml(user.name || user.username)}</div>
                    <small class="text-muted">@${escapeHtml(user.username)}</small>
                </div>
            </div>
            <span class="text-uppercase text-muted" style="font-size: 0.75rem;">Open chat</span>
        </div>
    `).join('');
}

function startDirectChat(userId) {
    const user = userDirectory[userId];
    if (!user) return;
    toggleCommandPalette(false);
    openChat(userId, 'user');
}

function renderRequestsSection(type, requests) {
    const title = type === 'incoming' ? 'Incoming Requests' : 'Sent Requests';
    let html = `<div class="section-card"><h6>${title}</h6>`;
    if (!requests.length) {
        html += `<p class="text-muted mb-0">No ${type === 'incoming' ? 'pending' : 'sent'} requests.</p></div>`;
        return html;
    }

    requests.forEach(request => {
        const relatedUserId = type === 'incoming' ? request.senderId : request.receiverId;
        const relatedUser = userDirectory[relatedUserId];
        const displayName = relatedUser?.name || (type === 'incoming' ? request.senderName : request.receiverName);
        const username = relatedUser?.username || (type === 'incoming' ? request.senderUsername : request.receiverUsername);

        html += `
            <div class="request-card d-flex align-items-center justify-content-between">
                <div class="d-flex align-items-center">
                    ${getAvatarMarkup(relatedUser, displayName)}
                    <div>
                        <div class="fw-bold">${escapeHtml(displayName)}</div>
                        <small class="text-muted">@${escapeHtml(username)}</small>
                    </div>
                </div>
                ${type === 'incoming'
                    ? `<div class="actions">
                        <button class="btn btn-sm btn-success" onclick="respondToFriendRequest(${request.id}, 'accept')">Accept</button>
                        <button class="btn btn-sm btn-outline-secondary" onclick="respondToFriendRequest(${request.id}, 'reject')">Decline</button>
                       </div>`
                    : '<span class="badge bg-warning text-dark">Waiting</span>'}
            </div>`;
    });

    html += '</div>';
    return html;
}

function respondToFriendRequest(requestId, action) {
    fetch(`/api/friends/${action}/${requestId}`, {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + getAuthToken() }
    })
        .then(handleJsonResponse)
        .then(() => {
            const message = action === 'accept' ? 'Friend request accepted!' : 'Friend request declined.';
            showToast('success', 'Request Updated', message);
            // Clear cache to force refresh
            apiCache.delete('friends');
            loadFriends();
            loadFriendRequests();
        })
        .catch(error => showToast('error', 'Update Failed', error.message || 'Unable to update request'));
}

function openGroupManager(groupId) {
    selectedGroup = groupId;
    fetch(`/api/groups/${groupId}`, {
        headers: { 'Authorization': 'Bearer ' + getAuthToken() }
    })
        .then(handleJsonResponse)
        .then(group => populateGroupManager(group))
        .catch(error => alert(error.message || 'Unable to load group'));
}

function populateGroupManager(group) {
    const modal = new bootstrap.Modal(document.getElementById('manageGroupModal'));
    document.getElementById('manageGroupTitle').textContent = `Manage ${group.name}`;
    const memberContainer = document.getElementById('groupMembersContainer');
    memberContainer.innerHTML = '';
    if (!group.members || !group.members.length) {
        document.getElementById('groupMembersEmpty').style.display = 'block';
    } else {
        document.getElementById('groupMembersEmpty').style.display = 'none';
        group.members.forEach(member => {
            memberContainer.innerHTML += `
                <div class="group-member-pill">
                    ${getAvatarMarkup(member, member.name || member.username)}
                    <span>${escapeHtml(member.name || member.username)}</span>
                    ${member.id !== getCurrentUser().id ? `<button class="remove-btn" onclick="removeMemberFromGroup(${selectedGroup}, ${member.id})">&times;</button>` : ''}
                </div>`;
        });
    }

    const select = document.getElementById('groupMemberSelect');
    select.innerHTML = '<option value="">Select user</option>';
    allUsers
        .filter(user => !group.members.find(m => m.id === user.id))
        .forEach(user => {
            select.innerHTML += `<option value="${user.id}">${escapeHtml(user.name || user.username)}</option>`;
        });

    modal.show();
}

function addMemberToGroup() {
    const userId = document.getElementById('groupMemberSelect').value;
    if (!userId || !selectedGroup) return;
    fetch(`/api/groups/${selectedGroup}/members?userId=${userId}`, {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + getAuthToken() }
    })
        .then(handleJsonResponse)
        .then(() => openGroupManager(selectedGroup))
        .catch(error => alert(error.message || 'Unable to add member'));
}

function removeMemberFromGroup(groupId, userId) {
    fetch(`/api/groups/${groupId}/members/${userId}`, {
        method: 'DELETE',
        headers: { 'Authorization': 'Bearer ' + getAuthToken() }
    })
        .then(handleJsonResponse)
        .then(() => openGroupManager(groupId))
        .catch(error => alert(error.message || 'Unable to remove member'));
}

function handleJsonResponse(response) {
    if (response.ok) return response.json();
    return response.json()
        .catch(() => ({}))
        .then(data => {
            throw new Error(data.error || 'Request failed');
        });
}

function setAvatarElement(element, entity) {
    if (!element) return;
    if (entity && entity.profilePictureBase64) {
        element.style.backgroundImage = `url('data:${entity.profilePictureType};base64,${entity.profilePictureBase64}')`;
        element.classList.remove('default-avatar');
        element.textContent = '';
    } else {
        element.style.backgroundImage = `url('${DEFAULT_AVATAR}')`;
        element.classList.add('default-avatar');
        element.textContent = '';
    }
}

function getAvatarMarkup(entity, text) {
    if (entity && entity.profilePictureBase64) {
        return `<div class="user-avatar me-3 lazy-load" style="background-image:url('data:${entity.profilePictureType};base64,${entity.profilePictureBase64}')" onload="this.classList.add('loaded')"></div>`;
    }
    const initial = (text || '?').charAt(0).toUpperCase();
    return `<div class="user-avatar me-3 default-avatar"><span>${initial}</span></div>`;
}

function escapeHtml(text) {
    if (!text) return '';
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function deleteAccount() {
    const confirmMessage = 'Are you sure you want to delete your account?\n\n' +
        'This will permanently delete:\n' +
        '• All your messages\n' +
        '• All your friend connections\n' +
        '• All your group memberships\n' +
        '• Your account and all data\n\n' +
        'This action CANNOT be undone!\n\n' +
        'Type "DELETE" to confirm:';
    
    const userInput = prompt(confirmMessage);
    
    if (userInput !== 'DELETE') {
        if (userInput !== null) {
            showToast('info', 'Cancelled', 'Account deletion cancelled.');
        }
        return;
    }
    
    // Final confirmation
    if (!confirm('Final confirmation: Are you absolutely sure you want to delete your account? This cannot be undone!')) {
        return;
    }
    
    fetch('/api/users/account', {
        method: 'DELETE',
        headers: {
            'Authorization': 'Bearer ' + getAuthToken()
        }
    })
    .then(response => {
        if (response.ok) {
            return response.json();
        }
        return response.json().then(data => {
            throw new Error(data.error || 'Failed to delete account');
        });
    })
    .then(() => {
        // Clear all local storage
        localStorage.removeItem('authToken');
        localStorage.removeItem('currentUser');
        
        // Show success message and redirect
        showToast('success', 'Account Deleted', 'Your account has been deleted successfully.');
        
        setTimeout(() => {
            window.location.href = '/index.html?accountDeleted=true';
        }, 2000);
    })
    .catch(error => {
        showToast('error', 'Deletion Failed', error.message || 'Failed to delete account. Please try again.');
    });
}

// Mobile menu toggle
function toggleMobileMenu() {
    const sidebar = document.getElementById('sidebar');
    if (sidebar) {
        sidebar.classList.toggle('open');
    }
}

// Close mobile menu when clicking outside
document.addEventListener('click', function(event) {
    const sidebar = document.getElementById('sidebar');
    const toggle = document.getElementById('mobileMenuToggle');
    
    if (sidebar && toggle && window.innerWidth <= 768) {
        if (!sidebar.contains(event.target) && !toggle.contains(event.target) && sidebar.classList.contains('open')) {
            sidebar.classList.remove('open');
        }
    }
});

// Close mobile menu when clicking on menu item
document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.menu-item').forEach(item => {
        item.addEventListener('click', function() {
            if (window.innerWidth <= 768) {
                const sidebar = document.getElementById('sidebar');
                if (sidebar) {
                    sidebar.classList.remove('open');
                }
            }
        });
    });
});


