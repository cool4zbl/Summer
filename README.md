# Summer Project

"Summer Project" is the backend app for my [personal website](htts://binliuzhang.com), as well as a sample project demonstrating how to build a Spring Boot application with Postgres, Docker, and Swagger/OpenAPI documentation.

## Features
### Post Likes
- RESTful API for managing posts likes, including:
  - Like a post (up to 20 times per user).
  - Retrieve total likes for a post.
- Thread-safe operations to handle concurrent likes.


## Tools & Technologies
- Spring Boot application with Postgres database.
- Flyway for database migrations.
- Swagger / OpenAPI documentation using springdoc.
- Docker Compose setup for easy local development.
- "Railway" for cloud deployment.
- Neon.tech for Postgres hosting.

## Running Postgres
Start the Postgres container (persistent volume + healthcheck already configured):

```bash
docker compose -f docker-compose.yml up -d postgres
```

To view logs:
```bash
docker compose -f docker-compose.yml logs -f postgres
```

## Running the App
Compile & run:
```bash
./gradlew bootRun
```

The app will connect to Postgres at `jdbc:postgresql://localhost:5432/app_db` using credentials configured in `application.yml` (override with environment variables: `DATABASE_URL`, `DB_USER`, `DB_PASSWORD`). Flyway runs migrations from `classpath:db/migration`.

## Swagger / OpenAPI (springdoc)
Dependency: `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0` (already added).
Configuration class: `OpenApiConfig` defines metadata and a `GroupedOpenApi` for paths under `/v1/**`.
Controller endpoints are annotated with `@Operation` and `@Parameter`.

### Accessing the UI
Once the app is running, open:
- Swagger UI: http://localhost:8080/swagger-ui.html (redirects) or http://localhost:8080/swagger-ui/index.html
- Raw OpenAPI JSON: http://localhost:8080/v3/api-docs
- Group-specific docs (group `v1`): http://localhost:8080/v3/api-docs/v1

### Adding Documentation
- Add `@Operation(summary = ..., description = ...)` on controller methods.
- Use `@Parameter(description = ...)` on method parameters.
- For models/DTOs, annotate classes or fields with `@Schema(description = ..., example = ...)`.
- To group additional versions, add more `GroupedOpenApi` beans in `OpenApiConfig`.

### Customizing Global Settings
Add properties in `application.yml` if desired:
```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    path: /docs
    operationsSorter: method
    tagsSorter: alpha
```
Swagger UI would then be at http://localhost:8080/docs.

### Security (Optional)
If you later add auth (e.g., JWT), you can define security schemes:
```java
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
```
Then reference in `@Operation(security = { @SecurityRequirement(name = "bearerAuth") })`.

## Troubleshooting
- 404 on `/swagger-ui.html`: ensure app is running and you are using Spring Boot web starter.
- Empty docs: check that your controller is under a package scanned by `@SpringBootApplication` (it is).
- Missing models: ensure you return concrete types, not raw `Map` or `Object`.

