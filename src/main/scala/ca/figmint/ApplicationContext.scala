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
	val restServerSystem = ActorSystem("restServerSystem", ConfigFactory.load.getConfig("restServerSystem"))
	val hostname = restServerSystem.settings.config.getString("restServerSystem.hostname")
	val port = restServerSystem.settings.config.getInt("restServerSystem.http.port")
	val logger = restServerSystem.log
	
	logger info("Application started.")

	val apiHandler = restServerSystem.actorOf(Props[ApplicationAPIHandler], "apiHandler")	
}

class ApplicationAPIHandler extends StatusHandler {
	val logger = ApplicationContext.logger
}

class ApplicationRestServer extends RestServer
	with RestServerProperties

class ApplicationRouteHandler extends RestRouteHandler 
	with RestServerRoutehandlerProperties {
	val apiStatusPath = new Regex("""/status""")
	val apiHandler = ApplicationContext.apiHandler
}

class ApplicationPipelineFactory extends RestServerPipelineFactory
	with RestServerPipelineFactoryProperties

trait RestServerProperties extends RestServerRequirements {
	val logger = ApplicationContext.logger
	val port = ApplicationContext.port
	val pipelineFactory = new ApplicationPipelineFactory
	val timeout = 4000L
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

