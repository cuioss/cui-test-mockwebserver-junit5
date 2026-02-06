# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A JUnit 5 extension for OkHttp's `mockwebserver3.MockWebServer` that simplifies HTTP/HTTPS testing. It provides annotation-driven dispatcher configuration, parameter injection, and HTTPS certificate management.

**Key dependency:** OkHttp3 MockWebServer (`mockwebserver3` package, not the older `okhttp3.mockwebserver`). The API uses builder patterns: `new MockResponse.Builder().code(200).body("...").build()`, `server.close()` (not `shutdown()`), and `request.getUrl().encodedPath()` (not `getPath()`).

## Build Commands

```bash
# Full pre-commit build (compile + tests + javadoc check) - USE THIS before committing
./mvnw -Ppre-commit clean install

# Quick build with tests
./mvnw clean install

# Run a single test class
./mvnw test -Dtest=CombinedDispatcherTest

# Run a single test method
./mvnw test -Dtest=CombinedDispatcherTest#shouldHandleConstructor
```

Parent POM `de.cuioss:cui-java-parent` provides most plugin configuration, enforcer rules, and a pre-commit linter that reformats copyright headers and import order.

## Architecture

### Extension Lifecycle (`MockWebServerExtension`)

The core JUnit 5 extension implements `BeforeEachCallback`, `AfterEachCallback`, and `ParameterResolver`:
- **Before each:** Creates `MockWebServer`, configures HTTPS if needed, resolves dispatcher, starts server (unless `manualStart=true`), stores in `ExtensionContext`
- **After each:** Calls `server.close()`, removes from context
- **Parameter injection:** `MockWebServer`, `URIBuilder` (pre-configured with server URL), `SSLContext` (when HTTPS enabled)

### Dispatcher Resolution Chain (`DispatcherResolver`)

The dispatcher resolution follows a priority order combining annotations found on the test class and current test method:

1. **`@MockResponseConfig`** annotations (class-level and method-level) - declarative mock responses for specific path + HTTP method combinations. Method-level annotations are context-aware and only active for that test.
2. **`@ModuleDispatcher`** annotations - point to `Dispatcher` classes or `ModuleDispatcherElement` providers
3. **`getModuleDispatcher()` method** on test class - returns a `ModuleDispatcherElement` instance
4. **Fallback** - default `CombinedDispatcher` with `/api` base URL

All resolved `ModuleDispatcherElement`s are combined into a `CombinedDispatcher` which uses chain-of-responsibility: requests are matched by base URL prefix, first matching dispatcher handles the request. Unmatched requests return 418 (teapot) or 404.

### Key Interfaces

- **`ModuleDispatcherElement`** - interface for modular request handling: defines `getBaseUrl()`, `handleGet/Post/Put/Delete(RecordedRequest)`, and `supportedMethods()`
- **`HttpMethodMapper`** - enum routing requests to the correct `handle*` method on a `ModuleDispatcherElement`
- **`EndpointAnswerHandler`** - pre-configured response constants (`RESPONSE_OK`, `RESPONSE_FORBIDDEN`, etc.) and fluent API for response management
- **`BaseAllAcceptDispatcher`** - convenience `ModuleDispatcherElement` that accepts all HTTP methods for a given base URL

### HTTPS Support (`CertificateResolver`, `ssl/`)

When `@EnableMockWebServer(useHttps=true)`:
- `CertificateResolver` looks for `@TestProvidedCertificate` annotation for custom certs, falls back to self-signed via `HandshakeCertificates`
- SSLContext is stored in ExtensionContext for parameter injection

## Conventions

- Uses Lombok (`@Getter`, `@Setter`, `@NonNull`, `@NoArgsConstructor`)
- Logging via `CuiLogger` from `cui-java-tools` (not `java.util.logging` directly)
- HTTP status codes referenced via `jakarta.servlet.http.HttpServletResponse` constants (e.g., `SC_OK`, `SC_FORBIDDEN`)
- MockResponse construction always via Builder: `new MockResponse.Builder().code(status).build()`
- RecordedRequest path access: `request.getUrl().encodedPath()` (query: `request.getUrl().encodedQuery()`)

## Git Workflow

All cuioss repositories have branch protection on `main`. Direct pushes to `main` are never allowed. Always use this workflow:

1. Create a feature branch: `git checkout -b <branch-name>`
2. Commit changes: `git add <files> && git commit -m "<message>"`
3. Push the branch: `git push -u origin <branch-name>`
4. Create a PR: `gh pr create --repo cuioss/cui-test-mockwebserver-junit5 --head <branch-name> --base main --title "<title>" --body "<body>"`
5. Wait for CI + Gemini review (waits until checks complete): `gh pr checks --repo cuioss/cui-test-mockwebserver-junit5 <pr-number> --watch`
6. **Handle Gemini review comments** — fetch with `gh api repos/cuioss/cui-test-mockwebserver-junit5/pulls/<pr-number>/comments` and for each:
   - If clearly valid and fixable: fix it, commit, push, then reply explaining the fix and resolve the comment
   - If disagree or out of scope: reply explaining why, then resolve the comment
   - If uncertain (not 100% confident): **ask the user** before acting
   - Every comment MUST get a reply (reason for fix or reason for not fixing) and MUST be resolved
7. Do **NOT** enable auto-merge unless explicitly instructed. Wait for user approval.
8. Return to main: `git checkout main && git pull`
