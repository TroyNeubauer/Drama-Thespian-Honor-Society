package com.troy.dramaserver.database;

import java.io.Serializable;

public class PointEntry implements Serializable {

	private long userID;
	private String category, role;
	private PointsStorage points;

	private interface PointsStorage {

		public double getPoints();
		public String toString();
	}

	protected static String getPointsString(double points) {
		if (points == 1.0)
			return "point";
		else
			return "points";
	}

	public class SinglePoints implements PointsStorage {
		private int amount;

		public SinglePoints(int amount) {
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

	public class RatePoints implements PointsStorage {
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
			return rate * (double) amount;
		}

		@Override
		public String toString() {
			return getPoints() + getPointsString(getPoints()) + " " + amount + " " + rateString + "s at " + rate + " " + getPointsString(rate) + " per " + rateString;
		}
	}

}
