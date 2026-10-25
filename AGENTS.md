<!-- BEGIN opencode-rag -->
## Code Navigation

OpenCodeRAG semantic search is available in this workspace. Use it when it helps:
- `search_semantic(query)` for broad code questions.
- `get_file_skeleton(filePath)` to orient before reading a large file.
- `find_usages(symbolName)` before changing a symbol.
- `describe_image(filePath)` for visual assets.

Run `opencode-rag index` after large changes if results look stale.

Quirks are optional project-local notes. Use `recall_quirks` and `add_quirk` when they help this project. Durable memory belongs in supermemory.
<!-- END opencode-rag -->

## Testing policy

Write tests that protect behaviour. Cover what can regress. Do not chase a coverage number.

### Layers

Use the lowest layer that can observe the behaviour.

| Layer | Scope | Where |
|---|---|---|
| Unit | One function, builder, or resolver. No process or network. A temporary directory is allowed when the unit writes files. | `src/test`, production package |
| Integration | Several pipeline stages composed, with real collaborators and temporary directories. | `src/integrationTest` |
| End-to-end | A generated project built and run by its real toolchain, or the shipped artifact. | `src/e2eTest`, or a CI workflow |

Each layer has its own Gradle task: `./gradlew kotest`, `./gradlew integrationTest`, and `./gradlew e2eTest`. `check` runs all three.

- Do not add an end-to-end check for behaviour that a lower layer already observes.
- Do not add a lower-layer test that only repeats an end-to-end check.

### Removed behaviour

Do not test that something no longer exists. Delete the test together with the code.

A test that asserts a removed flag, command, file, or type is rejected is not a regression test. It cannot fail for a useful reason, and the parser, the build, or the compiler already rejects the change. Keep one test for the general rule, such as an unknown option, and delete the per-case copies.

Do not keep a test only to prove that an old behaviour is gone. If a removal needs a permanent guard, that guard is a compiler error or a build check, not a test.

### End-to-end checks

An end-to-end test is allowed only for what unit and integration tests cannot reach: packaging, archive permissions, installers, a generated project compiled by its real toolchain, and launcher behaviour on a real operating system.

- Prefer a local end-to-end test when the toolchain runs on a developer machine. Put it in `src/e2eTest` and skip it when the toolchain is absent.
- Use a CI step for what a local test cannot do: several operating systems, the published installer, and the release archives.
- Name a CI step `Verify <target>`. Do not name it `Smoke test <target>`. A smoke test is a build-stability check, not a general end-to-end test.
- Run the test against a built artifact or a generated project. Do not run it against classes on the test classpath.
- Do not add a manual check without recording it as an automated test or a CI step.

### Test quality

Answer these questions for every test. Rewrite the test if an answer is "no".

1. Does it still pass after an internal rewrite that keeps the same behaviour?
2. Does it assert what the code should do, not what it does?
3. If it passes, do you trust the behaviour?
4. Does it go through the public API?
5. Does it assert state or output, not call order?

- Use real collaborators. Use a test double only at a boundary: network, file system, clock.
- Use a temporary directory instead of a fake file system.
- Assert one behaviour in each test. Name the test after the scenario and the expected outcome.
- Do not test a library, a generated accessor, a data class, or a constant.
- For a defect, add the failing test first. Confirm that it fails for the expected reason.

### Documentation

Do not test documentation text against program output. Remove the duplicate instead. The CLI usage text has one source: `microsmith --help`.
