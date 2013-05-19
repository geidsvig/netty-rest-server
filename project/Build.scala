import sbt._
import Keys._
import akka.sbt.AkkaKernelPlugin
import akka.sbt.AkkaKernelPlugin.{ Dist, outputDirectory, distJvmOptions}
 
object NettyRestServerBuild extends Build {
  val Organization = "geidsvig"
  val Version      = "1.0"
  val ScalaVersion = "2.10.1"

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
    organizationName := "geidsvig",
    organizationHomepage := Some(url("https://github.com/geidsvig")),
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
    scalaReflect, akkaKernel, akkaSlf4j, logback, finagleCore, finagleMemcache, jerkson, scalaTest, akkaTestkit
  )
}
 
object Dependency {
	val akkaVersion = "2.1.2"
	val akkaKernel = "com.typesafe.akka" % "akka-kernel_2.10" % akkaVersion
	val akkaSlf4j = "com.typesafe.akka" % "akka-slf4j_2.10" % akkaVersion
	val logback = "ch.qos.logback" % "logback-classic" % "1.0.0"

	val finagleCore = "com.twitter" % "finagle-core" % "5.0.3"
	val finagleMemcache = "com.twitter" % "finagle-memcached" % "5.0.3"
    val jerkson = "com.cloudphysics" % "jerkson_2.10" % "0.6.3"
    val scalaReflect = "org.scala-lang" % "scala-reflect" % "2.10.1" 
    val scalaTest = "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
	val akkaTestkit = "com.typesafe.akka" % "akka-testkit" % "2.0.2" % "test"
}

