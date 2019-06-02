package com.troy.dramaserver.net;

import static com.troy.dramaserver.net.HttpRequestHandler.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.*;

import com.troy.dramaserver.Server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

public class NetAPIs {

	private static final long MAX_SESSION_COOKIE_AGE = TimeUnit.DAYS.toSeconds(2);
	private static final String SESSION_COOKIE_NAME = "session";

	private static final Logger logger = LogManager.getLogger(NetAPIs.class);

	public static void init(final Server server) {
		addHandler("__create_account", new UrlHandler(HttpMethod.POST, "email", "name", "password") {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, HashMap<String, String> pairs) throws Exception {
				String email = pairs.get("email"), name = pairs.get("name");
				char[] password = pairs.get("password").toCharArray();
				pairs.put("password", null);// Hope GC cleans up the string fast

				Http.respond(ctx, request).JSONContent("success", server.registerUser(email, password, name)).redirect("/signin.html").send();
			}
		});

		addHandler("__sign_in", new UrlHandler(HttpMethod.POST, "email", "password") {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, HashMap<String, String> pairs) throws Exception {
				String email = pairs.get("email");
				char[] password = pairs.get("password").toCharArray();
				pairs.put("password", null);// Hope GC cleans up the string fast
				if (server.areCredentialsValid(email, password)) {
					Http.respond(ctx, request).JSONContent("success", true).redirect("/dashboard.html").send();
				} else {
					Http.respond(ctx, request).JSONContent("success", "Bad login").send();
				}
			}
		});

		requireLogin(server, "add.html");
		requireLogin(server, "dashboard.html");

		addHandler("__check_user", new UrlHandler(HttpMethod.GET, "email") {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, HashMap<String, String> pairs) throws Exception {
				Http.respond(ctx, request).JSONContent("has_user", server.containsUser(pairs.get("email"))).send();
			}
		});
	}

	private static void requireLogin(final Server server, String url) {
		addHandler(url, new UrlHandler(HttpMethod.GET) {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, HashMap<String, String> pairs) throws Exception {
				String header = request.headers().get(HttpHeaderNames.COOKIE);
				if (header != null) {
					Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(header);
					for (Cookie cookie : cookies) {
						if (cookie.name().equals(SESSION_COOKIE_NAME) && cookie.maxAge() < MAX_SESSION_COOKIE_AGE) {
							try {
								byte[] session = Base64.getDecoder().decode(cookie.value());
								if (server.getDatabase().getSessions().containsKey(session)) {
									Http.respond(ctx, request).content(Server.PUBLIC_DIR, url).send();
								}
							} catch (Exception e) {
								logger.warn("Exception thrown when looking at session cookie:");
								logger.catching(e);
							}

						}
					}
				}
				Http.respond(ctx, request).redirect("/signin.html").send();
			}
		});
	}

}
