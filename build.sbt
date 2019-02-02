lazy val V = new {
  val cats = "1.5.0"
  val catsEffect = "1.1.0"
  val circe = "0.10.0"
  val http4s = "0.20.0-M4"
  val log4j = "2.11.1"
  val pureconfig = "0.10.1"
  val log4cats = "0.2.0"
  val doobie = "0.6.0"
  val postgresql = "42.2.5.jre7"
  val flyway = "5.2.4"
}

lazy val finanzio = project
  .settings(
    scalaVersion := "2.12.8",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % V.cats,
      "org.typelevel" %% "cats-effect" % V.catsEffect,
      "org.http4s" %% "http4s-blaze-client" % V.http4s,
      "org.http4s" %% "http4s-circe" % V.http4s,
      "org.http4s" %% "http4s-dsl" % V.http4s,
      "io.circe" %% "circe-core" % V.circe,
      "io.circe" %% "circe-generic" % V.circe,
      "io.circe" %% "circe-generic-extras" % V.circe,
      "io.circe" %% "circe-java8" % V.circe,
      "org.apache.logging.log4j" % "log4j-core" % V.log4j % Runtime,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % V.log4j,
      "org.apache.logging.log4j" % "log4j-api" % V.log4j,
      "com.github.pureconfig" %% "pureconfig" % V.pureconfig,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % V.pureconfig,
      "io.chrisdavenport" %% "log4cats-slf4j" % V.log4cats,
      "org.tpolecat" %% "doobie-core" % V.doobie,
      "org.tpolecat" %% "doobie-postgres" % V.doobie,
      "org.tpolecat" %% "doobie-hikari" % V.doobie,
      "org.postgresql" % "postgresql" % V.postgresql,
      "org.flywaydb" % "flyway-core" % V.flyway
    ),
    scalacOptions ++= Seq(
      "-Ypartial-unification"
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8"),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )
