ThisBuild / scalaVersion := "2.12.10"

lazy val globalSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "utf-8",                            // Specify character encoding used by source files.
    "-explaintypes",                    // Explain type errors in more detail.
    "-feature",                         // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",           // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros",    // Allow macro definition (besides implementation and application)
    "-language:higherKinds",            // Allow higher-kinded types
    "-language:implicitConversions",    // Allow definition of implicit functions called views
    "-unchecked",                       // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                      // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings",                 // Fail the compilation if there are any warnings.
    "-Xfuture",                         // Turn on future language features.
    "-Xlint:adapted-args",              // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
    "-Xlint:constant",                  // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select",        // Selecting member of DelayedInit.
    "-Xlint:doc-detached",              // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",              // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                 // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",      // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override",          // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit",              // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",           // Option.apply used implicit view.
    "-Xlint:package-object-classes",    // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",    // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",            // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",               // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",     // A local type parameter shadows a type already in scope.
    "-Xlint:unsound-match",             // Pattern match may not be typesafe.
    "-Yno-adapted-args",                // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification",            // Enable partial unification in type constructor inference
    "-Ywarn-dead-code",                 // Warn when dead code is identified.
    "-Ywarn-extra-implicit",            // Warn when more than one implicit parameter section is defined.
    "-Ywarn-inaccessible",              // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any",                 // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override",          // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit",              // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen",             // Warn when numerics are widened.
    "-Ywarn-unused:implicits",          // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports",            // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals",             // Warn if a local definition is unused.
    "-Ywarn-unused:params",             // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars",            // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates",           // Warn if a private member is unused.
    "-Ywarn-value-discard"              // Warn when non-Unit expression results are unused.
  ),
  scalacOptions in (Compile, console) := scalacOptions.value.filterNot(_.startsWith("-Ywarn-unused")).filterNot(
    Set("-Xfatal-warnings")
  )
) ++ compilerPlugins

lazy val compilerPlugins = Seq(
  libraryDependencies ++= Seq(
    compilerPlugin("com.olegpy"      %% "better-monadic-for" % "0.3.1"),
    compilerPlugin("org.typelevel"   %% "kind-projector"     % "0.10.3"),
    compilerPlugin("org.scalamacros" %% "paradise"           % "2.1.1" cross CrossVersion.full)
  )
)

lazy val catodromo = (project in file("."))
  .settings(globalSettings)
  .aggregate(core)

lazy val core = (project in file("core"))
  .settings(globalSettings)
  .settings(
    moduleName := "catodromo-core",
    libraryDependencies ++= Seq(
      "org.hdrhistogram"           % "HdrHistogram"   % Versions.hdrHistogram,
      "com.github.julien-truffaut" %% "monocle-core"  % Versions.monocle,
      "com.github.julien-truffaut" %% "monocle-macro" % Versions.monocle,
      "org.typelevel"              %% "cats-effect"   % Versions.cats.effect,
      "org.typelevel"              %% "kittens"       % Versions.cats.kittens,
      "co.fs2"                     %% "fs2-core"      % Versions.fs2
    ),
    initialCommands in console += Seq(
      "import cats._",
      "import cats.implicits._",
      "import cats.effect._",
      "import cats.metrics._",
      "import cats.metrics.store._",
      "import scala.concurrent.duration._",
      "import scala.concurrent.ExecutionContext",
      "implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)",
      "implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)"
    ).mkString("\n")
  )

addCommandAlias("fmt", "scalafmtSbt;scalafmt")
