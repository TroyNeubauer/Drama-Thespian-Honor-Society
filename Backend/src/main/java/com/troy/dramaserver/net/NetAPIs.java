package com.troy.dramaserver.net;

import static com.troy.dramaserver.net.HttpRequestHandler.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.*;
import org.joda.time.*;
import org.joda.time.format.ISODateTimeFormat;

import com.google.gson.*;
import com.troy.dramaserver.Server;
import com.troy.dramaserver.database.*;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

public class NetAPIs {

	public static final Duration MAX_SESSION_COOKIE_AGE = Duration.standardDays(10);
	private static final String SESSION_COOKIE_NAME = "session";
	private static final int SESSION_COOKIE_LENGTH = 16;

	private static final Logger logger = LogManager.getLogger(NetAPIs.class);

	public static void init(final Server server) {
		addHandler("__create_account", new UrlHandler(HttpMethod.POST, "email", "name", "password") {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				String email = data.getAsJsonPrimitive("email").getAsString(), name = data.getAsJsonPrimitive("name").getAsString();
				char[] password = data.getAsJsonPrimitive("password").getAsString().toCharArray();
				boolean success = server.registerUser(email, password, name);

				HttpResponceBuilder builder = Http.respond(ctx, request).JSONContent("success", success);
				if (success)
					builder.cookie(createSessionCookie(server, server.getAccount(email)));
				builder.send();
			}
		});

		addHandler("__sign_in", new UrlHandler(HttpMethod.POST, "email", "password") {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				String email = data.getAsJsonPrimitive("email").getAsString();
				char[] password = data.getAsJsonPrimitive("password").getAsString().toCharArray();
				boolean success = server.areCredentialsValid(email, password);
				HttpResponceBuilder builder = Http.respond(ctx, request);
				if (success) {
					builder.cookie(createSessionCookie(server, server.getAccount(email)));
				}
				builder.JSONContent("success", success).send();
			}
		});

		addHandler("__get_my_id", new UrlHandler(HttpMethod.GET) {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				if (hasSessionCookie(server, request)) {
					Http.respond(ctx, request).JSONContent("id", getSessionAccount(server, request).getUserID()).send();
				} else
					Http.respond(ctx, request).redirect("/signin.html").send();
			}
		});

		addHandler("__add", new UrlHandler(HttpMethod.POST, "value") {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				if (hasSessionCookie(server, request)) {
					logger.info("Worked " + data);
					PointEntry entry = PointEntry.fromJSON(getSessionAccount(server, request).getUserID(), data.getAsJsonObject("value"));
					logger.info("made entry: " + entry);
					server.getDatabase().getWaitingPoints().add(entry);
					if (entry != null) {
						Http.respond(ctx, request).JSONContent("success", true).send();
						return;
					}
				}
				Http.respond(ctx, request).redirect("/signin.html").send();
			}
		});

		requireLogin(server, "add.html");
		requireLogin(server, "dashboard.html");

		addHandler("__get_picture", new UrlHandler(HttpMethod.GET) {

			@Override
			protected void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				Account account = getSessionAccount(server, request);
				if (account != null) {
					JsonObject obj = new JsonObject();
					obj.addProperty("success", true);
					if (account.getPicture() == null)
						obj.addProperty("image", (String) null);
					else {
						String image = Base64.getEncoder().encodeToString(account.getPicture());
						obj.addProperty("image", image);
					}
					Http.respond(ctx, request).JSONContent(obj).send();

				} else {
					Http.respond(ctx, request).redirect("/signin.html").send();
				}
			}

		});

		addHandler("__set_picture", new UrlHandler(HttpMethod.POST, "image") {

			@Override
			protected void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				Account account = getSessionAccount(server, request);
				if (account != null) {
					try {
						String content = data.get("image").getAsString();
						if (!content.startsWith("data:image/")) {
							Http.respond(ctx, request).JSONContent("success", false).send();
						} else {
							byte[] image = Base64.getDecoder().decode(content.substring(content.indexOf(',') + 1));
							BufferedImage decodedImage = ImageIO.read(new ByteArrayInputStream(image));
							logger.info("was able to decode image!");
							if (decodedImage.getWidth() > 256 || decodedImage.getHeight() > 256) {
								Http.respond(ctx, request).JSONContent("success", false).send();
								logger.warn("attempt to too large image from: " + account);
							} else {
								ByteArrayOutputStream stream = new ByteArrayOutputStream();
								ImageIO.write(decodedImage, "jpeg", stream);
								account.setPicture(stream.toByteArray());
								logger.info("change picture of " + account + " to length " + stream.size());
								Http.respond(ctx, request).JSONContent("success", true).send();
							}
						}
					} catch (Exception e) {
						logger.catching(e);
						logger.warn("attempt to upload bad image from account: " + account);
					}
				} else
					Http.respond(ctx, request).redirect("/signin.html").send();

			}
		});
		
		addAccountInfoHandler(server, "__set_name", "name");
		addAccountInfoHandler(server, "__set_indunction_date", "indunctionDate");
		addAccountInfoHandler(server, "__set_grad_year", "gradYear");
		addAccountInfoHandler(server, "__set_student_id", "studentID");
		addAccountInfoHandler(server, "__set_phone_number", "phoneNumber");
		addAccountInfoHandler(server, "__set_cell_phone_number", "cellPhoneNumber");
		addAccountInfoHandler(server, "__set_address", "address");

		addHandler("__check_email", new UrlHandler(HttpMethod.GET, "email") {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				Http.respond(ctx, request).JSONContent("has_user", server.containsUser(data.getAsJsonPrimitive("email").getAsString())).send();
			}
		});
	}

	private static void addAccountInfoHandler(Server server, String url, String variableName) {
		final Field field;
		try {
			field = Account.class.getDeclaredField(variableName);
		} catch (NoSuchFieldException | SecurityException e) {
			logger.warn("Unable to find field \"" + variableName + "\" in class Account");
			logger.catching(e);
			return;
		}
		try {
			field.setAccessible(true);
		} catch (SecurityException e) {
			logger.warn("Unable to make field \"" + variableName + "\" accesible");
			logger.catching(e);
			return;
		}
		addHandler(url, new UrlHandler(HttpMethod.POST, "value") {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				if (hasSessionCookie(server, request)) {
					Account account = getSessionAccount(server, request);
					if (account == null) {
						Http.respond(ctx, request).JSONContent("success", false).send();
					} else {
						String value = data.getAsJsonPrimitive("value").getAsString();
						Class<?> type = field.getType();
						try {
							if (type == int.class || type == Integer.class) {
								field.setInt(account, Integer.parseInt(value));
							} else if (type == long.class || type == Long.class) {
								field.setLong(account, Long.parseLong(value));
							} else if (type == String.class) {
								field.set(account, value);
							} else if (type == LocalDate.class) {
								field.set(account, ISODateTimeFormat.basicDate().parseLocalDate(value));
							}

							logger.info("Updated field " + field + " for user: " + account);
							Http.respond(ctx, request).JSONContent("success", true).send();
						} catch (Exception e) {
							logger.info("Failed to set field " + field + " for user: " + account);
							logger.catching(e);
							Http.respond(ctx, request).JSONContent("success", false).send();
							return;
						}
					}

				} else
					Http.respond(ctx, request).redirect("/signin.html").send();
			}
		});
	}

	private static void requireLogin(final Server server, String url) {
		addHandler(url, new UrlHandler(HttpMethod.GET) {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
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
