ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "BlockChain",
    libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor-typed" % "2.7.0",
        "com.typesafe.akka" %% "akka-stream" % "2.7.0",
        "com.typesafe.akka" %% "akka-slf4j" % "2.7.0",
        "com.typesafe.akka" %% "akka-persistence-typed" % "2.7.0",
        "com.typesafe.akka" %% "akka-persistence-testkit" % "2.7.0" % Test,
        "ch.qos.logback" % "logback-classic" % "1.4.14",
        "com.typesafe.akka" %% "akka-http" % "10.5.3",
        "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.3"

)
  )
