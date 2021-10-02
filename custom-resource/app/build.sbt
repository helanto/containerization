name := "app"

version := "0.0.1"

scalaVersion := "3.0.2"

// https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
libraryDependencies += "com.squareup.okhttp3" % "okhttp"     % "4.9.2"
// https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp-tls
libraryDependencies += "com.squareup.okhttp3" % "okhttp-tls" % "4.9.2"

// https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
libraryDependencies += "com.fasterxml.jackson.core"    % "jackson-databind"     % "2.13.0"
// https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-scala
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.0"
// https://mvnrepository.com/artifact/com.github.scopt/scopt
libraryDependencies += "com.github.scopt"             %% "scopt"                % "4.0.1"

// Deactivate tests when running assembly
assembly / test := {}
// Resolve assembly conflicts
assembly / assemblyMergeStrategy := {
  case PathList(xs @ _*) if xs.last == "module-info.class" => MergeStrategy.discard
  case x                                                   => (assembly / assemblyMergeStrategy).value.apply(x) // keep old strategy
}

// location of formatter configuration file.
scalafmtConfig := file(".scalafmt.conf")

// Check format before compilation.
Compile / compile := (Compile / compile dependsOn scalafmtCheckAll).value
