# MaskMe Demo Project

A Spring Boot demo application showcasing the [MaskMe](https://github.com/JAVA-MSDT/MaskMe)
annotation-driven masking library. Built for presentations and live demos.

## Table of Contents

- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Web UI](#web-ui)
- [REST API Endpoints](#rest-api-endpoints)
- [Tech Stack](#tech-stack)

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+

### Run the Application (Web UI + API)

```bash
mvn clean install
mvn spring-boot:run
```

Open [http://localhost:9090](http://localhost:9090) to access the web UI.

## Project Structure

```
AI-Guide/
├── src/main/java/com/javamsdt/masking/
│   ├── controller/
│   │   └── UserController.java            # GET /users — masking demo endpoints
│   ├── domain/
│   │   ├── Address.java                   # Domain entity with @MaskMe annotations
│   │   ├── Gender.java
│   │   ├── GeoLocation.java
│   │   └── User.java                      # Domain entity with @MaskMe + @ExcludeMaskMe
│   ├── dto/
│   │   ├── AddressDto.java                # DTO with @MaskMe on city, zipCode
│   │   ├── GeoLocationDto.java            # DTO with @MaskMe on id, latitude
│   │   └── UserDto.java                   # DTO with @MaskMe on 8 fields + @ExcludeMaskMe
│   ├── mapper/
│   │   └── UserMapper.java                # MapStruct mapper (User → UserDto)
│   ├── maskme/
│   │   ├── benchmark/
│   │   │   └── SimpleBenchmark.java       # Standalone MaskMe benchmark
│   │   ├── condition/
│   │   │   └── PhoneMaskingCondition.java # Custom condition with Spring DI
│   │   ├── config/
│   │   │   └── MaskMeConfiguration.java   # Spring config for MaskMe
│   │   └── converter/
│   │       └── CustomStringConverter.java # Custom converter (email, password)
│   ├── service/
│   │   └── UserService.java               # In-memory user data
│   └── SpringMaskingApplication.java      # Spring Boot entry point
├── src/main/resources/
│   ├── static/
│   │   ├── css/style.css                  # Shared styles for all pages
│   │   ├── index.html                     # Overview page — library description
│   │   └── api-demo.html                  # API demo — interactive endpoint testing
│   └── application.properties             # server.port=9090
├── pom.xml
└── README.md
```

## Architecture

### Application Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     Spring Boot Application                      │
│                     (port 9090)                                   │
├──────────────────────┬──────────────────────────────────────────┤
│                      │                                           │
│   Static HTML Pages  │  REST Controller                          │
│   ┌────────────────┐ │  ┌──────────────────────────────────┐    │
│   │ index.html     │ │  │ UserController                   │    │
│   │ api-demo.html  │ │  │  GET /users/{id}      (unmasked) │    │
│   └────────────────┘ │  │  GET /users/masked/{id} (masked) │    │
│                      │  │  GET /users/user/{id}  (domain)  │    │
│                      │  │  GET /users            (all)      │    │
│                      │  └──────────────────────────────────┘    │
├──────────────────────┴──────────────────────────────────────────┤
│                                                                  │
│   MaskMe Library Integration                                     │
│   ┌────────────────────────────────────────────────────────┐    │
│   │ MaskMeConfiguration                                    │    │
│   │  ├─ Framework Provider (Spring ApplicationContext)      │    │
│   │  ├─ Bean Registration (AlwaysMask, MaskMeOnInput)      │    │
│   │  └─ Custom Converter (CustomStringConverter)           │    │
│   ├────────────────────────────────────────────────────────┤    │
│   │ Conditions                                             │    │
│   │  ├─ AlwaysMaskMeCondition  → always masks              │    │
│   │  ├─ MaskMeOnInput          → masks when input="maskMe" │    │
│   │  └─ PhoneMaskingCondition  → masks specific phone      │    │
│   ├────────────────────────────────────────────────────────┤    │
│   │ Annotated DTOs                                         │    │
│   │  ├─ UserDto    (11 fields, 8 masked)                   │    │
│   │  ├─ AddressDto (7 fields, 2 masked)                    │    │
│   │  └─ GeoLocationDto (3 fields, 2 masked)               │    │
│   └────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘
```

### Masking Annotations Map

| Field      | DTO            | Condition             | Mask Value                      |
|------------|----------------|-----------------------|---------------------------------|
| id         | UserDto        | AlwaysMaskMeCondition | `"1000"`                        |
| name       | UserDto        | MaskMeOnInput         | `"{email}-{genderId}"`          |
| email      | UserDto        | AlwaysMaskMeCondition | `""` (empty → custom converter) |
| password   | UserDto        | AlwaysMaskMeCondition | `"****"` (default)              |
| phone      | UserDto        | PhoneMaskingCondition | `"****"` (default)              |
| birthDate  | UserDto        | AlwaysMaskMeCondition | `"01/01/1800"`                  |
| balance    | UserDto        | AlwaysMaskMeCondition | `""` (empty → 0)                |
| createdAt  | UserDto        | AlwaysMaskMeCondition | `"1900-01-01T00:00:00.00Z"`     |
| genderName | UserDto        | `@ExcludeMaskMe`      | Skipped entirely                |
| city       | AddressDto     | AlwaysMaskMeCondition | `"****"` (default)              |
| zipCode    | AddressDto     | MaskMeOnInput         | `"[ZIP_MASKED]"`                |
| id         | GeoLocationDto | AlwaysMaskMeCondition | `"00000000-0000-..."`           |
| latitude   | GeoLocationDto | AlwaysMaskMeCondition | `"00.0000"`                     |

## Web UI

The application serves two HTML pages at [http://localhost:9090](http://localhost:9090):

| Page     | URL              | Purpose                                                                         |
|----------|------------------|---------------------------------------------------------------------------------|
| Overview | `/`              | MaskMe library description, features, code comparison (hardcoded vs annotation) |
| API Demo | `/api-demo.html` | Interactive endpoint testing with visual user cards, side-by-side comparison    |

### API Demo Features

- **Side-by-side comparison** — fetches unmasked and masked user in one click, displayed side by side
- **Visual user cards** — masked fields highlighted in amber 🎭, excluded fields in green 🛡️
- **All 4 endpoints** — configurable headers and user selection
- **Raw JSON toggle** — expand to see the actual JSON response

## REST API Endpoints

### User Endpoints

| Method | URL                  | Headers                                         | Description                              |
|--------|----------------------|-------------------------------------------------|------------------------------------------|
| GET    | `/users/{id}`        | —                                               | Unmasked user DTO                        |
| GET    | `/users/masked/{id}` | `Mask-Input: maskMe`                            | Masked user (MaskMeOnInput condition)    |
| GET    | `/users/user/{id}`   | —                                               | Domain entity with AlwaysMaskMeCondition |
| GET    | `/users`             | `Mask-Input: maskMe`, `Mask-Phone: 01000000000` | All users with multiple conditions       |

**Example:**

```bash
# Unmasked
curl http://localhost:9090/users/1

# Masked (MaskMeOnInput)
curl -H "Mask-Input: maskMe" http://localhost:9090/users/masked/1

# Multiple conditions
curl -H "Mask-Input: maskMe" -H "Mask-Phone: 01000000000" http://localhost:9090/users
```

## Tech Stack

| Component | Technology                                          |
|-----------|-----------------------------------------------------|
| Framework | Spring Boot 4.0.1                                   |
| Java      | 21                                                  |
| Masking   | [MaskMe 1.0.0](https://github.com/JAVA-MSDT/MaskMe) |
| Mapping   | MapStruct 1.4.2                                     |
| Lombok    | 1.18.30                                             |
| Build     | Maven                                               |
| Frontend  | Vanilla HTML/CSS/JS (no framework)                  |

---

**Author:** [Ahmed Samy](https://github.com/JAVA-MSDT) | [LinkedIn](https://www.linkedin.com/in/java-msdt/)
