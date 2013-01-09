import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "JiraToSladiator"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "com.lunatech" %% "json-compare" % "1.0" % "test"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      resolvers += "Lunatech Artifactory" at "http://artifactory.lunatech.com/artifactory/releases-public"      
    )

}
