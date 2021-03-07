package com.digitald4.nbastats.util;

import com.digitald4.nbastats.model.PlayerDay;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DistinictSalaryList extends ArrayList<PlayerDay> {
		private final Map<Integer, AtomicInteger> byCostCount = new HashMap<>();

		private final int limit;
		private final String league;
		public DistinictSalaryList(int limit, String league) {
			this.limit = limit;
			this.league = league;
		}

		@Override
		public boolean add(PlayerDay playerDay) {
			AtomicInteger byCost =
					byCostCount.computeIfAbsent(playerDay.getFantasySiteInfo(league).getCost(), cost -> new AtomicInteger());
			if (byCost.get() < limit) {
				byCost.incrementAndGet();
				return super.add(playerDay);
			}

			return false;
		}
	}