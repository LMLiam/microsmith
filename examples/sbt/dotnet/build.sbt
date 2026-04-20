ThisBuild / scalaVersion := "3.7.1"

val microsmithVersion =
  sys.props.getOrElse(
    "microsmith.version",
    sys.error("Pass -Dmicrosmith.version=<version>."),
  )

def githubMicrosmithCredentials: Seq[Credentials] =
  for {
    actor <- sys.env.get("GITHUB_ACTOR").toSeq
    token <- sys.env.get("GITHUB_TOKEN").toSeq
  } yield Credentials("GitHub Package Registry", "maven.pkg.github.com", actor, token)

lazy val root = (project in file("."))
  .enablePlugins(MicrosmithSbtPlugin)
  .settings(
    name := "dotnet-sbt-native-fixture",
    microsmithOutputDirectory := baseDirectory.value / "Generated",
    resolvers += Resolver.mavenLocal,
    resolvers += "GitHub Microsmith" at "https://maven.pkg.github.com/lmliam/microsmith",
    credentials ++= githubMicrosmithCredentials,
    libraryDependencies += "io.github.lmliam.microsmith" % "runtime-scripting" % microsmithVersion % Provided,
  )
