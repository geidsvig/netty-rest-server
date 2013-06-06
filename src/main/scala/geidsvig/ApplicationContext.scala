package geidsvig

import scala.util.matching.Regex
import org.jboss.netty.handler.codec.http.HttpMethod
import com.typesafe.config.ConfigFactory
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import geidsvig.netty.rest.RestRouteHandler
import geidsvig.netty.rest.RestRouteHandlerRequirements
import geidsvig.netty.rest.status.StatusHandler
import geidsvig.netty.server.RestServer
import geidsvig.netty.server.RestServerPipelineFactorRequirements
import geidsvig.netty.server.RestServerPipelineFactory
import geidsvig.netty.server.RestServerRequirements
import akka.actor.Actor
import geidsvig.netty.rest.RestUtils
import com.twitter.finagle.memcached.protocol.NotFound
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import geidsvig.netty.rest.RestPathHandler
import geidsvig.netty.socket.comet.CometManager
import geidsvig.netty.socket.ws.WebSocketManager
import geidsvig.netty.socket.comet.CometHandlerFactory
import geidsvig.netty.socket.comet.CometManagerRequirements
import akka.event.LoggingAdapter
import geidsvig.netty.socket.ws.WebSocketManagerRequirements
import geidsvig.netty.socket.comet.CometHandler
import geidsvig.netty.socket.comet.CometHandlerRequirements
import geidsvig.netty.socket.comet.CometResponse
import geidsvig.netty.socket.comet.CometPacket
import org.jboss.netty.handler.codec.http.HttpRequest
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import geidsvig.netty.socket.ws.WebSocketHandlerFactory
import geidsvig.netty.socket.ws.WebSocketHandler
import geidsvig.netty.socket.ws.WebSocketHandlerRequirements

object ApplicationContext {
  val system = ActorSystem("restServerSystem", ConfigFactory.load.getConfig("restServerSystem"))

  val domain = system.settings.config.getString("restServerSystem.domain")
  val hostname = system.settings.config.getString("restServerSystem.hostname")
  val port = system.settings.config.getInt("restServerSystem.http.port")

  val logger = system.log

  logger info ("Application started.")

  val statusHandler: ActorRef = system.actorOf(Props[ApplicationStatusHandler], "statusHandler")

  val voidActor: ActorRef = system.actorOf(Props[VoidActor], "voidActor")
  
  val cometManager: CometManager = new MockCometManager
  
  val webSocketManager: WebSocketManager = new MockWebSocketManager
}

class ApplicationStatusHandler extends StatusHandler {
  val logger = ApplicationContext.logger
}

class ApplicationRestServer extends RestServer
  with RestServerProperties

class ApplicationRouteHandler extends RestRouteHandler
  with RestServerRouteHandlerProperties

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
  val chunkSize = 1024
}

trait SimpleCometHandlerDependencies extends CometHandlerRequirements {
  val receiveTimeout: Long = 30000
  val responseTimeout: Long = 10000
  val cometManager: CometManager = ApplicationContext.cometManager
}

class SimpleCometHandler(uuid: String) extends CometHandler(uuid) with SimpleCometHandlerDependencies {
  import scala.concurrent.ExecutionContext.Implicits.global
  def handleRequest(request: HttpRequest) {
    sendResponse(HttpResponseStatus.OK, "mock response")
  }
}

class MockCometHandlerFactory extends CometHandlerFactory {
  def createCometHandler(uuid: String): ActorRef = ApplicationContext.system.actorOf(Props(new SimpleCometHandler(uuid)))
}

trait TestCometManagerDependencies extends CometManagerRequirements {
  val cometHandlerFactory: CometHandlerFactory = new MockCometHandlerFactory
  val logger: LoggingAdapter = ApplicationContext.system.log
}

class MockCometManager extends CometManager with TestCometManagerDependencies {
  def hasRegisteredHandler(uuid: String): Option[ActorRef] = None
  def registerHandler(uuid: String) {}
  def deregisterHandler(uuid: String) {}
}

trait SimpleWebSocketHandlerDependencies extends WebSocketHandlerRequirements {
  val webSocketManager: WebSocketManager = ApplicationContext.webSocketManager
}

class SimpleWebSocketHandler(uuid: String) extends WebSocketHandler(uuid) with SimpleWebSocketHandlerDependencies {
  import scala.concurrent.ExecutionContext.Implicits.global
  // TODO heartbeat every 5 seconds with a text frame
  def handlePayload(payload: String) {
    sendResponse("mock response")
  }
}

class MockWebSocketHandlerFactory extends WebSocketHandlerFactory {
  def createWebSocketHandler(uuid: String): ActorRef = ApplicationContext.system.actorOf(Props(new SimpleWebSocketHandler(uuid)))
}

trait TestWebSocketManagerDependencies extends WebSocketManagerRequirements {
  val webSocketHandlerFactory: WebSocketHandlerFactory = new MockWebSocketHandlerFactory
  val logger: LoggingAdapter = ApplicationContext.system.log
}

class MockWebSocketManager extends WebSocketManager with TestWebSocketManagerDependencies {
  def hasRegisteredHandler(uuid: String): Option[ActorRef] = None
  def registerHandler(uuid: String) {}
  def deregisterHandler(uuid: String) {}
}

class VoidActor extends Actor with RestUtils {
  def receive = { case _ => {} }
}

trait RestServerRouteHandlerProperties extends RestRouteHandlerRequirements {
  val logger = ApplicationContext.logger
  val instantiationTime = System.currentTimeMillis()

  val pathsAndHandlers: Set[RestPathHandler] = Set(
    RestPathHandler(HttpMethod.GET, new Regex("""/status"""), ApplicationContext.statusHandler))

  val cometManager: CometManager = ApplicationContext.cometManager
  val webSocketManager: WebSocketManager = ApplicationContext.webSocketManager
  
  val cometEnabled = true
  val websocketEnabled = true

}



