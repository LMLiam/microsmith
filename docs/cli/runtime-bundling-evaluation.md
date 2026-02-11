# Bundled Runtime Evaluation (Java 24 Friction)

Phase 5 requires evaluating whether the CLI should ship with an embedded Java runtime.

## Options considered

1. Keep Java external (current)
- Pros: smallest artifact, easiest release pipeline, no platform-specific runtime packaging.
- Cons: teams must install/manage Java 24 separately.

2. `jlink` runtime images per OS
- Pros: controlled runtime footprint, predictable JVM behavior.
- Cons: separate build matrix per OS/arch, larger assets, more release complexity.

3. Native installer bundles (`jpackage`)
- Pros: best end-user installation UX.
- Cons: highest packaging and signing overhead, platform-specific maintenance.

## Recommendation

Adopt a two-track distribution model:

1. Primary GA channel (implemented now):
- fat jar + launcher scripts
- explicit Java 24 requirement

2. Follow-up optional channel:
- platform-specific bundled runtime archives for teams that cannot pre-install Java 24
- produced from a dedicated release workflow matrix with checksum signing

This keeps Phase 5 shipping velocity while documenting a concrete path to lower adoption friction.

## Exit criteria for bundled runtime follow-up

- artifact size and startup benchmarks are acceptable
- CI can produce signed bundles for Windows/Linux/macOS
- support policy for runtime CVE patch cadence is agreed
