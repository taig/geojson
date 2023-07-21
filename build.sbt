import sbtcrossproject.CrossPlugin.autoImport.CrossType
import sbtcrossproject.CrossProject

val Version = new {
  val CatsEffect = "3.5.1"
  val CatsMtl = "1.3.1"
  val Circe = "0.14.5"
  val Fs2 = "3.7.0"
  val Http4s = "1.0.0-M40"
  val Java = "17"
  val Log4Cats = "2.6.0"
  val Munit = "0.7.29"
  val MunitCatsEffect = "1.0.7"
  val Scala3 = "3.3.0"
  val Slf4j = "1.7.36"
}

def module(identifier: Option[String], jvmOnly: Boolean): CrossProject = {
  val platforms = List(JVMPlatform) ++ (if (jvmOnly) Nil else List(JSPlatform))
  CrossProject(identifier.getOrElse("root"), file(identifier.fold(".")("modules/" + _)))(platforms: _*)
    .crossType(CrossType.Pure)
    .withoutSuffixFor(JVMPlatform)
    .build()
    .settings(
      Compile / scalacOptions ++= "-source:future" :: "-rewrite" :: "-new-syntax" :: "-Wunused:all" :: Nil,
      name := "geojson" + identifier.fold("")("-" + _)
    )
}

inThisBuild(
  Def.settings(
    developers := List(Developer("taig", "Niklas Klein", "mail@taig.io", url("https://taig.io/"))),
    dynverVTagPrefix := false,
    homepage := Some(url("https://github.com/taig/geojson/")),
    licenses := List("MIT" -> url("https://raw.githubusercontent.com/taig/geojson/main/LICENSE")),
    organization := "io.taig",
    scalaVersion := Version.Scala3,
    versionScheme := Some("early-semver")
  )
)

lazy val root = module(identifier = None, jvmOnly = true)
  .enablePlugins(BlowoutYamlPlugin)
  .settings(noPublishSettings)
  .settings(
    blowoutGenerators ++= {
      val github = file(".github")
      val workflows = github / "workflows"

//      BlowoutYamlGenerator.lzy(workflows / "main.yml", GithubActionsGenerator.main(Version.Java)) ::
//        BlowoutYamlGenerator.lzy(workflows / "pull-request.yml", GithubActionsGenerator.pullRequest(Version.Java)) ::
      Nil
    }
  )
  .aggregate(core, circe)

lazy val core = module(Some("core"), jvmOnly = false)

lazy val circe = module(Some("circe"), jvmOnly = false)
  .settings(
    libraryDependencies ++=
      "io.circe" %%% "circe-core" % "0.14.5" ::
        Nil
  )
  .dependsOn(core)
