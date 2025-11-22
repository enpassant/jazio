name := """jazio"""

version := "2.0.0"

description := "Java IO effect library, inspired by the fantastic ZIO library"

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  ThisBuild / publishTo := {
    val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
    if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
    else localStaging.value
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
    //javacOptions += "-Xlint:unchecked",
    javaOptions += "-Xmx512m",
    organization := "io.github.enpassant",
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql" % "42.2.10" % Test,
      "com.h2database" % "h2" % "1.4.197" % Test,
      "org.junit.jupiter" % "junit-jupiter-api" % "6.0.1" % Test,
      "com.github.sbt.junit" % "jupiter-interface" % "0.17.0" % Test
    ),
    crossPaths := false,
    testOptions += Tests.Argument(TestFrameworks.JUnit)
  )


Test / parallelExecution := true

