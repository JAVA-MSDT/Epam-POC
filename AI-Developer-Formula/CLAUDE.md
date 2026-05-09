# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Spring Boot demo application showcasing the [MaskMe](https://github.com/JAVA-MSDT/MaskMe) annotation-driven field masking library. The project is used for live presentations and demos, with an interactive web UI alongside the REST API.

The Maven project lives under `AI-Guide/`. All source code, resources, and the `pom.xml` are inside that subdirectory.

## Commands

```bash
# From the AI-Guide/ directory:

# Build and install
mvn clean install

# Run the application (serves on http://localhost:9090)
mvn spring-boot:run
```

There are no tests in this project (`src/test/` does not exist).

## Architecture

The app follows a standard Spring MVC layered architecture with an added masking integration layer:

```
HTTP Request
    → UserController (GET /users, /users/{id}, /users/masked/{id})
    → UserService (in-memory hardcoded data, no database)
    → UserMapper (MapStruct: User domain → UserDto)
    → MaskMe library (processes @MaskMe annotations on DTOs)
    → JSON response
```

**Key design decisions:**
- **Masking happens only on the DTO layer**, not on domain entities. `User`, `Address`, `GeoLocation` are plain records; their DTO counterparts carry `@MaskMe` annotations.
- **`MaskMeConfiguration`** wires the MaskMe library into Spring's context via `@PostConstruct`/`@PreDestroy`, registers conditions (`AlwaysMask`, `MaskMeOnInput`), and installs a `CustomStringConverter` for special email/password masking behavior.
- **`PhoneMaskingCondition`** demonstrates Spring DI inside MaskMe conditions — it reads the `Mask-Phone` request header from Spring's `HttpServletRequest`.
- **Masking is condition-driven**: `AlwaysMaskMeCondition` always masks; `MaskMeOnInput` only masks when the `Mask-Input: maskMe` header is present.

## Static Frontend

Vanilla HTML/CSS/JS pages served by Spring Boot's static resource handler from `src/main/resources/static/`:

| Page | URL | Purpose |
|---|---|---|
| `index.html` | `/` | Overview / library description |
| `pages/api-demo.html` | `/pages/api-demo.html` | Interactive endpoint tester with visual masking diff |
| `pages/sprint-board.html` | `/pages/sprint-board.html` | Jira-style sprint board for demo presentations |
| `pages/prompts.html` | `/pages/prompts.html` | Copy-paste prompt reference |
| `pages/profile.html` | `/pages/profile.html` | User profile viewer |

All pages share `css/style.css`.

## REST API

| Method | URL | Headers | Description |
|---|---|---|---|
| GET | `/users/{id}` | — | Unmasked user DTO |
| GET | `/users/masked/{id}` | `Mask-Input: maskMe` | Masked user (MaskMeOnInput condition) |
| GET | `/users` | `Mask-Input: maskMe`, `Mask-Phone: 01000000000` | All users with multiple conditions |
