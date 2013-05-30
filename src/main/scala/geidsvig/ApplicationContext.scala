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
import geidsvig.netty.rest.RestHttpRequest
import com.twitter.finagle.memcached.protocol.NotFound
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import geidsvig.netty.rest.RestPathHandler
import geidsvig.netty.socket.ws.WebSocketHandshaker
import geidsvig.netty.socket.comet.CometManager
import geidsvig.netty.socket.ws.WebSocketManager
import geidsvig.netty.socket.comet.CometHandlerFactory
import geidsvig.netty.socket.comet.CometManagerRequirements
import akka.event.LoggingAdapter
import geidsvig.netty.socket.ws.WebSocketSessionHandlerFactory
import geidsvig.netty.socket.ws.WebSocketManagerRequirements
import geidsvig.netty.socket.comet.CometHandler
import geidsvig.netty.socket.comet.CometHandlerRequirements
import geidsvig.netty.socket.comet.CometResponse
import geidsvig.netty.socket.comet.CometPacket
import org.jboss.netty.handler.codec.http.HttpRequest
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

object ApplicationContext {
  val system = ActorSystem("restServerSystem", ConfigFactory.load.getConfig("restServerSystem"))

  val domain = system.settings.config.getString("restServerSystem.domain")
  val hostname = system.settings.config.getString("restServerSystem.hostname")
  val port = system.settings.config.getInt("restServerSystem.http.port")

  val logger = system.log

  logger info ("Application started.")

  val statusHandler: ActorRef = system.actorOf(Props[ApplicationStatusHandler], "statusHandler")

  val voidActor: ActorRef = system.actorOf(Props[VoidActor], "voidActor")
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

class ApplicationWebSocketHandshaker(domain: String) extends WebSocketHandshaker {
  val secureDomain = ("""^https?://(\w+\.)?""" + domain.replaceAll(".", "\\.")).r
}

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
}

/** simple comet handler
 *  should respond with a HttpResponse
 */
class SimpleCometHandler extends CometHandler with SimpleCometHandlerDependencies {
  import scala.concurrent.ExecutionContext.Implicits.global
  def handleRequest(request: HttpRequest) {
    sendResponse(CometPacket(HttpResponseStatus.OK, "mock response"))
  }
}

class MockCometHandlerFactory extends CometHandlerFactory {
  def createCometHandler(): ActorRef = ApplicationContext.system.actorOf(Props[SimpleCometHandler])
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

class MockWebSocketSessionHandlerFactory extends WebSocketSessionHandlerFactory {
  // FIXME create a valid websocket session handler
  // should heartbeat every 5 seconds with a text frame
  def createWebSocketSessionHandler(): ActorRef = ApplicationContext.voidActor
}

trait TestWebSocketManagerDependencies extends WebSocketManagerRequirements {
  val webSocketSessionHandlerFactory: WebSocketSessionHandlerFactory = new MockWebSocketSessionHandlerFactory
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

  val cometManager: CometManager = new MockCometManager
  val webSocketManager: WebSocketManager = new MockWebSocketManager

}



