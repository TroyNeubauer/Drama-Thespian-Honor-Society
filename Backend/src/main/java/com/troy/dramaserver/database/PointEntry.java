package com.troy.dramaserver.database;

import java.io.Serializable;

import org.apache.logging.log4j.*;

import com.google.gson.JsonObject;

public class PointEntry implements Serializable {
	
	private static final long serialVersionUID = 0;

	private static final Logger logger = LogManager.getLogger(PointEntry.class);

	private long userID, pointID;
	private String category, role, info;
	private PointsStorage points;

	public PointEntry(long userID, long pointID, String category, String role, String info, double amount, boolean oneAct) {
		this.userID = userID;
		this.pointID = pointID;
		this.category = category;
		this.role = role;
		this.info = info;
		this.points = new SinglePoints(amount, oneAct);
	}

	public PointEntry(long userID, long pointID, String category, String role, String info, double points) {
		this.userID = userID;
		this.pointID = pointID;
		this.category = category;
		this.role = role;
		this.info = info;
		this.points = new SimplePoint(points);
	}

	public PointEntry(long userID, long pointID, String category, String role, String info, double rate, double amount, String rateString) {
		this.userID = userID;
		this.pointID = pointID;
		this.category = category;
		this.role = role;
		this.info = info;
		this.points = new RatePoints(rate, amount, rateString);
	}

	public String getCategory() {
		return category;
	}

	public String getInfo() {
		return info;
	}

	public PointsStorage getPoints() {
		return points;
	}

	public String getRole() {
		return role;
	}

	public long getUserID() {
		return userID;
	}

	public long getPointID() {
		return pointID;
	}

	@Override
	public String toString() {
		return points.toString() + " | " + category + " as " + role + ", info: " + info + ", id=" + pointID;
	}

	protected static String getPointsString(double points) {
		if (points == 1.0)
			return "point";
		else
			return "points";
	}

	public static PointEntry fromJSON(long userID, long nextPointID, JsonObject object) {
		try {
			if (!object.has("category") || !object.has("role") || !object.has("info") || !object.has("amount"))
				return null;
			PointEntry result;
			if (object.has("oneAct")) {// Normal point
				logger.info("Normal points object");
				result = new PointEntry(userID, nextPointID, object.get("category").getAsString(), object.get("role").getAsString(), object.get("info").getAsString(),
						object.getAsJsonPrimitive("amount").getAsInt(), object.getAsJsonPrimitive("oneAct").getAsBoolean());
			} else {
				if (object.has("rate")) {
					logger.info("Rate points object");
					if (!object.has("rateString"))
						return null;
					result = new PointEntry(userID, nextPointID, object.get("category").getAsString(), object.get("role").getAsString(), object.get("info").getAsString(),
							object.getAsJsonPrimitive("rate").getAsDouble(), object.getAsJsonPrimitive("amount").getAsInt(), object.getAsJsonPrimitive("rateString").getAsString());
				} else {// Simple object
					logger.info("Simple points object");
					result = new PointEntry(userID, nextPointID, object.get("category").getAsString(), object.get("role").getAsString(), object.get("info").getAsString(),
							object.getAsJsonPrimitive("amount").getAsDouble());
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

	public String getExtendedInfo() {
		return points.getExtendedInfo();
	}

	public static interface PointsStorage extends Serializable {

		public double getPoints();

		public String getExtendedInfo();

		public String toString();
	}

	public static class SimplePoint implements PointsStorage {
		private double amount;

		public SimplePoint(double amount) {
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

		@Override
		public String getExtendedInfo() {
			return "";
		}
	}

	public static class SinglePoints implements PointsStorage {
		private double amount;
		boolean oneAct;

		public SinglePoints(double amount, boolean oneAct) {
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

		@Override
		public String getExtendedInfo() {
			return oneAct ? "one act" : "full length";
		}
	}

	public static class RatePoints implements PointsStorage {
		private double rate;
		private double amount;
		private String rateString;

		public RatePoints(double rate, double amount, String rateString) {
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

		@Override
		public String getExtendedInfo() {
			return amount + " " + rateString + "s at " + rate + " " + getPointsString(rate) + " per " + rateString;
		}
	}

}
