name := """jazio"""

version := "1.0.0-SNAPSHOT"

description := "Java IO effect library, inspired by the fantastic ZIO library"

//fork := true

//javacOptions += "-Xlint"

javaOptions += "-Xmx512m"

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.2.10" % Test,
  "com.h2database" % "h2" % "1.4.197" % Test,
  "junit" % "junit" % "4.12" % Test,
  "com.novocode" % "junit-interface" % "0.11" % Test
)

testOptions += Tests.Argument(TestFrameworks.JUnit)

Test / parallelExecution := true
