package com.troy.dramaserver.database;

import java.io.Serializable;

public class PointEntry implements Serializable {

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
		this.points = new RatePoints(points);
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
		return points.toString() + " " + category + " as " + role;
	}

	protected static String getPointsString(double points) {
		if (points == 1.0)
			return "point";
		else
			return "points";
	}
	
	
	
	
	private interface PointsStorage {

		public double getPoints();

		public String toString();
	}

	public class SinglePoints implements PointsStorage {
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
			return amount + " " + getPointsString(amount);
		}
	}

	public class RatePoints implements PointsStorage {
		private double rate;// When rate is 0 this indicates a one time point collection equal to amount
		private int amount;
		private String rateString;
		
		public RatePoints(int points) {
			this(0.0, points, null);
		}

		public RatePoints(double rate, int amount, String rateString) {
			this.rate = rate;
			this.amount = amount;
			this.rateString = rateString;
		}

		@Override
		public double getPoints() {
			if (rate == 0.0)
				return amount;
			else
				return rate * amount;
		}

		@Override
		public String toString() {
			if (rate == 0.0)
				return getPoints() + getPointsString(getPoints());
			else
				return getPoints() + getPointsString(getPoints()) + " " + amount + " " + rateString + "s at " + rate + " " + getPointsString(rate) + " per " + rateString;
		}
	}

}
