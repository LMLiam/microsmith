# Native Maven integration

Use the Maven plugin as the primary path for Java and Kotlin repositories that already build with Maven and want imported-project IDE support.

Prefer this path when:

- the repository already builds through Maven rather than Gradle
- generation should run as `mvn microsmith:generate`
- JetBrains IDEs should resolve built-in Microsmith `.microsmith.kts` symbols from the imported Maven project instead of a separate helper project

Canonical contract:

1. Keep Microsmith authoring in `build.microsmith.kts` or another `*.microsmith.kts` file.
2. Add `io.github.lmliam.microsmith:runtime-scripting` as a `provided` dependency so Maven-imported IDE projects see the built-in script definition and Microsmith types.
3. Add `io.github.lmliam.microsmith:maven-plugin` under `<build><plugins>`.
4. Configure `<repositories>` and `<pluginRepositories>` so both the project dependency and the Maven plugin resolve.
5. Run `mvn microsmith:generate`.

Minimal `pom.xml` example:

```xml
<project>
    <properties>
        <microsmith.version><!-- Microsmith version --></microsmith.version>
    </properties>

    <repositories>
        <repository>
            <id>github-microsmith</id>
            <url>https://maven.pkg.github.com/lmliam/microsmith</url>
        </repository>
    </repositories>

    <pluginRepositories>
        <pluginRepository>
            <id>github-microsmith</id>
            <url>https://maven.pkg.github.com/lmliam/microsmith</url>
        </pluginRepository>
    </pluginRepositories>

    <dependencies>
        <dependency>
            <groupId>io.github.lmliam.microsmith</groupId>
            <artifactId>runtime-scripting</artifactId>
            <version>${microsmith.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>io.github.lmliam.microsmith</groupId>
                <artifactId>maven-plugin</artifactId>
                <version>${microsmith.version}</version>
            </plugin>
        </plugins>
    </build>
</project>
```

Repository credentials:

- the local Maven repository at `~/.m2/repository` is enough for local fixture validation after `publishToMavenLocal`
- when consuming releases from GitHub Packages, configure a `github-microsmith` server entry in `~/.m2/settings.xml` or use `actions/setup-java` with `server-id: github-microsmith` in CI

External plugin example:

```xml
<plugin>
    <groupId>io.github.lmliam.microsmith</groupId>
    <artifactId>maven-plugin</artifactId>
    <version>${microsmith.version}</version>
    <dependencies>
        <dependency>
            <groupId>com.acme</groupId>
            <artifactId>microsmith-emitter-ts</artifactId>
            <version>1.4.2</version>
        </dependency>
    </dependencies>
</plugin>
```

If JetBrains IDEs also need those plugin-provided types in `.microsmith.kts`, mirror the plugin library as a `provided` project dependency or keep using the helper or fallback path for that repository.

Generated-source guidance:

- Microsmith does not auto-register generated directories as Java or Kotlin sources in Maven
- Microsmith writes generated `.proto` files under `./proto` by default
- only add concrete language source directories into the build when you have a real source-emission contract
- use the same explicit Maven source-registration approach you already use for other generated sources, such as `build-helper-maven-plugin`, rather than adding the entire output root blindly

Coexistence with the CLI, helper, and fallback paths:

- `microsmith init` still works in Maven repositories and is the fastest way to scaffold `build.microsmith.kts`
- once native Maven integration is adopted, use `mvn microsmith:generate` as the primary generation path
- the JetBrains IDE helper and fallback jar remain useful when you intentionally keep Microsmith outside Maven, or when a Maven-imported IDE project needs plugin-provided types that are not mirrored as project dependencies
- representative native Maven fixtures live in `examples/maven/java`, `examples/maven/kotlin`, and `examples/maven/dotnet`

[Back to the README](../README.md)
