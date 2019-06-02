package com.troy.dramaserver.net;

import java.util.HashMap;

import org.apache.logging.log4j.*;

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

	public void handle(ChannelHandlerContext ctx, FullHttpRequest request, HashMap<String, String> pairs) throws Exception {
		for (String param : paramaters) {
			if (!pairs.containsKey(param)) {
				Http.respond(ctx, request).content("Bad request, missing parameter: \"" + param + "\"").status(HttpResponseStatus.BAD_REQUEST).send();
				return;
			}
		}
		handleImpl(ctx, request, pairs);
	}

	public abstract void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, HashMap<String, String> pairs) throws Exception;

	public HttpMethod getMethod() {
		return method;
	}
}
