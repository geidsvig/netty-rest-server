package ca.figmint.rest

import scala.util.matching.Regex

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.HttpVersion
import org.jboss.netty.util.CharsetUtil

import akka.actor.ActorRef
import ca.figmint.netty.RestServerRouteHandlerRequirements
import ca.figmint.netty.RestServerRouteHandler
import ca.figmint.netty.RestUtils
import ca.figmint.rest.service.StatusRequest

abstract class RestRouteHandler extends RestServerRouteHandler with RestUtils {
	self: RestServerRouteHandlerRequirements =>
	
	val apiStatusPath: Regex
	val apiHandler: ActorRef

	def handleHttpRequest(ctx: ChannelHandlerContext, request: HttpRequest) {
		val method = request.getMethod
		val decoder = new org.jboss.netty.handler.codec.http.QueryStringDecoder(request.getUri)
		
		logger info (method + " " + request.getUri)
		
		(method, decoder.getPath) match {
			case (HttpMethod.GET, path) if pathMatches(path, apiStatusPath) => {
				apiHandler ! StatusRequest(ctx, request)
			}
			case (_, uri) => {
				val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND)
				response.setContent(ChannelBuffers.copiedBuffer("Unrecognized " + method + " " + uri, CharsetUtil.UTF_8))
				sendHttpResponse(ctx, request, response)
			}
		}
	}
	
}
