# Spring Security JWT Authentication Setup Guide

This guide explains the JWT-based Spring Security authentication implemented in your IAM project.

## Overview

Your project now includes:

- **JWT Token Generation & Validation** - Using JJWT library
- **Access & Refresh Token** - Short-lived access tokens with refresh capability
- **Spring Security Integration** - Stateless session management with JWT
- **Custom User Details Service** - Loading users from the database
- **JWT Authentication Filter** - Validating tokens on each request

## Components

### 1. **Entities**

#### User (Updated)

- Implements `UserDetails` for Spring Security
- Added fields for account status (`enabled`, `accountNonExpired`, etc.)
- Many-to-Many relationship with `Role` entity
- Provides authorities based on roles

### 2. **Security Components**

#### JwtUtil.java

- Generates access and refresh tokens
- Validates tokens
- Extracts username and claims from tokens
- Configurable token expiration times via properties

#### JwtAuthenticationFilter.java

- Intercepts requests and extracts JWT from `Authorization: Bearer <token>` header
- Validates tokens and sets authentication in security context
- Applied to all requests except public endpoints

#### SecurityConfig.java

- Configures Spring Security with JWT
- Enables CORS
- Disables CSRF (since we're using JWT)
- Sets stateless session management
- Defines public and protected endpoints

### 3. **Services**

#### CustomUserDetailsService.java

- Loads user details by username or ID
- Implements `UserDetailsService` interface

#### AuthenticationService.java

- Authenticates users with username/password
- Generates JWT tokens
- Refreshes access tokens
- Registers new users with password encoding

### 4. **Controllers**

#### AuthController.java

- `POST /api/auth/login` - Login endpoint (returns access + refresh tokens)
- `POST /api/auth/refresh` - Refresh access token

### 5. **DTOs**

- `LoginRequest` - Username and password
- `LoginResponse` - Access token, refresh token, expiration, username
- `RefreshTokenRequest` - Refresh token for obtaining new access token

### 6. **Repository**

#### UserRepository.java

- Database operations for User entity
- Find by username, email
- Check existence

## Configuration

### application.properties

```properties
# JWT Configuration
jwt.secret=mySecretKeyForJWTTokenGenerationWithMinimum256BitsForHS256AlgorithmSecurityPurpose
jwt.access.expiration=900000      # 15 minutes in milliseconds
jwt.refresh.expiration=604800000  # 7 days in milliseconds

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/iam_db
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

## API Endpoints

### 1. Login

**POST** `/api/auth/login`

Request:

```json
{
  "username": "user@example.com",
  "password": "password123"
}
```

Response:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "username": "user@example.com"
}
```

### 2. Refresh Token

**POST** `/api/auth/refresh`

Request:

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Response:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "username": "user@example.com"
}
```

### Using Access Token

Include the access token in subsequent requests:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

## Database Setup

### Required Tables

The application will auto-create these tables (with `spring.jpa.hibernate.ddl-auto=update`):

1. **t_users** - User entity with security fields
2. **t_people** - People entity (one-to-one with User)
3. **t_roles** - Role entity
4. **t_organization** - Organization entity
5. **user_roles** - Join table for User-Role relationship

### Sample User Creation

```sql
-- Insert a user (password should be BCrypt hashed)
INSERT INTO t_users (username, email, password, enabled, account_non_expired, account_non_locked, credentials_non_expired)
VALUES ('testuser', 'test@example.com', '$2a$10$...', true, true, true, true);
```

## Security Features

1. **Password Encoding** - BCrypt algorithm
2. **Stateless Authentication** - No sessions, JWT only
3. **CORS Support** - Configurable origins
4. **Role-Based Access** - Users can have multiple roles
5. **Token Expiration** - Automatic token expiration
6. **Refresh Token** - Obtain new access token without re-login

## Token Claims

### Access Token

```json
{
  "type": "access",
  "sub": "username",
  "iat": 1702900000,
  "exp": 1702900900
}
```

### Refresh Token

```json
{
  "type": "refresh",
  "sub": "username",
  "iat": 1702900000,
  "exp": 1703505800
}
```

## How to Use

### 1. Create a User

Implement a registration endpoint:

```java
@PostMapping("/auth/register")
public ResponseEntity<?> register(@RequestBody User user) {
    authenticationService.registerUser(user);
    return ResponseEntity.ok("User registered successfully");
}
```

### 2. Login & Get Tokens

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'
```

### 3. Use Access Token

```bash
curl -X GET http://localhost:8080/api/protected/endpoint \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 4. Refresh Token When Expired

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"YOUR_REFRESH_TOKEN"}'
```

## Protecting Endpoints

### Method Security

```java
@GetMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> adminEndpoint() {
    return ResponseEntity.ok("Admin access");
}

@GetMapping("/user")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public ResponseEntity<?> userEndpoint() {
    return ResponseEntity.ok("User access");
}
```

### Request-level Security

Modify `SecurityConfig.java` to restrict specific paths:

```java
.authorizeHttpRequests((authz) -> authz
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
    .anyRequest().authenticated()
)
```

## Troubleshooting

### Issue: Token Validation Fails

- Verify `jwt.secret` is the same across restarts
- Check token expiration: `jwt.access.expiration`
- Ensure `Authorization` header format is correct: `Bearer <token>`

### Issue: User Not Found

- Create the user in database with BCrypt-encoded password
- Verify username matches exactly (case-sensitive)

### Issue: CORS Errors

- Update `corsConfigurationSource()` in `SecurityConfig.java`
- Add your frontend URL to `allowedOrigins`

## Dependencies Added

```xml
<!-- JWT Token Support -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

## Best Practices

1. **Secret Key** - Change `jwt.secret` to a strong random value in production
2. **HTTPS** - Always use HTTPS in production
3. **Token Expiration** - Keep access tokens short-lived (15-30 min)
4. **Refresh Token** - Store refresh tokens securely on client (httpOnly cookies)
5. **Logout** - Implement token blacklisting for logout functionality

## Next Steps

1. Configure your database connection in `application.properties`
2. Create the Role entity if not already created
3. Create sample users and roles
4. Test the authentication endpoints
5. Implement role-based access control on your endpoints
6. Consider token blacklisting for logout functionality
7. Add swagger/springdoc-openapi for API documentation

---

For questions or issues, refer to the [JJWT Documentation](https://github.com/jwtk/jjwt) and [Spring Security Documentation](https://spring.io/projects/spring-security).
