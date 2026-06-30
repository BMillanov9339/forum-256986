# Forum

A full-stack forum application built with Spring Boot, React, PostgreSQL, and Docker Compose.

## Features

- User registration and JWT authentication
- Topics and replies
- Profile management
- Moderator and administrator roles
- Soft deletion and moderation auditing
- Database migrations with Flyway
- Health and readiness endpoints
- OpenAPI documentation

## Technology

- Java 21
- Spring Boot 4
- React 19 and Vite
- PostgreSQL 16
- Docker Compose
- Maven

## Quick start with Docker

### Prerequisites

- Docker Desktop or Docker Engine with Compose v2
- Linux containers enabled
- Ports `5173`, `9000`, `5432`, and `8090` available
- Internet access for the first build

### Configuration

Create the local environment file:

```powershell
Copy-Item .env.example .env
