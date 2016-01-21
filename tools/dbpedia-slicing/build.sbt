
lazy val root = (project in file(".")).settings(
  name := "DBpedia Dataset Slicing",
  version := "0.1",
  scalaVersion := scalaVer,
  libraryDependencies := libDeps,
  fork in run := true,
  javaOptions in run += "-Xmx192G",
  connectInput in run := true,
  mainClass in (Compile, run) := Some("de.uni.leipzig.sdw.dbpedia.slicing.LogTest"),
  mainClass in (Compile, packageBin) := Some("de.uni.leipzig.sdw.dbpedia.slicing.LogTest"),
  mainClass in (Compile, assembly) := Some("de.uni.leipzig.sdw.slicing.Filtering"),
  assemblyJarName in assembly := "dbp-flink-slicing.jar"
)

lazy val scalaVer = "2.11.7"
lazy val sbtVersion = "0.13.9"
lazy val scalazVersion= "7.1.+"
lazy val sesameVersion = "2.8.+"
lazy val bananaRdfVersion = "0.8.+"

lazy val libDeps = Seq(
  "org.w3" %% "banana-rdf" % bananaRdfVersion,
  "org.w3" %% "banana-jena" % bananaRdfVersion excludeAll(
    ExclusionRule("javax.xml.stream", "stax-api"),
    ExclusionRule("org.slf4j", "slf4j-api"),
    ExclusionRule("org.slf4j", "log4j12")
    ),
  "org.scalaz" %% "scalaz-core" % scalazVersion,
  "org.scalaz" %% "scalaz-effect" % scalazVersion,
  "org.apache.flink" %% "flink-scala" % "0.10.+" % "provided",
  "org.apache.flink" %% "flink-clients" % "0.10.+" % "provided",
  "org.clapper" %% "grizzled-slf4j" % "1.+",
  "org.apache.commons" % "commons-compress" % "1.+",
  "com.google.guava" % "guava" % "19.+",
  "org.scalatest" %% "scalatest" % "2.2.+" % Test
)
