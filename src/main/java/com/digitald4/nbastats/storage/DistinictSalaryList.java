package com.digitald4.nbastats.storage;

import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp.LineUpPlayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DistinictSalaryList extends ArrayList<LineUpPlayer> {
		private Map<Integer, AtomicInteger> byCostCount = new HashMap<>();

		private final int limit;
		public DistinictSalaryList(int limit) {
			this.limit = limit;
		}

		@Override
		public boolean add(LineUpPlayer player) {
			AtomicInteger byCost = byCostCount.computeIfAbsent(player.getCost(), cost -> new AtomicInteger());
			if (byCost.get() < limit) {
				byCost.incrementAndGet();
				return super.add(player);
			}
			return false;
		}
	}