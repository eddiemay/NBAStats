package com.digitald4.nbastats.distributed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class Model {

	public static class Player implements Serializable {
		public final int playerId;
		public final int cost;
		public final int[] projection;

		public Player(String in) {
			String[] parts = in.split(",");
			this.playerId = Integer.parseInt(parts[0]);
			if (parts.length < 8) {
				System.out.println("Not enough data: " + in + " only " + parts.length + " parts");
				throw new RuntimeException("Not enough data: " + in + " only " + parts.length + " parts");
			}
			this.cost = Integer.parseInt(parts[1]);
			this.projection = new int[parts.length - 2];
			for (int p = 2; p < parts.length; p++) {
				projection[p - 2] = (int) Math.round(Double.parseDouble(parts[p]));
			}
		}
	}

	public static class PlayerGroup implements Serializable {
		public final int[] playerIds;
		public final int cost;
		public final int[] projection;

		public PlayerGroup(Player... players) {
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

		public int getCost() {
			return cost;
		}
	}

	public static class LineUp implements Serializable, Comparable<LineUp> {
		public final int projected;
		public final int totalSalary;
		public final int[] playerIds;

		public LineUp(int projected, int totalSalary, PlayerGroup... groups) {
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

		public int getProjected() {
			return projected;
		}

		public int compareTo(LineUp lineUp) {
			return Integer.compare(projected, lineUp.projected);
		}
	}

	public static class TopLineUps extends PriorityQueue<LineUp> implements Serializable {
		private final int limit;
		public TopLineUps(int limit) {
			super(limit);
			this.limit = limit;
		}

		@Override
		public boolean add(LineUp lineUp) {
			boolean ret = super.add(lineUp);
			if (size() > limit) {
				poll();
			}
			return ret;
		}

		public void addSorted(PriorityQueue<LineUp> queue) {
			if (size() >= limit) {
				while (queue.size() > 0 && queue.peek().projected < peek().projected) {
					queue.poll();
				}
			}
			this.addAll(queue);
			while (size() > limit) {
				poll();
			}
		}
	}
}