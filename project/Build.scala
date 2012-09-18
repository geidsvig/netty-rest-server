import sbt._
import Keys._
import akka.sbt.AkkaKernelPlugin
import akka.sbt.AkkaKernelPlugin.{ Dist, outputDirectory, distJvmOptions}
 
object HootBombRestKernelBuild extends Build {
  val Organization = "ca.figmintgames"
  val Version      = "0.1"
  val ScalaVersion = "2.9.1"

  val appDependencies = Dependencies.restServer
  libraryDependencies ++= Dependencies.restServer
 
  lazy val NettyRestServer = Project(
    id = "netty-rest-server",
    base = file("."),
    settings = defaultSettings ++ AkkaKernelPlugin.distSettings ++ Seq(
      libraryDependencies ++= Dependencies.restServer,
      distJvmOptions in Dist := "-Xms512M -Xmx1024M -Xss1M -XX:+UseParallelGC -XX:GCTimeRatio=19",
      outputDirectory in Dist := file("target/rest-dist")
    )
  )
 
  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := Organization,
    version      := Version,
    scalaVersion := ScalaVersion,
    crossPaths   := false,
    organizationName := "Figmint Games",
    organizationHomepage := Some(url("http://www.figmint.ca")),
    libraryDependencies ++= Dependencies.restServer
  )
  
  lazy val defaultSettings = buildSettings ++ Seq(
    resolvers ++= Seq("repo.novus snaps" at "http://repo.novus.com/snapshots/",
      "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"),
 
    // compile options
    scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked"),
    javacOptions  ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    libraryDependencies ++= Dependencies.restServer
 
  )
}
 
object Dependencies {
  import Dependency._
 
  val restServer = Seq(
    akkaKernel, akkaSlf4j, logback, finagleCore, finagleMemcache, codahaleJerkson, specs2, akkaTestkit
  )
}
 
object Dependency {
	val akkaKernel = "com.typesafe.akka" % "akka-kernel" % "2.0.3"
	val akkaSlf4j = "com.typesafe.akka" % "akka-slf4j" % "2.0.3"
	val logback = "ch.qos.logback" % "logback-classic" % "1.0.0"

	val finagleCore = "com.twitter" % "finagle-core" % "5.0.3"
	val finagleMemcache = "com.twitter" % "finagle-memcached" % "5.0.3"
	val codahaleJerkson = "com.codahale" %% "jerkson" % "0.5.0"

	val specs2 = "org.specs2" %% "specs2" % "1.12.1" % "test"

	val akkaAmqp = "com.typesafe.akka" %% "akka-amqp" % "2.1-SNAPSHOT"
	val akkaTestkit = "com.typesafe.akka" % "akka-testkit" % "2.0.2" % "test"
}
