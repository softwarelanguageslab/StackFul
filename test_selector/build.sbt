name := "Concolic_Testing_Backend"

version := "0.1"

scalaVersion := "2.13.6"

mappings in (Compile, packageBin) ++= (mappings in (Compile, packageSrc)).value

// http://www.scalatest.org/user_guide/using_scalatest_with_sbt
libraryDependencies ++= Seq(
	"org.scalactic" %% "scalactic" % "3.0.+",
	"org.scalatest" %% "scalatest" % "3.2.+" % "test",
	"io.spray" %%  "spray-json" % "1.3.+",
	"com.github.scopt" %% "scopt" % "4.0.1+"
)

javaOptions in run += "-Djava.library.path=/Users/mvdcamme/PhD/Projects/Concolic_Execution/z3-master/build"

// [Required] Enable plugin and automatically find def main(args:Array[String]) methods from the classpath
enablePlugins(PackPlugin)
packMain := Map("backend" -> "backend.Main")

/*
 * Assembly
 */
val pathToMain: String = "backend.Main"
assemblyMergeStrategy in assembly := {
case x if Assembly.isConfigFile(x) =>
  MergeStrategy.concat
case PathList(ps @ _*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
  MergeStrategy.rename
case PathList("META-INF", xs @ _*) =>
  (xs map {_.toLowerCase}) match {
    case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
      MergeStrategy.discard
    case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
      MergeStrategy.discard
    case "plexus" :: xs =>
      MergeStrategy.discard
    case "services" :: xs =>
      MergeStrategy.filterDistinctLines
    case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
      MergeStrategy.filterDistinctLines
    case _ => MergeStrategy.first
  }
case _ => MergeStrategy.first}
test in assembly := {}
mainClass in assembly := Some(pathToMain)