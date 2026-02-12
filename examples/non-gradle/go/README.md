# Go Fixture: Microsmith CLI

This fixture demonstrates invoking Microsmith from a Go repository without Gradle.

## Local

```bash
microsmith run schema.microsmith.kts --out ./internal/gen/proto
```

## CI

See `.github/workflows/microsmith.yml`.
