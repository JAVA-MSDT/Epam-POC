# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Spring Boot demo application showcasing the [MaskMe](https://github.com/JAVA-MSDT/MaskMe) annotation-driven field
masking library. The project is used for live presentations and demos, with an interactive web UI alongside the REST
API.

The Maven project lives under `AI-Guide/`. All source code, resources, and the `pom.xml` are inside that subdirectory.

## Commands

```bash
# From the AI-Guide/ directory:

# Build and run tests
mvn clean install

# Run a single test class
mvn test -Dtest=BalanceMaskingConditionTest

# Run the application (serves on http://localhost:9090)
mvn spring-boot:run
```

## Architecture

Standard Spring MVC layered architecture with an added masking integration layer:

```
HTTP Request
    → UserController (GET /users, /users/{id}, /users/masked/{id})
    → UserService (in-memory hardcoded data, no database)
    → UserMapper (MapStruct: User domain → UserDto)
    → MaskMe library (processes @MaskMe annotations on DTOs)
    → JSON response
```

**Key design decisions:**

- **Masking happens only on the DTO layer.** `User`, `Address`, `GeoLocation` are plain domain records with no masking
  annotations. Only their DTO counterparts (`UserDto`, `AddressDto`) carry `@MaskMe`.
- **Condition instances are Spring-managed singletons with mutable state** (`setInput()` is called per-request). This is
  safe because MaskMe uses ThreadLocal internally — do not change condition classes to non-singleton scope.
- **`MaskMeConfiguration`** (`@PostConstruct`/`@PreDestroy`) bootstraps the library: registers a
  `MaskMeFrameworkProvider` that resolves beans via `applicationContext.getBean(type)`, registers
  `CustomStringConverter`, and clears global converter state on shutdown to prevent memory leaks.
- **`AlwaysMaskMeCondition` and `MaskMeOnInput`** are library built-ins that must be explicitly declared as `@Bean` in
  `MaskMeConfiguration` — they are not `@Component`-annotated in the library.
- **`CustomStringConverter`** (`priority = 10`, overrides library default of 0): returns `"[EMAIL PROTECTED]"` for empty
  `maskValue` strings, `"************"` for password fields, and `null` for everything else. A `null` return is
  intentional — it signals fall-through to the next converter.
- **`PhoneMaskingCondition`** double-validates: the `Mask-Phone` header value must match both the DTO field value AND a
  phone that exists in the user database.
- **`BalanceMaskingCondition`** is implemented and fully tested but **not yet wired** — `UserDto.balance` has no
  `@MaskMe` annotation, and there is no endpoint for it. This is the incomplete MASK-101 ticket.

## MaskMe Condition Wiring Pattern

When adding a new condition to an endpoint, pass pairs of `(ConditionClass.class, inputValue)` to
`MaskMeInitializer.mask()`:

```java
// Single condition
MaskMeInitializer.mask(dto, MaskMeOnInput .class, maskInput);

// Multiple conditions (varargs pairs)
MaskMeInitializer.

mask(dto, new Object[] {
    MaskMeOnInput.class, maskInput,
            PhoneMaskingCondition.class, maskPhone
});
```

## Static Frontend

Vanilla HTML/CSS/JS pages served from `src/main/resources/static/`:

| Page                      | URL                        | Purpose                                                     |
|---------------------------|----------------------------|-------------------------------------------------------------|
| `index.html`              | `/`                        | Overview / library description                              |
| `pages/api-demo.html`     | `/pages/api-demo.html`     | Interactive endpoint tester with visual masking diff        |
| `pages/sprint-board.html` | `/pages/sprint-board.html` | Jira-style sprint board (MASK-101 through MASK-104 tickets) |
| `pages/prompts.html`      | `/pages/prompts.html`      | Copy-paste AI prompt reference                              |
| `pages/profile.html`      | `/pages/profile.html`      | Individual user profile viewer                              |
| `pages/users.html`        | `/pages/users.html`        | User list with original/masked toggle                       |

All pages share `css/style.css`. `KNOWN_MASK_VALUES` in `api-demo.html` must be updated when new mask output strings are
added.

A standalone `prompts.html` at the repo root is a self-contained duplicate (different primary color `#4361ee`) designed
to be opened as a file without the server running.

## REST API

| Method | URL                  | Headers                                         | Description                                              |
|--------|----------------------|-------------------------------------------------|----------------------------------------------------------|
| GET    | `/users/{id}`        | —                                               | Unmasked user DTO                                        |
| GET    | `/users/masked/{id}` | `Mask-Input: maskMe`                            | Masked user (MaskMeOnInput + AlwaysMask conditions)      |
| GET    | `/users`             | `Mask-Input: maskMe`, `Mask-Phone: 01000000000` | All users, phone masked when header matches stored value |

Always-masked fields (regardless of headers): `email` → `[EMAIL PROTECTED]`, `password` → `************`,
`address.city` → `****`.

## Sprint Board Tickets (Demo Tasks)

| Ticket   | Sprint | Points | Status | Summary                                                          |
|----------|--------|--------|--------|------------------------------------------------------------------|
| MASK-101 | 4      | 5      | To Do  | Add `BalanceMaskingCondition` endpoint + `BigDecimalConverter`   |
| MASK-102 | 4      | 3      | To Do  | Unit tests for MASK-101                                          |
| MASK-103 | 5      | 5      | To Do  | Add `VisibilityMaskingCondition` for role-based email visibility |
| MASK-104 | 5      | 3      | To Do  | Unit tests for MASK-103                                          |

`BalanceMaskingCondition` and its tests (`BalanceMaskingConditionTest`) already exist for MASK-101 — the remaining work
is the `@MaskMe` annotation on `UserDto.balance`, a `BigDecimalConverter`, and the new endpoint.

## Code Style

Use comments sparingly. Only comment complex code where the logic is non-obvious.

## Key Dependencies

- **Spring Boot 4.0.1** (requires Java 21+)
- **MapStruct 1.4.2.Final** — annotation processor; must be listed after Lombok in `annotationProcessorPaths` in
  `pom.xml`
- **MaskMe 1.0.0** (`io.github.java-msdt:maskme`)
- **Lombok 1.18.30**