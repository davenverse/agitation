val catsV = "2.4.2"
val catsEffectV = "3.0.0-RC2"
val specs2V = "4.10.6"
val kindProjectorV = "0.10.3"
val betterMonadicForV = "0.3.1"

ThisBuild / githubWorkflowArtifactUpload := false

val Scala213 = "2.13.4"

ThisBuild / crossScalaVersions := Seq("2.12.13", Scala213)
ThisBuild / scalaVersion := crossScalaVersions.value.last

ThisBuild / githubWorkflowArtifactUpload := false

val Scala213Cond = s"matrix.scala == '$Scala213'"

def rubySetupSteps(cond: Option[String]) = Seq(
  WorkflowStep.Use(
    UseRef.Public("ruby", "setup-ruby", "v1"),
    name = Some("Setup Ruby"),
    params = Map("ruby-version" -> "2.7"),
    cond = cond
  ),
  WorkflowStep.Run(
    List("gem install saas", "gem install jekyll -v 4"),
    name = Some("Install microsite dependencies"),
    cond = cond
  )
)

ThisBuild / githubWorkflowBuildPreamble ++=
  rubySetupSteps(Some(Scala213Cond))

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("test", "mimaReportBinaryIssues")),
  WorkflowStep.Sbt(List("site/makeMicrosite"), cond = Some(Scala213Cond))
)

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")

// currently only publishing tags
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(RefPredicate.StartsWith(Ref.Tag("v")))

ThisBuild / githubWorkflowPublishPreamble ++=
  WorkflowStep.Use(UseRef.Public("olafurpg", "setup-gpg", "v3")) +: rubySetupSteps(None)

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    name = Some("Publish artifacts to Sonatype"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  ),
  WorkflowStep.Sbt(
    List(s"++$Scala213", "site/publishMicrosite"),
    name = Some("Publish microsite")
  )
)

lazy val `agitation` = project
  .in(file("."))
  .disablePlugins(MimaPlugin)
  .enablePlugins(NoPublishPlugin)
  .settings(commonSettings)
  .aggregate(core.js, core.jvm)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .enablePlugins(MimaPlugin)
  .settings(commonSettings)
  .settings(
    name := "agitation",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core"          % catsV,
      "org.typelevel" %%% "cats-effect-kernel" % catsEffectV,
      "org.typelevel" %%% "cats-effect"        % catsEffectV % Test,
      "org.typelevel" %%% "cats-effect-laws"   % catsEffectV % Test,
      "org.specs2" %%% "specs2-core"           % specs2V     % Test,
      "org.specs2" %%% "specs2-scalacheck"     % specs2V     % Test
    )
  )

lazy val site = project
  .in(file("site"))
  .settings(commonSettings)
  .dependsOn(core.jvm)
  .disablePlugins(MimaPlugin)
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(MdocPlugin)
  .enablePlugins(NoPublishPlugin)
  .settings {
    import microsites._
    Seq(
      micrositeName := "agitation",
      micrositeDescription := "A Control Structure for Cancellation",
      micrositeAuthor := "Christopher Davenport",
      micrositeGithubOwner := "ChristopherDavenport",
      micrositeGithubRepo := "agitation",
      micrositeBaseUrl := "/agitation",
      micrositeDocumentationUrl := "https://www.javadoc.io/doc/io.chrisdavenport/agitation_2.12",
      micrositeFooterText := None,
      micrositeHighlightTheme := "atom-one-light",
      micrositePalette := Map(
        "brand-primary" -> "#3e5b95",
        "brand-secondary" -> "#294066",
        "brand-tertiary" -> "#2d5799",
        "gray-dark" -> "#49494B",
        "gray" -> "#7B7B7E",
        "gray-light" -> "#E5E5E6",
        "gray-lighter" -> "#F4F3F4",
        "white-color" -> "#FFFFFF"
      ),
      fork in mdoc := true,
      scalacOptions ~= filterConsoleScalacOptions,
      libraryDependencies += "com.47deg" %% "github4s" % "0.28.2",
      micrositePushSiteWith := GitHub4s,
      micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
      micrositeSearchEnabled := false,
      micrositeExtraMdFiles := Map(
        file("CODE_OF_CONDUCT.md") -> ExtraMdFileConfig(
          "code-of-conduct.md",
          "page",
          Map("title" -> "code of conduct", "section" -> "code of conduct", "position" -> "100")
        ),
        file("LICENSE") -> ExtraMdFileConfig(
          "license.md",
          "page",
          Map("title" -> "license", "section" -> "license", "position" -> "101")
        )
      )
    )
  }

// General Settings
lazy val commonSettings = Seq(
  addCompilerPlugin("org.typelevel" % "kind-projector"     % kindProjectorV cross CrossVersion.binary),
  addCompilerPlugin("com.olegpy"   %% "better-monadic-for" % betterMonadicForV)
)

// Global Settings
inThisBuild(
  List(
    organization := "io.chrisdavenport",
    developers := List(
      Developer(
        "ChristopherDavenport",
        "Christopher Davenport",
        "chris@christopherdavenport.tech",
        url("https://github.com/ChristopherDavenport")
      )
    ),
    homepage := Some(url("https://github.com/ChristopherDavenport/agitation")),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    pomIncludeRepository := { _ => false },
    scalacOptions in (Compile, doc) ++= Seq(
      "-groups",
      "-sourcepath",
      (baseDirectory in LocalRootProject).value.getAbsolutePath,
      "-doc-source-url",
      "https://github.com/ChristopherDavenport/agitation/blob/v" + version.value + "€{FILE_PATH}.scala"
    )
  )
)
