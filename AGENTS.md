# AGENTS.md

## Build Commands

```bash
mvn clean package    # Build, outputs JAR to target/
mvn deploy          # Publish to local repo/ for redistribution
```

## Key Entry Points

- **Main class**: `com.da.web.core.DApp`
- **Config file**: `src/main/resources/app.properties`
- **Static files**: `src/main/resources/static/` (configurable via `static` prop)

## Important Quirks

- **Port fallback**: Starts at 8080, auto-increments if port is busy
- **Path routing order**: explicit routes → static files → beans → 404
- **Bean injection**: `@Path` beans get request params injected into fields without `@Inject`
- **Windows paths**: Uses `\\` for Windows, `/` for Unix in classpath scanning

## No Test Suite

This project has no tests. Verify manually by running a demo app and curl'ing endpoints.