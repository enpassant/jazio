name := """jazio"""

version := "1.0.0-SNAPSHOT"

description := "Java IO effect library, inspired by the fantastic ZIO library"

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://s01.oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false },
  pomExtra := (
    <url>https://github.com/enpassant/jazio</url>
    <licenses>
      <license>
        <name>Apache-style</name>
        <url>https://opensource.org/licenses/Apache-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:enpassant/jazio.git</url>
      <connection>scm:git:git@github.com:enpassant/jazio.git</connection>
    </scm>
    <developers>
      <developer>
        <id>enpassant</id>
        <name>Enpassant</name>
        <email>enpassant.prog@gmail.com</email>
        <url>https://github.com/enpassant</url>
      </developer>
    </developers>)
)

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    publishSettings,
    javaOptions += "-Xmx512m",
    organization := "io.github.enpassant",
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql" % "42.2.10" % Test,
      "com.h2database" % "h2" % "1.4.197" % Test,
      "junit" % "junit" % "4.12" % Test,
      "com.novocode" % "junit-interface" % "0.11" % Test
    ),
    crossPaths := false,
    testOptions += Tests.Argument(TestFrameworks.JUnit)
  )


Test / parallelExecution := true

