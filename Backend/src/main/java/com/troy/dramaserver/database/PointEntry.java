package com.troy.dramaserver.database;

import java.io.Serializable;

import org.apache.logging.log4j.*;

import com.google.gson.JsonObject;

public class PointEntry implements Serializable {

	private static final Logger logger = LogManager.getLogger(PointEntry.class);

	private long userID;
	private String category, role, info;
	private PointsStorage points;

	public PointEntry(long userID, String category, String role, String info, int amount, boolean oneAct) {
		this.userID = userID;
		this.category = category;
		this.role = role;
		this.info = info;
		this.points = new SinglePoints(amount, oneAct);
	}

	public PointEntry(long userID, String category, String role, String info, int points) {
		this.userID = userID;
		this.category = category;
		this.role = role;
		this.info = info;
		this.points = new SimplePoint(points);
	}

	public PointEntry(long userID, String category, String role, String info, double rate, int amount, String rateString) {
		this.userID = userID;
		this.category = category;
		this.role = role;
		this.info = info;
		this.points = new RatePoints(rate, amount, rateString);
	}

	@Override
	public String toString() {
		return points.toString() + " | " + category + " as " + role + ", info: " + info;
	}

	protected static String getPointsString(double points) {
		if (points == 1.0)
			return "point";
		else
			return "points";
	}

	public static PointEntry fromJSON(long userID, JsonObject object) {
		try {
			if (!object.has("category") || !object.has("role") || !object.has("info") || !object.has("amount"))
				return null;
			PointEntry result;
			if (object.has("oneAct")) {// Normal point
				logger.info("Normal points object");
				result = new PointEntry(userID, object.get("category").getAsString(), object.get("role").getAsString(), object.get("info").getAsString(),
						object.getAsJsonPrimitive("amount").getAsInt(), object.getAsJsonPrimitive("oneAct").getAsBoolean());
			} else {
				if (object.has("rate")) {
					logger.info("Rate points object");
					if (!object.has("rateString"))
						return null;
					result = new PointEntry(userID, object.get("category").getAsString(), object.get("role").getAsString(), object.get("info").getAsString(),
							object.getAsJsonPrimitive("rate").getAsDouble(), object.getAsJsonPrimitive("amount").getAsInt(), object.getAsJsonPrimitive("rateString").getAsString());
				} else {// Simple object
					logger.info("Simple points object");
					result = new PointEntry(userID, object.get("category").getAsString(), object.get("role").getAsString(), object.get("info").getAsString(),
							object.getAsJsonPrimitive("amount").getAsInt());
				}
			}
			logger.info("Returning object: " + result);
			return result;
		} catch (Exception e) {
			logger.warn("Catching bad JSON!");
			logger.catching(e);
			return null;
		}
	}

	private static interface PointsStorage extends Serializable {

		public double getPoints();

		public String toString();
	}

	public static class SimplePoint implements PointsStorage {
		private int amount;

		public SimplePoint(int amount) {
			this.amount = amount;
		}

		@Override
		public double getPoints() {
			return amount;
		}

		@Override
		public String toString() {
			return amount + " " + getPointsString(amount);
		}
	}

	public static class SinglePoints implements PointsStorage {
		private int amount;
		boolean oneAct;

		public SinglePoints(int amount, boolean oneAct) {
			this.amount = amount;
			this.oneAct = oneAct;
		}

		@Override
		public double getPoints() {
			return amount;
		}

		public boolean isOneAct() {
			return oneAct;
		}

		@Override
		public String toString() {
			return amount + " " + getPointsString(amount) + '(' + (oneAct ? "one act" : "full length") + ')';
		}
	}

	public static class RatePoints implements PointsStorage {
		private double rate;
		private int amount;
		private String rateString;

		public RatePoints(double rate, int amount, String rateString) {
			this.rate = rate;
			this.amount = amount;
			this.rateString = rateString;
		}

		@Override
		public double getPoints() {
			return rate * amount;
		}

		@Override
		public String toString() {
			return getPoints() + " " + getPointsString(getPoints()) + " " + amount + " " + rateString + "s at " + rate + " " + getPointsString(rate) + " per " + rateString;
		}
	}

}
