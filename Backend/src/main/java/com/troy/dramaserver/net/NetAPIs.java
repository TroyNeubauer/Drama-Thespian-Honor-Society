package com.troy.dramaserver.net;

import static com.troy.dramaserver.net.HttpRequestHandler.addHandler;

import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Duration;

import com.troy.dramaserver.Server;
import com.troy.dramaserver.database.Account;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

public class NetAPIs {

	public static final Duration MAX_SESSION_COOKIE_AGE = Duration.standardMinutes(2);
	private static final String SESSION_COOKIE_NAME = "session";
	private static final int SESSION_COOKIE_LENGTH = 16;

	private static final Logger logger = LogManager.getLogger(NetAPIs.class);

	public static void init(final Server server) {
		addHandler("__create_account", new UrlHandler(HttpMethod.POST, "email", "name", "password") {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, HashMap<String, String> pairs)
					throws Exception {
				String email = pairs.get("email"), name = pairs.get("name");
				char[] password = pairs.get("password").toCharArray();
				pairs.put("password", null);// Hope GC cleans up the string fast
				boolean success = server.registerUser(email, password, name);

				HttpResponceBuilder builder = Http.respond(ctx, request).JSONContent("success", success);
				if (success)
					builder.cookie(createSessionCookie(server, server.getAccount(email)));
				builder.send();
			}
		});

		addHandler("__sign_in", new UrlHandler(HttpMethod.POST, "email", "password") {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, HashMap<String, String> pairs)
					throws Exception {
				String email = pairs.get("email");
				char[] password = pairs.get("password").toCharArray();
				pairs.put("password", null);// Hope GC cleans up the string fast
				boolean success = server.areCredentialsValid(email, password);
				HttpResponceBuilder builder = Http.respond(ctx, request);
				if (success) {
					builder.cookie(createSessionCookie(server, server.getAccount(email)));
				}
				builder.JSONContent("success", success).send();
			}
		});

		addHandler("__add", new UrlHandler(HttpMethod.POST, "value") {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, HashMap<String, String> pairs)
					throws Exception {
				if (hasSessionCookie(server, request)) {
					Http.respond(ctx, request).JSONContent("success", true).send();

				} else
					Http.respond(ctx, request).redirect("/signin.html").send();
			}
		});

		requireLogin(server, "add.html");
		requireLogin(server, "dashboard.html");

		/*private LocalDate indunctionDate;
		private boolean admin;
		private int gradYear, studentID, phoneNumber, cellPhoneNumber;
		private String address;*/

		addHandler("__set_picture_path", new UrlHandler(HttpMethod.POST, "picturePath") {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, HashMap<String, String> pairs)
					throws Exception {
				if (hasSessionCookie(server, request)) {
					Account account = getSessionAccount(server, request);
					if (account == null) {
						Http.respond(ctx, request).JSONContent("success", false).send();
					} else {
						String path = pairs.get("email");
						// TODO verify path
						account.setPicturePath(path);
						logger.info("Updated picture path for user: " + account);
						Http.respond(ctx, request).JSONContent("success", true).send();
					}

				} else
					Http.respond(ctx, request).redirect("/signin.html").send();
			}
		});

		addHandler("__check_email", new UrlHandler(HttpMethod.GET, "email") {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, HashMap<String, String> pairs)
					throws Exception {
				Http.respond(ctx, request).JSONContent("has_user", server.containsUser(pairs.get("email"))).send();
			}
		});
	}

	private static void requireLogin(final Server server, String url) {
		addHandler(url, new UrlHandler(HttpMethod.GET) {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, HashMap<String, String> pairs)
					throws Exception {
				if (hasSessionCookie(server, request))
					Http.respond(ctx, request).content(Server.PUBLIC_DIR, url).send();
				else
					Http.respond(ctx, request).redirect("/signin.html").send();
			}
		});
	}

	private static Cookie createSessionCookie(Server server, Account account) {
		byte[] value = new byte[SESSION_COOKIE_LENGTH];
		server.getRandom().nextBytes(value);
		server.addSession(value, account.getUserID());
		Cookie cookie = new DefaultCookie(SESSION_COOKIE_NAME, Base64.getEncoder().encodeToString(value));
		cookie.setSecure(true);
		cookie.setHttpOnly(true);
		cookie.setMaxAge(MAX_SESSION_COOKIE_AGE.getStandardSeconds());
		return cookie;
	}

	protected static Account getSessionAccount(Server server, FullHttpRequest request) {
		String header = request.headers().get(HttpHeaderNames.COOKIE);
		if (header != null) {
			Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(header);
			for (Cookie cookie : cookies) {
				if (cookie.name().equals(SESSION_COOKIE_NAME)) {
					try {
						return server.getSession(Base64.getDecoder().decode(cookie.value()));
					} catch (Exception e) {
						logger.warn("Exception thrown when looking at session cookie: " + cookie.value());
						logger.catching(e);
					}
					break;
				}
			}
		}
		return null;
	}

	private static boolean hasSessionCookie(Server server, HttpRequest request) {
		String header = request.headers().get(HttpHeaderNames.COOKIE);
		if (header != null) {
			Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(header);
			for (Cookie cookie : cookies) {
				if (cookie.name().equals(SESSION_COOKIE_NAME)) {
					try {
						return server.hasSession(Base64.getDecoder().decode(cookie.value()));
					} catch (Exception e) {
						logger.warn("Exception thrown when looking at session cookie: " + cookie.value());
						logger.catching(e);
					}
					break;
				}
			}
		}
		return false;
	}

}
