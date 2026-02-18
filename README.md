# JWT Authentication System

A Spring Boot application that implements a complete JWT (JSON Web Token) authentication system with a REST API and a Java client for testing.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [API Endpoints](#api-endpoints)
- [Project Structure](#project-structure)
- [Security Considerations](#security-considerations)
- [License](#license)

## 🔍 Overview

This project demonstrates a basic JWT authentication implementation using Spring Boot. It provides a RESTful API for user authentication, token generation, token refresh, and secure endpoint access. The system includes both the server-side implementation and a Java REST client for testing purposes.

## ✨ Features

- **User Authentication**: Login endpoint that generates JWT tokens
- **Token Management**: 
  - Token generation with UUID
  - Token expiration (30 seconds by default)
  - Token refresh mechanism
- **Secure Endpoints**: Protected resources that require valid JWT tokens
- **Public Endpoints**: Open access endpoints for testing
- **REST Client**: Interactive command-line client for testing the API
- **Automatic Token Refresh**: Client automatically refreshes expired tokens

## 🏗️ Architecture

The project follows a standard Spring Boot REST API architecture:

```
├── controller/
│   └── JwtController.java          # REST API endpoints
├── model/
│   └── Jwt.java                    # Token data model
└── restClient/
    └── jwtRestClient.java          # Java HTTP client for testing
```

### Components

1. **JwtController**: Manages authentication endpoints
2. **Jwt Model**: Represents token data structure
3. **REST Client**: Command-line interface for API testing

## 📦 Requirements

- Java 17 or higher
- Spring Boot 3.x
- Maven or Gradle (for dependency management)
- Jakarta Persistence API

### Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>jakarta.persistence</groupId>
        <artifactId>jakarta.persistence-api</artifactId>
    </dependency>
</dependencies>
```

## 🚀 Installation

1. Clone the repository:
```bash
git clone https://github.com/seergiip/JWT.git
cd JWT
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

The server will start on `http://localhost:8080`

## 💻 Usage

### Running the Server

Start the Spring Boot application:

```bash
mvn spring-boot:run
```

### Using the REST Client

Run the Java client to interact with the API:

```bash
java com.rgbconsulting.jwt.restClient.jwtRestClient
```

The client provides an interactive menu:

```
----------------- JWT -----------------
0. Sortir (Exit)
1. Fer login (Login)
2. Fer secure ping (Secure Ping)
Option: 
```

### Client Workflow

1. **Login** (Option 1): Authenticate and receive a JWT token
2. **Secure Ping** (Option 2): Access protected endpoint with the token
3. The client automatically refreshes expired tokens

## 🔌 API Endpoints

### Authentication Endpoints

#### POST `/jwt/auth/login`
Authenticates a user and returns a JWT token.

**Request Body:**
```json
{
  "username": "sergi",
  "password": "pepitodelospalotes1234"
}
```

**Response:**
```json
{
  "access_token": "550e8400-e29b-41d4-a716-446655440000",
  "expires_in": 30,
  "username": "sergi",
  "time_generated": 1640000000000
}
```

#### POST `/jwt/auth/refresh`
Refreshes an expired token with a new one.

**Request Body:**
```json
{
  "access_token": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:**
```json
{
  "access_token": "660e8400-e29b-41d4-a716-446655440001",
  "expires_in": 30,
  "username": "sergi",
  "time_generated": 1640000030000
}
```

### Protected Endpoints

#### GET `/jwt/secure/ping`
Secure endpoint that requires a valid JWT token.

**Headers:**
```
Authorization: Bearer {token}
```

**Response:**
```
ok: true, user: sergi
```

**Error Responses:**
- `401 Unauthorized`: Token expired or invalid

### Public Endpoints

#### GET `/jwt/public/ping`
Public endpoint that doesn't require authentication.

**Response:**
```
ok: true, user: unknown
```

## 📁 Project Structure

```
com.rgbconsulting.jwt/
│
├── controller/
│   └── JwtController.java
│       ├── POST /jwt/auth/login
│       ├── POST /jwt/auth/refresh
│       ├── GET /jwt/secure/ping
│       └── GET /jwt/public/ping
│
├── model/
│   └── Jwt.java
│       ├── id
│       ├── username
│       ├── password
│       ├── access_token
│       ├── expires_in
│       └── time_generated
│
└── restClient/
    └── jwtRestClient.java
        ├── login()
        ├── petitionSecurePing()
        ├── authRefresh()
        └── getToken()
```

## 🔒 Security Considerations

⚠️ **Important**: This is a demonstration project and should NOT be used in production without significant security enhancements:

1. **Token Storage**: Tokens are stored in-memory and will be lost on server restart
2. **Password Handling**: Passwords are not hashed or encrypted
3. **Token Generation**: Uses UUID instead of cryptographically signed JWT
4. **Short Expiration**: 30-second token expiration is for testing only
5. **No User Database**: User credentials are hardcoded in the client
6. **No HTTPS**: Should use HTTPS in production
7. **No Rate Limiting**: Vulnerable to brute force attacks

### Recommended Improvements for Production

- Use a proper JWT library (e.g., `jjwt`, `spring-security-oauth2`)
- Implement password hashing (BCrypt, Argon2)
- Store tokens in a database or Redis
- Add refresh token rotation
- Implement proper user authentication
- Add rate limiting and throttling
- Use HTTPS for all communications
- Implement proper error handling
- Add logging and monitoring
- Use environment variables for configuration

## 🛠️ Configuration

Default configuration values:

```java
// Token expiration time
expires_in = 30 seconds

// Server configuration
server.port = 8080

// Client credentials (for testing)
username = "sergi"
password = "pepitodelospalotes1234"
```

## 🧪 Testing

### Manual Testing with cURL

**Login:**
```bash
curl -X POST http://localhost:8080/jwt/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"sergi","password":"pepitodelospalotes1234"}'
```

**Secure Ping:**
```bash
curl -X GET http://localhost:8080/jwt/secure/ping \
  -H "Authorization: Bearer {your-token-here}"
```

**Public Ping:**
```bash
curl -X GET http://localhost:8080/jwt/public/ping
```

## 📝 License

This project is open source and available under the [MIT License](LICENSE).

## 👤 Author

**Sergi**
- GitHub: [@seergiip](https://github.com/seergiip)

**Note**: This is an educational project designed to demonstrate basic JWT authentication concepts. It is not intended for production use without significant security enhancements.
