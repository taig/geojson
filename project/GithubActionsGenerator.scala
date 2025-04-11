import io.circe.Json
import io.circe.syntax._

object GithubActionsGenerator {
  object Step {
    def setupJava(version: String): Json = Json.obj(
      "name" := "Setup Java JDK",
      "uses" := "actions/setup-java@v3",
      "with" := Json.obj(
        "distribution" := "temurin",
        "java-version" := version,
        "cache" := "sbt"
      )
    )

    val SetupSbt: Json = Json.obj(
      "name" := "Setup sbt",
      "uses" := "sbt/setup-sbt@v1"
    )

    val Checkout: Json = Json.obj(
      "name" := "Checkout",
      "uses" := "actions/checkout@v3",
      "with" := Json.obj(
        "fetch-depth" := 0
      )
    )
  }

  object Job {
    def apply(name: String, mode: String = "DEV", needs: List[String] = Nil)(steps: Json*): Json = Json.obj(
      "name" := name,
      "runs-on" := "ubuntu-latest",
      "env" := Json.obj(
        s"SBT_TPOLECAT_$mode" := "true"
      ),
      "needs" := needs,
      "steps" := steps
    )

    def blowout(javaVersion: String): Json = Job(name = "Blowout")(
      Step.Checkout,
      Step.setupJava(javaVersion),
      Step.SetupSbt,
      Json.obj("run" := "sbt blowoutCheck")
    )

    def scalafmt(javaVersion: String): Json = Job(name = "Scalafmt")(
      Step.Checkout,
      Step.setupJava(javaVersion),
      Step.SetupSbt,
      Json.obj("run" := "sbt scalafmtCheckAll")
    )

    def scalafix(javaVersion: String): Json = Job(name = "Scalafix", mode = "CI")(
      Step.Checkout,
      Step.setupJava(javaVersion),
      Step.SetupSbt,
      Json.obj("run" := "sbt scalafixCheckAll")
    )

    def lint(javaVersion: String): Json = Job(name = "Fatal warnings and code formatting")(
      Step.Checkout,
      Step.setupJava(javaVersion),
      Json.obj(
        "name" := "Workflows",
        "run" := "sbt -Dmode=ci blowoutCheck"
      ),
      Json.obj(
        "name" := "Code formatting",
        "run" := "sbt -Dmode=ci scalafmtCheckAll"
      ),
      Json.obj(
        "name" := "Fatal warnings",
        "run" := "sbt -Dmode=ci compile"
      )
    )

    def deploy(javaVersion: String): Json =
      Job(name = "Deploy", mode = "RELEASE", needs = List("blowout", "scalafmt", "scalafix"))(
        Step.Checkout,
        Step.setupJava(javaVersion),
        Step.SetupSbt,
        Json.obj(
          "name" := "Release",
          "run" := "sbt ci-release",
          "env" := Json.obj(
            "PGP_PASSPHRASE" := "${{secrets.PGP_PASSPHRASE}}",
            "PGP_SECRET" := "${{secrets.PGP_SECRET}}",
            "SONATYPE_PASSWORD" := "${{secrets.SONATYPE_PASSWORD}}",
            "SONATYPE_USERNAME" := "${{secrets.SONATYPE_USERNAME}}"
          )
        )
      )
  }

  def main(javaVersion: String): Json = Json.obj(
    "name" := "CI",
    "on" := Json.obj(
      "push" := Json.obj(
        "branches" := List("main")
      )
    ),
    "jobs" := Json.obj(
      "blowout" := Job.blowout(javaVersion),
      "scalafmt" := Job.scalafmt(javaVersion),
      "scalafix" := Job.scalafix(javaVersion),
      "deploy" := Job.deploy(javaVersion)
    )
  )

  def tag(javaVersion: String): Json = Json.obj(
    "name" := "CD",
    "on" := Json.obj(
      "push" := Json.obj(
        "tags" := List("*.*.*")
      )
    ),
    "jobs" := Json.obj(
      "blowout" := Job.blowout(javaVersion),
      "scalafmt" := Job.scalafmt(javaVersion),
      "scalafix" := Job.scalafix(javaVersion),
      "deploy" := Job.deploy(javaVersion)
    )
  )

  def pullRequest(javaVersion: String): Json = Json.obj(
    "name" := "CI",
    "on" := Json.obj(
      "pull_request" := Json.obj(
        "branches" := List("main")
      )
    ),
    "jobs" := Json.obj(
      "blowout" := Job.blowout(javaVersion),
      "scalafmt" := Job.scalafmt(javaVersion),
      "scalafix" := Job.scalafix(javaVersion)
    )
  )
}
