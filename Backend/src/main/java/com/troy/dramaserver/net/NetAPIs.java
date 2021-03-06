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
					PointEntry entry = PointEntry.fromJSON(getSessionAccount(server, request).getUserID(), server.getDatabase().nextPointID(), data.getAsJsonObject("value"));
					logger.info("made entry: " + entry);
					server.getDatabase().getPendingPoints().add(entry);
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

		addHandler("__get_account", new UrlHandler(HttpMethod.GET, "user") {

			@Override
			protected void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				Account myAccount = getSessionAccount(server, request);
				if (myAccount != null) {
					try {
						long requestedId = data.get("user").getAsLong();
						if (myAccount.isAdmin() || myAccount.getUserID() == requestedId) {
							Account account = server.getDatabase().getUserByID(requestedId);
							if (account != null) {
								JsonObject result = new JsonObject();
								result.addProperty("success", true);

								JsonObject acc = new JsonObject();
								acc.addProperty("name", account.getName());
								acc.addProperty("email", account.getEmail());
								acc.addProperty("id", account.getUserID());
								if (account.getPicture() == null)
									acc.add("photo", JsonNull.INSTANCE);
								else
									acc.addProperty("photo", Base64.getEncoder().encodeToString(account.getPicture()));

								result.add("value", acc);
								Http.respond(ctx, request).JSONContent(result).send();
								return;
							}
						}
					} catch (Exception e) {
					}
					Http.respond(ctx, request).JSONContent("success", false).send();
				} else
					Http.respond(ctx, request).redirect("/signin.html").send();
			}

		});

		addHandler("__get_my_picture", new UrlHandler(HttpMethod.GET) {

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

		addHandler("__approve", new UrlHandler(HttpMethod.POST, "pointId") {

			@Override
			protected void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				Account account = getSessionAccount(server, request);
				if (account != null && account.isAdmin()) {
					long id = data.get("pointId").getAsLong();
					Http.respond(ctx, request).JSONContent("success", server.getDatabase().transferPoint(id, server.getDatabase().getPendingPoints(), server.getDatabase().getApprovedPoints())).send();
				}
				Http.respond(ctx, request).JSONContent("success", false).send();
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

		addHandler("__get_my_pending", new UrlHandler(HttpMethod.GET) {

			@Override
			protected void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				Account account = getSessionAccount(server, request);
				if (account != null) {
					JsonObject result = new JsonObject();

					JsonArray values = new JsonArray();
					for (PointEntry points : server.getDatabase().getPendingPoints()) {
						if (points.getUserID() == account.getUserID())
							addPoint(values, points);
					}
					result.add("value", values);
					result.addProperty("success", true);

					Http.respond(ctx, request).JSONContent(result).send();
				} else
					Http.respond(ctx, request).JSONContent("success", false).send();

			}
		});

		addHandler("__get_all_pending", new UrlHandler(HttpMethod.GET) {

			@Override
			protected void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				Account account = getSessionAccount(server, request);
				if (account != null && account.isAdmin()) {
					JsonObject result = new JsonObject();

					JsonArray values = new JsonArray();
					for (PointEntry points : server.getDatabase().getPendingPoints()) {
						addPoint(values, points);
					}
					result.add("value", values);
					result.addProperty("success", true);

					Http.respond(ctx, request).JSONContent(result).send();
				} else
					Http.respond(ctx, request).JSONContent("success", false).send();

			}
		});

		addHandler("__get_my_approved", new UrlHandler(HttpMethod.GET) {

			@Override
			protected void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				Account account = getSessionAccount(server, request);
				if (account != null) {
					JsonObject result = new JsonObject();

					JsonArray values = new JsonArray();
					for (PointEntry points : server.getDatabase().getApprovedPoints()) {
						if (points.getUserID() == account.getUserID())
							addPoint(values, points);
					}
					result.add("value", values);
					result.addProperty("success", true);

					Http.respond(ctx, request).JSONContent(result).send();
				} else
					Http.respond(ctx, request).JSONContent("success", false).send();

			}
		});

		addHandler("__get_all_approved", new UrlHandler(HttpMethod.GET) {

			@Override
			protected void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				Account account = getSessionAccount(server, request);
				if (account != null && account.isAdmin()) {
					JsonObject result = new JsonObject();

					JsonArray values = new JsonArray();
					for (PointEntry points : server.getDatabase().getApprovedPoints()) {
						addPoint(values, points);
					}
					result.add("value", values);
					result.addProperty("success", true);

					Http.respond(ctx, request).JSONContent(result).send();
				} else
					Http.respond(ctx, request).JSONContent("success", false).send();

			}
		});

		addHandler("__is_admin", new UrlHandler(HttpMethod.GET) {

			@Override
			protected void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				Account account = getSessionAccount(server, request);
				if (account != null) {
					JsonObject result = new JsonObject();

					result.addProperty("value", account.isAdmin());
					result.addProperty("success", true);

					Http.respond(ctx, request).JSONContent(result).send();
				} else
					Http.respond(ctx, request).JSONContent("success", false).send();

			}
		});

		addHandler("signout.html", new UrlHandler(HttpMethod.GET) {

			@Override
			protected void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				SessionData session = server.getDatabase().getSession(getSession(server, request));
				if (session != null) {
					logger.info("logging out user: " + server.getSession(session.getData()));
					session.setValidUntil(DateTime.now());
					Http.respond(ctx, request).redirect("/index.html").send();
				}
			}
		});

		addAccountInfoHandler(server, "name", "name");
		addAccountInfoHandler(server, "email", "email");
		addAccountInfoHandler(server, "indunction_date", "indunctionDate");
		addAccountInfoHandler(server, "grad_year", "gradYear");
		addAccountInfoHandler(server, "student_id", "studentID");
		addAccountInfoHandler(server, "phone_number", "phoneNumber");
		addAccountInfoHandler(server, "cell_phone_number", "cellPhoneNumber");
		addAccountInfoHandler(server, "address", "address");
		addAccountInfoHandler(server, "its_id", "itsID");

		addHandler("__check_email", new UrlHandler(HttpMethod.GET, "email") {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				Http.respond(ctx, request).JSONContent("has_user", server.containsUser(data.getAsJsonPrimitive("email").getAsString())).send();
			}
		});
	}

	private static void addPoint(JsonArray values, PointEntry points) {
		JsonObject obj = new JsonObject();
		obj.addProperty("category", points.getCategory());
		obj.addProperty("role", points.getRole());
		obj.addProperty("info", points.getInfo());
		obj.addProperty("amount", points.getPoints().getPoints());
		obj.addProperty("userId", points.getUserID());
		obj.addProperty("pointId", points.getPointID());
		obj.addProperty("extendedInfo", points.getExtendedInfo());
		values.add(obj);
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
		final Class<?> type = field.getType();
		try {
			field.setAccessible(true);
		} catch (SecurityException e) {
			logger.warn("Unable to make field \"" + variableName + "\" accesible");
			logger.catching(e);
			return;
		}
		addHandler("__set_" + url, new UrlHandler(HttpMethod.POST, "value") {
			@Override
			public void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				Account account = getSessionAccount(server, request);
				if (account != null) {
					String value = data.getAsJsonPrimitive("value").getAsString();

					try {
						if (type == int.class || type == Integer.class) {
							field.setInt(account, Integer.parseInt(value));
						} else if (type == long.class || type == Long.class) {
							field.setLong(account, Long.parseLong(value));
						} else if (type == String.class) {
							field.set(account, value);
						} else if (type == LocalDate.class) {
							field.set(account, ISODateTimeFormat.date().parseLocalDate(value));
						}

						Http.respond(ctx, request).JSONContent("success", true).send();
					} catch (NumberFormatException e) {
						field.setByte(account, (byte) 0);
						logger.info("Number format exception! Set" + field + " to 0 for user: " + account);
						Http.respond(ctx, request).JSONContent("success", true).send();
					} catch (Exception e) {
						logger.info("Failed to set field " + field + " for user: " + account);
						logger.catching(e);
						Http.respond(ctx, request).JSONContent("success", false).send();
						return;
					}

				} else
					Http.respond(ctx, request).JSONContent("success", false).send();
			}
		});
		addHandler("__get_" + url, new UrlHandler(HttpMethod.GET) {

			@Override
			protected void handleImpl(ChannelHandlerContext ctx, FullHttpRequest request, JsonObject data) throws Exception {
				Account account = getSessionAccount(server, request);
				if (account != null) {
					try {
						JsonObject result = new JsonObject();
						Object obj = field.get(account);
						if (obj == null) {
							result.add("value", JsonNull.INSTANCE);
						} else if (type == int.class || type == Integer.class || type == long.class || type == Long.class || type == byte.class || type == Byte.class || type == float.class
								|| type == Float.class || type == double.class || type == Double.class) {
							result.addProperty("value", ((Number) obj));
						} else if (type == LocalDate.class) {
							result.addProperty("value", ISODateTimeFormat.date().print((LocalDate) obj));
						} else {
							result.addProperty("value", String.valueOf(obj));
						}

						result.addProperty("success", true);
						Http.respond(ctx, request).JSONContent(result).send();
					} catch (Exception e) {
						logger.info("Failed to get field " + field + " for user: " + account);
						logger.catching(e);
						Http.respond(ctx, request).JSONContent("success", false).send();
						return;
					}

				} else
					Http.respond(ctx, request).JSONContent("success", false).send();
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

	protected static byte[] getSession(Server server, FullHttpRequest request) {
		String header = request.headers().get(HttpHeaderNames.COOKIE);
		if (header != null) {
			Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(header);
			for (Cookie cookie : cookies) {
				if (cookie.name().equals(SESSION_COOKIE_NAME)) {
					try {
						return Base64.getDecoder().decode(cookie.value());
					} catch (Exception e) {
						logger.warn("Exception thrown when looking at session cookie: " + cookie.value());
						logger.catching(e);
					}
					break;
				}
			}
			logger.warn("Unable to find session for cookies " + cookies);
		} else {
			logger.warn("Missing header. Cannot determine account ");
		}
		return null;
	}

	protected static Account getSessionAccount(Server server, FullHttpRequest request) {
		return server.getSession(getSession(server, request));
	}

	private static boolean hasSessionCookie(Server server, FullHttpRequest request) {
		return getSessionAccount(server, request) != null;
	}

}
