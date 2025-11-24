# Real-Time Chat Application

A professional-grade real-time chat application built with Spring Boot, featuring one-to-one messaging, group chats, friend requests, and a modern dark/light theme UI.

## 🚀 Features

- **Real-Time Messaging**: WebSocket/STOMP based instant messaging
- **One-to-One Chat**: Private conversations between friends
- **Group Chat**: Create and manage group conversations
- **Friend System**: Send, accept, and reject friend requests
- **User Authentication**: Secure login with BCrypt password encryption
- **Google OAuth2**: Sign in with Google option
- **Dark/Light Theme**: Toggle between themes with user preference storage
- **Media Sharing**: Send images and files in chats
- **Online Status**: See who's online in real-time
- **Message History**: Persistent message storage in MySQL
- **Modern UI**: Beautiful, responsive design with animations

## 🛠️ Technologies

### Backend
- Java 21
- Spring Boot 3.2.0
- Spring Security
- Spring WebSocket/STOMP
- Spring Data JPA
- MySQL
- JWT Authentication
- BCrypt Password Encoding
- Google OAuth2

### Frontend
- HTML5, CSS3, JavaScript
- Bootstrap 5.3
- Font Awesome Icons
- SockJS & STOMP.js for WebSocket

## 📋 Prerequisites

- Java 21 or higher
- Maven 3.6+
- MySQL 8.0+
- Google OAuth2 Credentials (for Google Sign-In)

## 🔧 Setup Instructions

### 1. Database Setup

Create a MySQL database:

```sql
CREATE DATABASE chatapp_db;
```

Update `src/main/resources/application.properties` with your MySQL credentials:

```properties
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 2. Google OAuth2 Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable Google+ API
4. Create OAuth 2.0 credentials
5. Add authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`
6. Update `application.properties`:

```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET
```

### 3. Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/chatapp/
│   │   ├── config/          # Security, WebSocket configurations
│   │   ├── controller/      # REST and WebSocket controllers
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── entity/         # JPA entities
│   │   ├── repository/     # JPA repositories
│   │   ├── service/        # Business logic
│   │   └── util/           # Utility classes
│   └── resources/
│       ├── static/         # Frontend files
│       │   ├── css/        # Stylesheets
│       │   ├── js/         # JavaScript files
│       │   ├── dashboard.html
│       │   ├── index.html
│       │   ├── login.html
│       │   └── register.html
│       └── application.properties
└── pom.xml
```

## 🎯 Usage

1. **Register**: Create a new account at `/register.html`
2. **Login**: Sign in at `/login.html` or use Google OAuth2
3. **Dashboard**: Access the main chat interface at `/dashboard.html`
4. **Add Friends**: Go to "All Users" and send friend requests
5. **Chat**: Once friends accept, start private conversations
6. **Create Groups**: Create group chats and add members
7. **Theme Toggle**: Use the theme button to switch between dark/light modes

## 🔐 API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - User login
- `GET /api/auth/validate` - Validate JWT token

### Users
- `GET /api/users/all` - Get all users
- `GET /api/users/search?query=` - Search users
- `GET /api/users/{id}` - Get user by ID
- `PUT /api/users/profile` - Update user profile
- `POST /api/users/profile/picture` - Update profile picture
- `POST /api/users/online` - Update online status

### Friends
- `POST /api/friends/request` - Send friend request
- `POST /api/friends/accept/{id}` - Accept friend request
- `POST /api/friends/reject/{id}` - Reject friend request
- `GET /api/friends/pending` - Get pending requests
- `GET /api/friends/accepted` - Get accepted friends

### Chat
- `GET /api/chat/conversation/{userId}` - Get conversation history
- `POST /api/chat/read/{userId}` - Mark conversation as read

### Groups
- `POST /api/groups/create` - Create new group
- `GET /api/groups/my-groups` - Get user's groups
- `POST /api/groups/{id}/members` - Add member to group
- `DELETE /api/groups/{id}/members/{userId}` - Remove member
- `GET /api/groups/{id}/messages` - Get group messages

### WebSocket
- `/ws` - WebSocket endpoint
- `/app/chat.sendMessage` - Send private message
- `/app/chat.sendGroupMessage` - Send group message
- `/app/chat.typing` - Send typing indicator

## 🎨 UI Features

- **Home Page**: Animated hero section with feature highlights
- **Login/Register**: Modern card-based forms with validation
- **Dashboard**: Sidebar navigation with chat window
- **Real-time Updates**: Instant message delivery
- **Typing Indicators**: See when someone is typing
- **Message Status**: Read receipts (✓✓)
- **Responsive Design**: Works on desktop and mobile

## 🔒 Security Features

- Password encryption with BCrypt
- JWT token-based authentication
- Spring Security configuration
- CORS protection
- SQL injection prevention (JPA)
- XSS protection

## 📝 Notes

- The database schema is auto-generated on first run
- Profile pictures and media are stored as BLOB in MySQL
- Theme preference is saved per user
- Online status updates automatically on login/logout

## 🐛 Troubleshooting

1. **Database Connection Error**: Check MySQL credentials in `application.properties`
2. **OAuth2 Not Working**: Verify Google credentials and redirect URI
3. **WebSocket Connection Failed**: Ensure port 8080 is not blocked
4. **Build Errors**: Make sure Java 21 is installed and Maven is configured

## 📄 License

This project is created for educational purposes.

## 👨‍💻 Development

Built with ❤️ using Spring Boot and modern web technologies.


