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
    name := "scala-sbt-native-fixture",
    resolvers += Resolver.mavenLocal,
    resolvers += "GitHub Microsmith" at "https://maven.pkg.github.com/lmliam/microsmith",
    credentials ++= githubMicrosmithCredentials,
    libraryDependencies += "me.liam.microsmith" % "runtime-scripting" % microsmithVersion % Provided,
  )
