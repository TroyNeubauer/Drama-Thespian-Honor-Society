package com.troy.dramaserver.net;

import java.nio.charset.Charset;

import org.apache.logging.log4j.*;

import com.google.gson.JsonObject;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

public abstract class UrlHandler {

	private static final Logger logger = LogManager.getLogger(UrlHandler.class);

	private HttpMethod method;
	private String[] paramaters;

	public UrlHandler(HttpMethod method, String... paramaters) {
		if (method != HttpMethod.GET && method != HttpMethod.POST)
			throw new IllegalArgumentException("Unknown HTTP method:" + method);
		this.method = method;
		this.paramaters = paramaters;
	}

	public void handle(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
		for (String param : paramaters) {
			if (!data.has(param)) {
				Http.respond(ctx, request).content("Bad request, missing parameter: \"" + param + "\". Request body: \"" + request.content().toString(Charset.forName("ASCII")) + "\" full url: " + request.uri())
						.status(HttpResponseStatus.BAD_REQUEST).send();
				logger.info("Bad resuest specified: " + data.entrySet() + " needed: " + param + " full url: " + request.uri());
				return;
			}
		}
		handleImpl(ctx, request, data);
	}

	protected abstract void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception;

	public HttpMethod getMethod() {
		return method;
	}
}
