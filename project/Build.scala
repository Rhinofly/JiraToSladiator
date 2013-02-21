import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName = "JiraToSladiator"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.lunatech" %% "json-compare" % "1.0" % "test",
    "nl.rhinofly" %% "jira-exception-processor" % "2.0.1",
    // Play 2.0.4 uses Akka 2.0.2, which has an old internal typesafe-config lib,
    // that doesn't support explicit resource() urls.
    // We use a newer version of Akka, so we can use the newer and external typesafe config library.
    "com.typesafe.akka" % "akka-actor" % "2.0.5",
    "com.typesafe.akka" % "akka-slf4j" % "2.0.5",
    "com.typesafe" % "config" % "1.0.0")

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    resolvers += "Lunatech Artifactory" at "http://artifactory.lunatech.com/artifactory/releases-public")

}
