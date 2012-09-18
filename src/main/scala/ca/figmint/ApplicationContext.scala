package ca.figmint

import scala.util.matching.Regex

import com.typesafe.config.ConfigFactory

import akka.actor.ActorSystem
import akka.actor.Props
import ca.figmint.netty.RestServer
import ca.figmint.netty.RestServerPipelineFactorRequirements
import ca.figmint.netty.RestServerPipelineFactory
import ca.figmint.netty.RestServerRequirements
import ca.figmint.netty.RestServerRouteHandlerRequirements
import ca.figmint.rest.RestRouteHandler
import ca.figmint.rest.service.StatusHandler

object ApplicationContext {
	val actorSystem = ActorSystem("actorSystem", ConfigFactory.load.getConfig("actorSystem"))
	val hostname = actorSystem.settings.config.getString("restServer.hostname")
	val port = actorSystem.settings.config.getInt("restServer.http.port")
	val logger = actorSystem.log
	
	logger info("Application started.")

	val apiHandler = actorSystem.actorOf(Props[ApplicationAPIHandler], "apiHandler")	
}

class ApplicationAPIHandler extends StatusHandler {
	val logger = ApplicationContext.logger
}

class ApplicationRestServer extends RestServer with RestServerProperties

class ApplicationRouteHandler extends RestRouteHandler with RestServerRoutehandlerProperties {
	 val apiStatusPath = new Regex("""/status""")
	 val apiHandler = ApplicationContext.apiHandler
}

class ApplicationPipelineFactory extends RestServerPipelineFactory with RestServerPipelineFactoryProperties

trait RestServerProperties extends RestServerRequirements {
	val logger = ApplicationContext.logger
	val port = ApplicationContext.port
	val pipelineFactory = new ApplicationPipelineFactory
} 

trait RestServerPipelineFactoryProperties extends RestServerPipelineFactorRequirements {
	val logger = ApplicationContext.logger
	val routeHandler = new ApplicationRouteHandler
	val chunkSize = 16
}

trait RestServerRoutehandlerProperties extends RestServerRouteHandlerRequirements {
	val logger = ApplicationContext.logger
	val instantiationTime = System.currentTimeMillis()
}

