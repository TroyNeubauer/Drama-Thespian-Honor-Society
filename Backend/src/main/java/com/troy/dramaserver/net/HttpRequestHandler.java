package com.troy.dramaserver.net;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

import java.io.*;
import java.net.URLDecoder;
import java.util.HashMap;

import org.apache.logging.log4j.*;

import com.troy.dramaserver.Server;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private static final Logger logger = LogManager.getLogger(HttpRequestHandler.class);

	private static final String UTF_8 = "utf-8";

	private static final HashMap<String, UrlHandler> handlers = new HashMap<>();

	protected static void addHandler(String url, UrlHandler handler) {
		handlers.put(url, handler);
	}

	private static HashMap<String, String> getPairs(String url) {
		HashMap<String, String> pairs = new HashMap<>();
		String pairsString;
		if (url.contains("?")) {
			int index = url.indexOf('?');
			pairsString = url.substring(index + 1);
		} else {
			pairsString = url;
		}
		if (pairsString.contains("&")) {
			for (String pair : pairsString.split("&")) {
				addPair(pairs, pair);
			}
		} else {
			addPair(pairs, pairsString);
		}
		return pairs;
	}

	private static void addPair(HashMap<String, String> map, String pair) {
		if (pair.contains("=")) {
			int index = pair.indexOf("=");
			try {
				map.put(URLDecoder.decode(pair.substring(0, index), UTF_8), URLDecoder.decode(pair.substring(index + 1), UTF_8));
			} catch (UnsupportedEncodingException e) {
				return;// Don't add any malformed parts
			}
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		String url = request.uri();
		if (url.startsWith("/"))
			url = url.substring(1);
		if (url.isEmpty())
			url = "index.html";
		url.replaceAll("../", "");// No relative pathing up to the server=
		String fullUrl = url;
		if (url.contains("?"))
			url = url.substring(0, url.indexOf("?"));

		if (handlers.containsKey(url)) {
			UrlHandler handler = handlers.get(url);
			HashMap<String, String> pairs;
			if (handler.getMethod() == HttpMethod.GET) {
				pairs = getPairs(fullUrl);// Use the url for parameters
			} else {
				byte[] bytes = new byte[request.content().writerIndex()];// Use the content otherwise
				request.content().readBytes(bytes);
				pairs = getPairs(new String(bytes));
			}
			handler.handle(ctx, request, pairs);
		} else if (request.method() == HttpMethod.GET) {
			Http.respond(ctx, request).content(Server.PUBLIC_DIR, url).send();
		} else {
			Http.respond(ctx, request).content("Not implemented").status(NOT_IMPLEMENTED).send();
		}
	}
}
