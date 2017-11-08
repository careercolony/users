lazy val root = (project in file(".")).enablePlugins(JavaAppPackaging)
name := """akka-http-docker-careercolony-starter"""

version := "1.0"

scalaVersion := "2.11.8"

packageName in Docker := "akka-http-docker-careercolony-user"
dockerExposedPorts := Seq(8081)

organization := "com.careercolony"

val akkaV = "2.4.5"
libraryDependencies ++= Seq(
  "org.neo4j.driver" % "neo4j-java-driver" % "1.0.4",
  "io.spray" %%  "spray-json" % "1.3.3",
  "com.typesafe.akka" %% "akka-http-core" % akkaV,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaV,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaV % "test",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV,
  "org.scalatest"     %% "scalatest" % "2.2.6" % "test",
  "ch.megard" %% "akka-http-cors" % "0.1.8",
  "com.jason-goodwin" %% "authentikat-jwt" % "0.4.5"

)
