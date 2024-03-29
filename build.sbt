inThisBuild(List(
  crossScalaVersions            := Seq("2.13.8"),
  scalaVersion                  := crossScalaVersions.value.head
))

lazy val globalSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "utf-8",                         // Specify character encoding used by source files.
    "-explaintypes",                 // Explain type errors in more detail.
    "-feature",                      // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",        // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros", // Allow macro definition (besides implementation and application)
    "-language:higherKinds",         // Allow higher-kinded types
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-unchecked",                    // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                   // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings",              // Fail the compilation if there are any warnings.
    "-Xlint:adapted-args",           // Warn if an argument list is modified to match the receiver.
    "-Xlint:constant",               // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select",     // Selecting member of DelayedInit.
    "-Xlint:doc-detached",           // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",           // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",              // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",   // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-unit",           // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",        // Option.apply used implicit view.
    "-Xlint:package-object-classes", // Class or object defined in package object.
    "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",         // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",            // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",  // A local type parameter shadows a type already in scope.
    "-Ywarn-dead-code",              // Warn when dead code is identified.
    "-Ywarn-extra-implicit",         // Warn when more than one implicit parameter section is defined.
    "-Ywarn-numeric-widen",          // Warn when numerics are widened.
    "-Ywarn-unused:implicits",       // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports",         // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals",          // Warn if a local definition is unused.
    //"-Ywarn-unused:params",          // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars",         // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates",        // Warn if a private member is unused.
    "-Ywarn-value-discard"           // Warn when non-Unit expression results are unused.
  ),
  Compile / console / scalacOptions := scalacOptions.value
    .filterNot(_.startsWith("-Ywarn-unused"))
    .filterNot(
      Set("-Xfatal-warnings")
    ),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, x)) if x <= 12 =>
        Seq(
          "-Xfuture",                        // Turn on future language features.
          "-Xlint:unsound-match",            // Pattern match may not be typesafe.
          "-Yno-adapted-args",               // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
          "-Ypartial-unification",           // Enable partial unification in type constructor inference
          "-Ywarn-inaccessible",             // Warn about inaccessible types in method signatures.
          "-Ywarn-infer-any",                // Warn when a type argument is inferred to be `Any`.
          "-Ywarn-nullary-override",         // Warn when non-nullary `def f()' overrides nullary `def f'.
          "-Ywarn-nullary-unit",             // Warn when nullary methods return Unit.
          "-Xlint:by-name-right-associative" // By-name parameter of right associative operator.
        )

      case Some((2, x)) if x == 13 =>
        Seq("-Ymacro-annotations")

      case _ => Nil
    }
  }
) ++ compilerPlugins

lazy val compilerPlugins = Seq(
  libraryDependencies ++= Seq(
    compilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
    compilerPlugin("org.typelevel" % "kind-projector"     % "0.13.2" cross CrossVersion.full)
  ),
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, x)) if x <= 12 =>
        Seq(compilerPlugin("org.scalamacros" %% "paradise" % "2.1.1" cross CrossVersion.full))

      case _ => Nil
    }
  }
)

lazy val moduleSettings = Seq(
  console / initialCommands += Seq(
    "import cats._",
    "import cats.implicits._",
    "import cats.effect._",
    "import cats.metrics._",
    "import cats.metrics.store._",
    "import scala.concurrent.duration._",
    "import scala.concurrent.ExecutionContext",
  ).mkString("\n")
)

lazy val catodromo = (project in file("."))
  .settings(globalSettings)
  .aggregate(core.jvm, core.js, jmx, graphite)

// Modules

lazy val core = (crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Full) in file("modules/core"))
  .settings(globalSettings)
  .settings(moduleSettings)
  .settings(
    moduleName := "catodromo-core",
    libraryDependencies ++= Seq(
      "dev.optics" %%% "monocle-core"  % Versions.monocle,
      "dev.optics" %%% "monocle-macro" % Versions.monocle,
      "org.typelevel"              %%% "cats-effect"   % Versions.cats.effect,
      "org.typelevel"              %%% "kittens"       % Versions.cats.kittens,
      "co.fs2"                     %%% "fs2-core"      % Versions.fs2
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.hdrhistogram"           % "HdrHistogram"   % Versions.hdrHistogram
    )
  )

lazy val jmx = (project in file("modules/jmx"))
  .settings(globalSettings)
  .settings(moduleSettings)
  .settings(
    moduleName := "catodromo-jmx"
  )
  .dependsOn(core.jvm)

lazy val graphite = (project in file("modules/graphite"))
  .settings(globalSettings)
  .settings(moduleSettings)
  .settings(
    moduleName := "catodromo-graphite",
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-io" % Versions.fs2
    )
  )
  .dependsOn(core.jvm)

// Examples

lazy val graphiteExample = (project in file("examples/graphite-carbon"))
  .settings(globalSettings)
  .settings(moduleSettings)
  .settings(
    moduleName := "catodromo-example-graphite"
  )
  .dependsOn(graphite)

// Command Alisaes

addCommandAlias("fmt", "scalafmtSbt;scalafmt")
