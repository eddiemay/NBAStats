package com.digitald4.nbastats.storage;

import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.Fibonacci;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.Pair;
import com.digitald4.common.util.Provider;
import com.digitald4.common.util.TopSet;
import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp;
import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp.LineUpPlayer;
import com.digitald4.nbastats.proto.NBAStatsProtos.Position;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.joda.time.DateTime;

public class LineUpStore extends GenericStore<LineUp> {
	private static final int LIMIT = 100;
	public enum FantasyLeague {
		DRAFT_KINGS("draftkings", 50000),
		FAN_DUEL("fanduel", 60000);

		public String name;
		public int salaryCap;

		FantasyLeague(String name, int salaryCap) {
			this.name = name;
			this.salaryCap = salaryCap;
		}
	}

	private final PlayerDayStore playerDayStore;
	public LineUpStore(Provider<DAO> daoProvider, PlayerDayStore playerDayStore) {
		super(LineUp.class, daoProvider);
		this.playerDayStore = playerDayStore;
	}

	public Set<LineUp> processDraftKings(DateTime date) {
		String leauge = FantasyLeague.DRAFT_KINGS.name;
		return processDraftKings(playerDayStore.processStats(date)
				.stream()
				.filter(playerDay -> playerDay.getStatsMap().get("Projection") != null)
				.filter(playerDay -> playerDay.getStatsMap().get("Projection").getFantasySitePointsMap().get(leauge) != null)
				.map(playerDay -> LineUpPlayer.newBuilder()
						.setPlayerId(playerDay.getPlayerId())
						.setName(playerDay.getName())
						.addAllPosition(playerDay.getFantasySiteInfoOrThrow(leauge).getPositionList())
						.setCost(playerDay.getFantasySiteInfoOrThrow(leauge).getCost())
						.setProjectedPoints(playerDay.getStatsOrThrow("Projection").getFantasySitePointsOrThrow(leauge))
						.build())
				.collect(Collectors.toList()));
	}

	public Set<LineUp> processDraftKings(List<LineUpPlayer> lineUpPlayers) {
		System.out.println(lineUpPlayers.size());
		List<LineUpPlayer> pgs = new DistinictSalaryList(2);
		List<LineUpPlayer> sgs = new DistinictSalaryList(2);
		List<LineUpPlayer> sfs = new DistinictSalaryList(2);
		List<LineUpPlayer> pfs = new DistinictSalaryList(2);
		List<LineUpPlayer> cs = new DistinictSalaryList(2);
		List<LineUpPlayer> gs = new DistinictSalaryList(3);
		List<LineUpPlayer> fs = new DistinictSalaryList(3);
		List<LineUpPlayer> utils = new DistinictSalaryList(4);
		int[] fibonacci = Fibonacci.genSequence(lineUpPlayers.size() + 1);
		AtomicInteger index = new AtomicInteger();
		lineUpPlayers.stream().sorted(Comparator.comparing(LineUpPlayer::getProjectedPoints).reversed()).forEach(player -> {
			player = player.toBuilder().setHashId(fibonacci[index.incrementAndGet()]).build();
			if (player.getPositionList().contains(Position.PG) || player.getPositionList().contains(Position.SG)) {
				gs.add(player);
				if (player.getPositionList().contains(Position.PG)) pgs.add(player);
				if (player.getPositionList().contains(Position.SG)) sgs.add(player);
			}
			if (player.getPositionList().contains(Position.SF) || player.getPositionList().contains(Position.PF)) {
				fs.add(player);
				if (player.getPositionList().contains(Position.SF)) sfs.add(player);
				if (player.getPositionList().contains(Position.PF)) pfs.add(player);
			}
			if (player.getPositionList().contains(Position.C)) cs.add(player);
			utils.add(player);
		});
		System.out.println("Utils: " + utils.size());
		System.out.println("  Gs: " + gs.size());
		System.out.println("    PGs: " + pgs.size());
		System.out.println("    SGs: " + sgs.size());
		System.out.println("  Fs: " + fs.size());
		System.out.println("    SFs: " + sfs.size());
		System.out.println("    PFs: " + pfs.size());
		System.out.println("  Cs: " + cs.size());
		System.out.println("Iterations: " + FormatText.formatCurrency(((long) pgs.size()) * sgs.size()
				* sfs.size() * pfs.size() * cs.size() * gs.size() * fs.size() * utils.size()));
		System.out.println("Combinations: " + FormatText.formatCurrency(Calculate.combinations(utils.size(), 8)));

		TopSet<LineUp> bestLineUps = new TopSet<>(LIMIT,
				Comparator.comparing(LineUp::getProjectedPoints).reversed().thenComparing(LineUp::getHashId));
		List<Pair<LineUpPlayer, LineUpPlayer>> pairs = new ArrayList<>();
		for (LineUpPlayer pg : pgs) {
			for (LineUpPlayer sg : sgs) {
				if (pg != sg) {
					pairs.add(Pair.of(pg, sg));
				}
			}
		}

		// LineUpPlayer[] sfArray = sfs.toArray(new LineUpPlayer[sfs.size()]);
		LineUpPlayer[] pfArray = pfs.toArray(new LineUpPlayer[pfs.size()]);
		LineUpPlayer[] cArray = cs.toArray(new LineUpPlayer[cs.size()]);
		LineUpPlayer[] gArray = gs.toArray(new LineUpPlayer[gs.size()]);
		LineUpPlayer[] fArray = fs.toArray(new LineUpPlayer[fs.size()]);
		LineUpPlayer[] utilArray = utils.toArray(new LineUpPlayer[utils.size()]);
		System.out.println("Runners required: " + pairs.size());
		pairs.subList(0, 4).parallelStream().map(pair -> {
			Set<LineUp> lineUps = new TopSet<>(LIMIT,
					Comparator.comparing(LineUp::getProjectedPoints).reversed().thenComparing(LineUp::getHashId));
			LineUpPlayer pg = pair.getLeft();
			LineUpPlayer sg = pair.getRight();
			for (LineUpPlayer sf : sfs.subList(0, 2)) {
				if (sf == sg || sf == pg) continue;
				for (LineUpPlayer pf : pfArray) {
					if (pf == sf || pf == sg || pf == pg) continue;
					for (LineUpPlayer c : cArray) {
						if (c == pf || c == sf) continue;
						for (LineUpPlayer g : gArray) {
							if (g == pg || g == sg || g == sf || g == pf) continue;
							for (LineUpPlayer f : fArray) {
								if (f == sf || f == pf || f == g || f == sg || f == pg) continue;
								for (LineUpPlayer util : utilArray) {
									if (util == pg || util == sg || util == sf || util == pf || util == c || util == g || util == f)
										continue;
									int totalSalary = pg.getCost() + sg.getCost() + sf.getCost() + pf.getCost() + c.getCost()
											+ g.getCost() + f.getCost() + util.getCost();
									if (totalSalary < FantasyLeague.DRAFT_KINGS.salaryCap) {
										lineUps.add(LineUp.newBuilder()
												.setHashId(pg.getHashId() + sg.getHashId() + sf.getHashId() + pf.getHashId() + c.getHashId()
														+ util.getHashId() + g.getHashId() + f.getHashId())
												.setFantasySite(FantasyLeague.DRAFT_KINGS.name)
												.setProjectedPoints(pg.getProjectedPoints() + sg.getProjectedPoints()
														+ sf.getProjectedPoints() + pf.getProjectedPoints() + c.getProjectedPoints()
														+ g.getProjectedPoints() + f.getProjectedPoints() + util.getProjectedPoints())
												.setTotalSalary(totalSalary)
												.addPlayer(pg).addPlayer(sg).addPlayer(sf).addPlayer(pf).addPlayer(c)
												.addPlayer(g).addPlayer(f).addPlayer(util)
												.build());
									}
								}
							}
						}
					}
				}
			}
			return lineUps;
		}).forEach(lineUps -> {
			synchronized (this) {
				System.out.println(lineUps.size());
				bestLineUps.addSorted(lineUps);
			}
		});
		//System.out.println("Valid LineUps " + FormatText.formatCurrency(valid));
		return bestLineUps;
	}
	//19,976,848,461
	//Processed: $349,899,264.00
	// Total Time: 8 secs

	/*

Utils: 119
  Gs: 61
    PGs: 32
    SGs: 51
  Fs: 70
    SFs: 45
    PFs: 43
  Cs: 37

	Utils: 129
  Gs: 65
    PGs: 34
    SGs: 55
  Fs: 76
    SFs: 50
    PFs: 48
  Cs: 39

	153
	PGs: 37
	SGs: 55
	Gs: 56
	SFs: 53
	PFs: 50
	Fs: 64
	Cs: 35
	Utils: 53
	Iterations $35,852,727,680,000.00
	Processed: $0.00
	Valid Ups: $17,415,321,130.00
	Total Time: 32 secs*/

	public Set<LineUp> processFanDuel(List<LineUpPlayer> lineUpPlayers) {
		System.out.println(lineUpPlayers.size());
		List<LineUpPlayer> pgs = new DistinictSalaryList(3);
		List<LineUpPlayer> sgs = new DistinictSalaryList(3);
		List<LineUpPlayer> sfs = new DistinictSalaryList(3);
		List<LineUpPlayer> pfs = new DistinictSalaryList(3);
		List<LineUpPlayer> cs = new DistinictSalaryList(2);

		lineUpPlayers.stream().sorted(Comparator.comparing(LineUpPlayer::getProjectedPoints).reversed()).forEach(player -> {
			if (player.getPositionList().contains(Position.PG)) pgs.add(player);
			if (player.getPositionList().contains(Position.SG)) sgs.add(player);
			if (player.getPositionList().contains(Position.SF)) sfs.add(player);
			if (player.getPositionList().contains(Position.PF)) pfs.add(player);
			if (player.getPositionList().contains(Position.C)) cs.add(player);
		});
		System.out.println("PGs: " + pgs.size());
		System.out.println("SGs: " + sgs.size());
		System.out.println("SFs: " + sfs.size());
		System.out.println("PFs: " + pfs.size());
		System.out.println(" Cs: " + cs.size());
		System.out.println("Iterations: " + FormatText.formatCurrency(
				((long) pgs.size()) * sgs.size() * sfs.size() * pfs.size() * cs.size() * 2));
		System.out.println("Combinations: " + FormatText.formatCurrency(
				Calculate.combinations(pgs.size() + sgs.size() + sfs.size() + pfs.size() + cs.size(), 9)));

		LineUpPlayer[] pgArray = pgs.toArray(new LineUpPlayer[pgs.size()]);
		LineUpPlayer[] sgArray = sgs.toArray(new LineUpPlayer[sgs.size()]);
		LineUpPlayer[] sfArray = sfs.toArray(new LineUpPlayer[sfs.size()]);
		LineUpPlayer[] pfArray = pfs.toArray(new LineUpPlayer[pfs.size()]);
		Set<LineUp> bestLineUps = new TopSet<>(LIMIT,
				Comparator.comparing(LineUp::getProjectedPoints).thenComparing(LineUp::getId));
		cs.parallelStream().map(c -> {
			Set<LineUp> lineUps = new TopSet<>(LIMIT,
					Comparator.comparing(LineUp::getProjectedPoints).thenComparing(LineUp::getId));
			int id = cs.indexOf(c) * 1000;
			for (int pG1 = 0; pG1 < pgArray.length - 1; pG1++) {
				LineUpPlayer pg1 = pgArray[pG1];
				for (int pG2 = pG1 + 1; pG2 < pgArray.length ; pG2++) {
					LineUpPlayer pg2 = pgArray[pG2];
					for (int sG1 = 0; sG1 < sgArray.length - 1; sG1++) {
						LineUpPlayer sg1 = sgArray[sG1];
						if (sg1 == pg1 || sg1 == pg2) continue;
						for (int sG2 = sG1 + 1; sG2 < sgArray.length; sG2++) {
							LineUpPlayer sg2 = sgArray[sG2];
							if (sg2 == pg1 || sg2 == pg2) continue;
							for (int sF1 = 0; sF1 < sfArray.length - 1; sF1++) {
								LineUpPlayer sf1 = sfArray[sF1];
								if (sf1 == pg1 || sf1 == pg2 || sf1 == sg1 || sf1 == sg2) continue;
								for (int sF2 = sF1 + 1; sF2 < sfArray.length; sF2++) {
									LineUpPlayer sf2 = sfArray[sF2];
									if (sf2 == pg1 || sf2 == pg2 || sf2 == sg1 || sf2 == sg2) continue;
									for (int pF1 = 0; pF1 < pfArray.length - 1; pF1++) {
										LineUpPlayer pf1 = pfArray[pF1];
										if (pf1 == c || pf1 == sf2 || pf1 == sf1) continue;
										for (int pF2 = pF1 + 1; pF2 < pfArray.length; pF2++) {
											LineUpPlayer pf2 = pfArray[pF2];
											if (pf2 == c || pf2 == sf2 || pf2 == sf1) continue;
											int totalSalary = pg1.getCost() + pg2.getCost() + sg1.getCost() + sg2.getCost()
													+ sf1.getCost() + sf2.getCost() + pf1.getCost() + pf2.getCost() + c.getCost();
											if (totalSalary < FantasyLeague.FAN_DUEL.salaryCap) {
												lineUps.add(LineUp.newBuilder()
														.setId(id++)
														.setFantasySite(FantasyLeague.DRAFT_KINGS.name)
														.setProjectedPoints(pg1.getProjectedPoints() + pg2.getProjectedPoints() + sg1.getProjectedPoints() + sg2.getProjectedPoints()
														+ sf1.getProjectedPoints() + sf2.getProjectedPoints() + pf1.getProjectedPoints() + pf2.getProjectedPoints() + c.getProjectedPoints())
														.setTotalSalary(totalSalary)
														.addPlayer(pg1).addPlayer(pg2).addPlayer(sg1).addPlayer(sg2)
														.addPlayer(sf1).addPlayer(sf2).addPlayer(pf1).addPlayer(pf2).addPlayer(c)
														.build());
											}
										}
									}
								}
							}
						}
					}
				}
			}
			return lineUps;
		}).forEach(bestLineUps::addAll);
		return bestLineUps;
	}

	private class DistinictSalaryList extends ArrayList<LineUpPlayer> {
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
}
