// Authentication Management
let currentUser = null;
let authToken = null;
const DEFAULT_AVATAR_URL = '/images/default-avatar.svg';

function initAuth() {
    authToken = localStorage.getItem('authToken');
    if (authToken) {
        validateToken();
    }
}

function validateToken() {
    if (!authToken) return;
    
    fetch('/api/auth/validate', {
        headers: {
            'Authorization': 'Bearer ' + authToken
        }
    })
    .then(response => response.json())
    .then(data => {
            if (data.error) {
                logout();
            } else {
                setCurrentUser(data);
                if (window.location.pathname === '/login.html' || window.location.pathname === '/register.html') {
                    window.location.href = '/dashboard.html';
                }
            }
    })
    .catch(() => {
        logout();
    });
}

// Login
document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', function(e) {
            e.preventDefault();
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            
            const formData = new FormData();
            formData.append('username', username);
            formData.append('password', password);
            
            fetch('/api/auth/login', {
                method: 'POST',
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                if (data.error) {
                    showError('loginForm', data.error);
                } else {
                    authToken = data.token;
                    localStorage.setItem('authToken', authToken);
                    setCurrentUser(data.user);
                    window.location.href = '/dashboard.html';
                }
            })
            .catch(error => {
                showError('loginForm', 'Login failed. Please try again.');
            });
        });
    }

    // Register
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', function(e) {
            e.preventDefault();
            const formData = new FormData();
            formData.append('name', document.getElementById('name').value);
            formData.append('username', document.getElementById('username').value);
            formData.append('email', document.getElementById('email').value);
            formData.append('password', document.getElementById('password').value);
            formData.append('phone', document.getElementById('phone').value || '');
            
            const profilePicture = document.getElementById('profilePicture');
            if (profilePicture.files.length > 0) {
                formData.append('profilePicture', profilePicture.files[0]);
            }
            
            fetch('/api/auth/register', {
                method: 'POST',
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                if (data.error) {
                    showError('registerForm', data.error);
                    // Show failure notification
                    showToast('error', 'Registration Unsuccessful', data.error || 'Registration failed. Please try again.');
                } else {
                    // Clear any stored auth data - user needs to login manually
                    localStorage.removeItem('authToken');
                    localStorage.removeItem('currentUser');
                    // Show success notification
                    showToast('success', 'Registration Successful', 'Your registration was successful! A confirmation email has been sent. Please login with your username and password.');
                    // Redirect to home page (index.html) - user must login manually
                    setTimeout(() => {
                        window.location.href = '/index.html?registered=true';
                    }, 3000);
                }
            })
            .catch(error => {
                showError('registerForm', 'Registration failed. Please try again.');
                showToast('error', 'Registration Unsuccessful', 'Registration failed. Please try again.');
            });
        });
    }
    
    initAuth();
});

function showError(formId, message) {
    const errorDiv = document.getElementById('errorMessage');
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    }
}

function showSuccess(message) {
    const successDiv = document.getElementById('successMessage');
    if (successDiv) {
        successDiv.textContent = message;
        successDiv.style.display = 'block';
    }
}

// Toast Notification System
function showToast(type, title, message) {
    const container = document.getElementById('toastContainer') || createToastContainer();
    
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

function createToastContainer() {
    const container = document.createElement('div');
    container.id = 'toastContainer';
    container.className = 'toast-container';
    document.body.appendChild(container);
    return container;
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function logout() {
    localStorage.removeItem('authToken');
    authToken = null;
    setCurrentUser(null);
    // Redirect to home page with logout notification
    window.location.href = '/index.html?logout=true';
}

function getAuthToken() {
    return localStorage.getItem('authToken');
}

function getCurrentUser() {
    if (!currentUser) {
        const stored = localStorage.getItem('currentUser');
        if (stored) {
            currentUser = JSON.parse(stored);
        }
    }
    return currentUser;
}

function setCurrentUser(user) {
    currentUser = user || null;
    if (user) {
        localStorage.setItem('currentUser', JSON.stringify(user));
    } else {
        localStorage.removeItem('currentUser');
    }
}


