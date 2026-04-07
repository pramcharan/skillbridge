# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SkillBridge is an AI-powered freelance marketplace built with **Spring Boot 3.4.1** and **Java 21**. It connects clients (employers) with freelancers through job postings, proposals, and project management.

## Development Commands

### Build & Run
```bash
# Start MySQL via Docker (maps host port 3307 -> container 3306)
docker compose up -d

# Compile and run
./mvnw spring-boot:run

# Build JAR
./mvnw clean package
```

### Testing
```bash
# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=JobServiceTest

# Run a single test method
./mvnw test -Dtest=JobServiceTest#testCreateJob

# Run with coverage (JaCoCo enforces 60% on service layer)
./mvnw test jacoco:report
```

### Code coverage report output
`target/site/jacoco/index.html`

## Architecture

### Layered Structure
```
Controller → Service → Repository → Entity (JPA/Hibernate)
              ↓
            DTO (request/response)
```

- **Controllers** (17) under `com.skillbridge.controller` — REST endpoints at `/api/v1/*`
- **Services** (22) under `com.skillbridge.service` — business logic, some with `@Async` and `@Scheduled`
- **Repositories** under `com.skillbridge.repository` — Spring Data JPA interfaces
- **Entities** under `com.skillbridge.entity` — JPA entities with indexes and constraints
- **DTOs** under `com.skillbridge.dto` — request/response objects, separated into `request`/`response`/`ai` subpackages
- **Security** under `com.skillbridge.security` — JWT auth, OAuth2, user details

### Key API Endpoints

| Prefix | Purpose |
|--------|---------|
| `/api/v1/auth` | Registration, login, OAuth2 |
| `/api/v1/auth/password` | Forgot/reset password |
| `/api/v1/jobs` | Job CRUD, search, posting |
| `/api/v1/proposals` | Freelancer proposals |
| `/api/v1/projects` | Active projects, milestones |
| `/api/v1/projects/{id}/revisions` | Revision requests |
| `/api/v1/profile` | User profiles, onboarding |
| `/api/v1/portfolio` | Freelancer portfolio items |
| `/api/v1/reviews` | Reviews and ratings |
| `/api/v1/notifications` | User notifications |
| `/api/v1/dashboard` | Freelancer/client dash stats |
| `/api/v1/admin` | Admin-only endpoints |
| `/api/v1/community` | Community chat, messages |
| `/api/v1/disputes` | Dispute resolution |
| `/api/v1/saved-jobs` | Bookmarked jobs |
| `/api/v1/upload` | File uploads |
| `/api/v1/stats` | Public stats |

### Authentication & Authorization

- **JWT-based auth** — stateless, 24-hour expiration, Bearer token in `Authorization` header
- **OAuth2 login** — Google and GitHub as providers
- **Password reset** — email-based with token expiry
- **Role-based access** — `ADMIN` role required for `/api/v1/admin/**`, all other API endpoints require authentication
- **Token revocation** — logout tracks revoked tokens to prevent reuse
- **Security config** — `SecurityConfig` permits all static HTML pages and static assets; API endpoints require auth except public stats and auth routes

### WebSocket

STOMP over SockJS at `/ws`. Authenticates via JWT in the `Authorization` header on `CONNECT`. Destination prefixes: `/app` for sending, `/topic`/`/user`/`/queue` for subscribing.

### AI Integration

Pluggable AI provider system via `AiExplanationFactory`:
- **OpenRouter** (primary) — uses `stepfun/step-3.5-flash:free` with `qwen/qwen3.6-plus:free` as backup
- **Ollama** (local) — configurable model, currently disabled
- **OpenAI** — configured but key not set
- **WeightedScoringService** — non-AI scoring based on skill match, experience, etc.

### Database

- **MySQL 8.0** on host port **3307** (container port 3306)
- Database: `skillbridge_db`, user: `sbuser`, password: `sbpassword`
- `spring.jpa.hibernate.ddl-auto=update` — auto-creates/alters tables
- HikariCP connection pooling (max 10 connections)
- JPA batch inserts/updates enabled for performance

### Key Domain Entities

- **User** — central entity with Role enum; linked to almost all other entities
- **Job** — posted by clients, with status tracking and indexing
- **Proposal** — freelancers submit proposals for jobs; has status lifecycle
- **Project** — accepted proposals become projects; tracks milestones and deliverables
- **ChatMessage** — project-level communication
- **RevisionRequest** — clients can request revisions on project deliverables
- **Review** — post-project ratings and feedback
- **Notification** — async event notifications for users
- **DisputeTicket** — dispute resolution between client and freelancer
- **CommunityMessage** / **CommunityReaction** — community chat features
- **RoomPresence** — WebSocket-based presence tracking for chat rooms
- **SavedJob**, **PortfolioItem**, **FileAttachment** — supporting entities

### Configuration Files

| File | Purpose |
|------|---------|
| `application.properties` | Main config — DB, AI providers, file upload, caching |
| `application-secrets.properties` | OAuth creds, mail, JWT secret, API keys (not committed) |
| `docker-compose.yml` | MySQL container |
| `WebConfig.java` | Resource handlers, CORS (allows localhost:5500 for Live Server) |
| `SecurityConfig.java` | Spring Security, JWT filter, OAuth2 login |
| `WebSocketConfig.java` | STOMP broker, JWT interceptor |
| `AiConfig.java` | AI provider configuration |
| `CloudinaryConfig.java` | Cloudinary file storage |

### Testing

- **H2 in-memory database** for test profile — no MySQL needed
- Test config: `src/test/resources/application-test.properties`
- 19 test classes covering controllers, services, repositories, and AI services
- Uses Spring Boot Test with `@SpringBootTest`, Mockito for unit tests

### Notes

- The frontend is served as static HTML/CSS/JS from `src/main/resources/static/` (no separate frontend framework)
- File uploads go to `uploads/` directory, served via `/uploads/**` resource handler
- Cloudinary is used for profile images and file uploads
- Lombok is used extensively for boilerplate reduction
- `scheduler_service` handles automated notifications and stale data cleanup
