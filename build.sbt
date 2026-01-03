import com.typesafe.sbt.packager.archetypes
import scala.sys.process._

// --- Global / Build-wide Settings ---
Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / organization := "jagfx"
ThisBuild / version := "0.2.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.7.4"

ThisBuild / semanticdbEnabled := true

// --- Dependency Versions ---
val javaFxVersion = "23.0.1"
val logbackVersion = "1.5.23"
val scribeVersion = "3.17.0"
val ikonliVersion = "12.4.0"
val munitVersion = "1.0.4"

// --- Project Definition ---
lazy val root = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "jagfx",

    // App Packaging
    Compile / mainClass := Some("jagfx.Launcher"),
    executableScriptName := "jagfx",
    maintainer := "xsyetopz",

    // Dependencies
    libraryDependencies ++= javaFxDependencies,
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "com.outr" %% "scribe-slf4j" % scribeVersion,
      "org.kordamp.ikonli" % "ikonli-javafx" % ikonliVersion,
      "org.kordamp.ikonli" % "ikonli-materialdesign2-pack" % ikonliVersion,
      "org.scalameta" %% "munit" % munitVersion % Test
    ),

    // Test & Run Configuration
    testFrameworks += new TestFramework("munit.Framework"),
    run / fork := true,
    run / connectInput := true,
    outputStrategy := Some(StdoutOutput)
  )

// --- Command Aliases ---
addCommandAlias("cli", "runMain jagfx.JagFXCli")
addCommandAlias("dist", "universal:packageBin")

// --- Helper Functions & Tasks ---
lazy val javaFxDependencies = {
  val modules =
    Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
  val platforms = Seq("win", "linux", "mac", "mac-aarch64")

  for {
    m <- modules
    p <- platforms
  } yield "org.openjfx" % s"javafx-$m" % javaFxVersion classifier p
}

lazy val scss = taskKey[Unit]("Compile SCSS to CSS")
scss := {
  def isToolAvailable(tool: String): Boolean =
    try Process(Seq("which", tool)).! == 0
    catch { case _: Exception => false }

  val compiler =
    if (isToolAvailable("bunx")) "bunx"
    else if (isToolAvailable("npx")) "npx"
    else
      throw new Exception(
        "SCSS compilation failed: neither 'bunx' nor 'npx' found in PATH"
      )

  val src = "src/main/scss/style.scss"
  val dst = "src/main/resources/jagfx/style.css"
  val exit = s"$compiler sass $src $dst --no-source-map".!
  if (exit != 0) throw new Exception(s"SCSS compilation failed with code $exit")
}

Compile / compile := ((Compile / compile) dependsOn scss).value
