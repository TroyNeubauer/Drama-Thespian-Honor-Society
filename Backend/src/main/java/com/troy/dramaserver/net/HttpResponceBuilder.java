package com.troy.dramaserver.net;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

import java.io.*;
import java.util.*;

import org.apache.commons.io.FilenameUtils;

import com.google.gson.*;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.AsciiString;

public class HttpResponceBuilder {

	private static final AsciiString SERVER_NAME = new AsciiString("Netty");

	public static final String JSON = "application/json";
	public static final String HTML = "text/html";
	public static final String CSS = "text/css";
	public static final String PLAIN_TEXT = "text/plain";

	private ChannelHandlerContext ctx;
	private FullHttpRequest request;

	private DefaultFullHttpResponse response;
	private HttpHeaders headers;
	private ByteBuf contentBuf;

	private List<Cookie> cookies = new ArrayList<Cookie>();

	public HttpResponceBuilder(ChannelHandlerContext ctx, FullHttpRequest request) {
		this.ctx = ctx;
		this.request = request;
		this.response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		this.contentBuf = response.content();
		this.headers = response.headers();

		headers.set(CONTENT_LENGTH, "0");
		headers.set(SERVER, SERVER_NAME);
	}

	public HttpResponceBuilder content(File root, String url) {
		File file = new File(root, url);
		try {
			if (!file.exists()) {
				throw new FileNotFoundException();
			} else {
				FileInputStream stream = new FileInputStream(file);
				contentBuf.writeBytes(stream, (int) file.length());
				headers.set(CONTENT_LENGTH, String.valueOf(file.length()));
				headers.set(CONTENT_TYPE, getContentType(FilenameUtils.getExtension(url)));
				stream.close();
			}
		} catch (Exception e) {
			content("404 File not found: " + url);
			status(HttpResponseStatus.NOT_FOUND);
		}
		return this;
	}

	public HttpResponceBuilder content(String content) {
		setContentImpl(content);
		headers.set(CONTENT_TYPE, PLAIN_TEXT);
		return this;
	}

	public HttpResponceBuilder JSONContent(String key, String value) {
		return JSONContent("{\"" + key + "\":\"" + value + "\"}");// Put info format: "key":"value"
	}

	public HttpResponceBuilder JSONContent(JsonObject object) {
		JSONContent(new Gson().toJson(object));
		return this;
	}

	public HttpResponceBuilder JSONContent(String key, boolean value) {
		JSONObjectImpl(key, Boolean.toString(value));
		return this;
	}

	public HttpResponceBuilder JSONContent(String key, int value) {
		JSONObjectImpl(key, Integer.toString(value));
		return this;
	}

	public HttpResponceBuilder JSONContent(String key, long value) {
		JSONObjectImpl(key, Long.toString(value));
		return this;
	}

	public HttpResponceBuilder JSONContent(String key, double value) {
		JSONObjectImpl(key, Double.toString(value));
		return this;
	}

	private void JSONObjectImpl(String key, Object value) {
		JSONContent("{\"" + key + "\":" + value + '}');// Put info format: "key":value
	}

	private HttpResponceBuilder JSONContent(String content) {
		setContentImpl(content);
		headers.set(CONTENT_TYPE, JSON);
		return this;
	}

	public HttpResponceBuilder HTMLContent(String content) {
		setContentImpl(content);
		headers.set(CONTENT_TYPE, HTML);
		return this;
	}

	public HttpResponceBuilder redirect(String url) {
		return addRedirect(url, HttpResponseStatus.FOUND);
	}

	public HttpResponceBuilder addRedirect(String url, HttpResponseStatus status) {
		headers.set(LOCATION, url);
		response.setStatus(status);
		return this;
	}

	public HttpResponceBuilder status(HttpResponseStatus status) {
		response.setStatus(status);
		return this;
	}

	public void setContentImpl(String content) {
		byte[] bytes = content.getBytes();
		contentBuf.writeBytes(bytes);
		headers.set(CONTENT_LENGTH, String.valueOf(contentBuf.writerIndex()));
	}

	void send() {
		if (cookies.size() > 0)
			headers.set(SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookies));
		if (HttpUtil.isKeepAlive(request)) {
			ctx.write(response, ctx.voidPromise());
		} else {
			ctx.write(response).addListener(ChannelFutureListener.CLOSE);
		}
		ctx.flush();
	}

	private static String getContentType(String extension) {
		switch (extension) {
		case "json":
			return JSON;
		case "zip":
			return "application/zip";
		case "js":
			return "application/javascript";
		case "png":
			return "image/png";
		case "gif":
			return "image/gif";
		case "jpg":
			return "image/jpeg";
		case "jpeg":
			return "image/jpeg";
		case "css":
			return CSS;
		case "html":
			return HTML;
		default:
			return PLAIN_TEXT;
		}
	}

	public HttpResponceBuilder cookie(Cookie cookie) {
		cookies.add(cookie);
		return this;
	}
}
