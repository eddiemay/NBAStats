package com.digitald4.nbastats.distributed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Model {

	public static class Player implements Serializable {
		public final int playerId;
		public final int cost;
		public final int[] projection;

		public Player(String in) {
			String[] parts = in.split(",");
			this.playerId = Integer.parseInt(parts[0]);
			this.cost = Integer.parseInt(parts[1]);
			this.projection = new int[parts.length - 2];
			for (int p = 2; p < parts.length; p++) {
				projection[p - 2] = (int) Math.round(Double.parseDouble(parts[p]));
			}
		}
	}

	public static class PlayerGroup implements Serializable {
		public final int[] playerIds; // 2 bytes
		public final int cost; // 2 bytes
		public final int[] projection; // 2 * 6 = 12 bytes
		private int projectionMethod;

		PlayerGroup(Player... players) {
			this.playerIds = new int[players.length];
			int cost = 0;
			projection = new int[players[0].projection.length];
			for (int p = 0; p < players.length; p++) {
				Player player = players[p];
				playerIds[p] = player.playerId;
				cost += player.cost;
				for (int i = 0; i < projection.length; i++) {
					projection[i] += player.projection[i];
				}
			}
			this.cost = cost;
		}

		public int getProjectionMethod() {
			return projectionMethod;
		}

		public void setProjectionMethod(int projectionMethod) {
			this.projectionMethod = projectionMethod;
		}

		public int getProjection() {
			return projection[projectionMethod];
		}
	}

	public static class LineUp implements Serializable, Comparable<LineUp> {
		public final int projected;
		final int totalSalary;
		final int[] playerIds;

		LineUp(int projected, int totalSalary, PlayerGroup... groups) {
			this.projected = projected;
			this.totalSalary = totalSalary;
			List<Integer> ids = new ArrayList<>();
			for (PlayerGroup group : groups) {
				for (int id : group.playerIds) {
					ids.add(id);
				}
			}
			this.playerIds = new int[ids.size()];
			for (int p = 0; p < ids.size(); p++) {
				this.playerIds[p] = ids.get(p);
			}
		}

		@Override
		public int compareTo(LineUp lineUp) {
			return Integer.compare(projected, lineUp.projected);
		}

		public String toString(String method) {
			StringBuilder toString = new StringBuilder(projected + "," + method + "," + totalSalary);
			for (int id : playerIds) {
				toString.append(",").append(id);
			}
			return toString.toString();
		}
	}
}
