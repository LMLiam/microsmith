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

Write a test to catch a regression. Do not write a test to describe the current code.

### Layers

Use the lowest layer that can observe the behaviour.

| Layer | Scope | Where |
|---|---|---|
| Unit | One function, builder, or resolver. No file system, no process, no network. | `modules/*/src/test` |
| Integration | Pipeline stages composed, with real collaborators and temporary directories. | `modules/*/src/test` |
| End-to-end | The shipped artifact: dist launcher, archive, installer, or generated project compiled by its real toolchain. | `.github/workflows` |

- Do not add an end-to-end check for behaviour that a lower layer already observes.
- Do not add a lower-layer test that only repeats an end-to-end check.

### End-to-end checks

An end-to-end check is allowed only for what unit and integration tests cannot reach: packaging, archive permissions, installers, and generated output compiled by a real toolchain.

- Name the step `Verify <target>`. Do not name it `Smoke test <target>`. A smoke test is a build-stability check, not an ad-hoc activity.
- Run the check against a built artifact. Do not run it against classes on the test classpath.
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
