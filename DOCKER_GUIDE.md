# Summer Application - Docker Setup

## Quick Start

You have **two options** for running this application:

### Option 1: Local Development (Recommended for coding)
Run only the database in Docker, run the Spring Boot app locally.

```bash
# Start database only
docker-compose -f docker-compose.dev.yml up -d

# Run the app from your IDE or:
./gradlew bootRun
```

**Pros:**
- ✅ Fast hot-reload during development
- ✅ Easy debugging from IDE
- ✅ Faster build cycles

**Cons:**
- ❌ Requires Java 21 installed locally
- ❌ Different environment than production

---

### Option 2: Full Containerization (Production-like)
Run everything (database + app) in Docker containers.

```bash
# Build and start everything
docker-compose up --build -d

# View logs
docker-compose logs -f summer

# Stop everything
docker-compose down
```

**Pros:**
- ✅ Production-ready setup
- ✅ No local Java installation needed
- ✅ Consistent environment across team
- ✅ Easy deployment

**Cons:**
- ❌ Slower rebuild times during development
- ❌ No hot-reload

---

## Accessing the Application

Once running (either option):
- API: http://localhost:8080/v1/likes/{slug}
- Swagger UI: http://localhost:8080/docs
- Health Check: http://localhost:8080/actuator/health
- Database: localhost:5432 (postgres/app:mypassword)

## CORS Configuration

The application is configured to accept CORS requests from any localhost origin (http://localhost:*, http://127.0.0.1:*), perfect for local frontend development.

