package com.digitald4.nbastats.compute;

import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.DAOAPIImpl;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.Pair;
import com.digitald4.common.util.Provider;
import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay.FantasySiteInfo;
import com.digitald4.nbastats.storage.APIDAO;
import com.digitald4.nbastats.storage.GameLogStore;
import com.digitald4.nbastats.storage.LineUpStore;
import com.digitald4.nbastats.storage.PlayerDayStore;
import com.digitald4.nbastats.storage.PlayerStore;
import com.digitald4.nbastats.util.Constaints;
import com.digitald4.nbastats.util.Constaints.FantasyLeague;
import com.digitald4.nbastats.util.DistinictSalaryList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

public class FanDuelIO {
	private final StatsProcessor statsProcessor;
	private final LineUpStore lineUpStore;

	private FanDuelIO(StatsProcessor statsProcessor, LineUpStore lineUpStore) {
		this.statsProcessor = statsProcessor;
		this.lineUpStore = lineUpStore;
	}

	public void output(DateTime date) throws IOException {
		String league = FantasyLeague.FAN_DUEL.name;
		List<PlayerDay> pgs = new DistinictSalaryList(40, league);
		List<PlayerDay> sgs = new DistinictSalaryList(40, league);
		List<PlayerDay> sfs = new DistinictSalaryList(40, league);
		List<PlayerDay> pfs = new DistinictSalaryList(40, league);
		List<PlayerDay> cs = new DistinictSalaryList(20, league);

		List<PlayerDay> selected = statsProcessor.processStats(date)
				.stream()
				.filter(playerDay -> playerDay.getFantasySiteInfoOrDefault(league, FantasySiteInfo.getDefaultInstance()).getProjectionCount() > 4)
				.sorted((p1, p2) -> Double.compare(p2.getFantasySiteInfoOrThrow(league).getProjectionOrDefault("RotoG Proj", 0), p1.getFantasySiteInfoOrThrow(league).getProjectionOrDefault("RotoG Proj", 0)))
				.peek(player -> {
					switch (player.getFantasySiteInfoOrThrow(league).getPosition(0)) {
						case PG: pgs.add(player); break;
						case SG: sgs.add(player); break;
						case SF: sfs.add(player); break;
						case PF: pfs.add(player); break;
						case C: cs.add(player); break;
					}
				})
				.collect(Collectors.toList());
		List<Pair<PlayerDay, PlayerDay>> pgPs = toPairList(pgs);
		List<Pair<PlayerDay, PlayerDay>> sgPs = toPairList(sgs);
		List<Pair<PlayerDay, PlayerDay>> sfPs = toPairList(sfs);
		List<Pair<PlayerDay, PlayerDay>> pfPs = toPairList(pfs);
		System.out.println("PGs: " + pgs.size());
		System.out.println("SGs: " + sgs.size());
		System.out.println("SFs: " + sfs.size());
		System.out.println("PFs: " + pfs.size());
		System.out.println("Cs: " + cs.size());
		System.out.println("PG Pairs: " + pgPs.size());
		System.out.println("SG Pairs: " + sgPs.size());
		System.out.println("SF Pairs: " + sfPs.size());
		System.out.println("PF Pairs: " + pfPs.size());
		System.out.println("Iterations: " + NumberFormat.getInstance().format(
				((long) pgPs.size()) * sgPs.size() * sfPs.size() * pfPs.size() * cs.size()));

		String processPath = ProcessFanDuel.PROCESS_PATH + "pg_pairs.csv";
		String dataPath = ProcessFanDuel.DATA_PATH;
		write(processPath, pgPs.subList(0, Math.min(pgPs.size(), 465)));
		write(selected);
		write(dataPath + "sg_pairs.csv", sgPs);
		write(dataPath + "sf_pairs.csv", sfPs);
		write(dataPath + "pf_pairs.csv", pfPs);
		FileWriter fw = new FileWriter("input/fanduel/data/centers.csv");
		for (PlayerDay center : cs) {
			fw.write(center.getPlayerId() + "\n");
		}
		fw.close();
	}

	private static void write(String fileName, List<Pair<PlayerDay, PlayerDay>> pairs) throws IOException {
		FileWriter fw = new FileWriter(fileName);
		for (Pair<PlayerDay, PlayerDay> pair : pairs) {
			fw.write(pair.getLeft().getPlayerId() + "," + pair.getRight().getPlayerId() + "\n");
		}
		fw.close();
	}

	private static void write(List<PlayerDay> players) throws IOException {
		FileWriter fw = new FileWriter(ProcessFanDuel.DATA_PATH + "players.csv");
		for (String method : players.get(0).getFantasySiteInfoOrThrow(FantasyLeague.FAN_DUEL.name).getProjectionMap().keySet()) {
			fw.write(method + ",");
		}
		fw.write("\n");
		for (PlayerDay playerDay : players) {
			fw.write(write(playerDay) + "\n");
		}
		fw.close();
	}

	private static String write(PlayerDay player) {
		FantasySiteInfo fantasySiteInfo = player.getFantasySiteInfoOrThrow(FantasyLeague.FAN_DUEL.name);
		return player.getPlayerId() + "," + fantasySiteInfo.getCost() + "," + fantasySiteInfo.getProjectionMap().values()
				.stream()
				.map(proj -> String.valueOf(Calculate.round(proj, 1)))
				.collect(Collectors.joining(","));
	}

	private static List<Pair<PlayerDay, PlayerDay>> toPairList(List<PlayerDay> players) {
		List<Pair<PlayerDay, PlayerDay>> pairs = new ArrayList<>(players.size() * 2);
		for (int i = 0; i < players.size() - 1; i++) {
			PlayerDay player1 = players.get(i);
			for (int j = i + 1; j < players.size(); j++) {
				pairs.add(Pair.of(player1, players.get(j)));
			}
		}
		return pairs;
	}

	private void insertData(DateTime date) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(
				String.format(ProcessFanDuel.OUTPUT_PATH, date.toString(DateTimeFormat.forPattern("yyyy-MM-dd"))) + "part-r-00000"));
		Map<String, AtomicInteger> methodCounts = new HashMap<>();
		String line;
		while ((line = reader.readLine()) != null) {
			String[] parts = line.split(",");
			String method = parts[1];
			if (methodCounts.computeIfAbsent(method, method_ -> new AtomicInteger()).get() < Constaints.LINEUP_LIMIT) {
				methodCounts.get(method).incrementAndGet();
				lineUpStore.create(LineUp.newBuilder()
						.setDate(date.toString(Constaints.COMPUTER_DATE))
						.setFantasySite(FantasyLeague.FAN_DUEL.name)
						.setProjected(Double.parseDouble(parts[0].trim()))
						.setProjectionMethod(method)
						.setTotalSalary(Integer.parseInt(parts[2]))
						.addAllPlayerId(Arrays.asList(parts).subList(3, parts.length).stream()
								.map(Integer::parseInt).collect(Collectors.toList()))
						.build());
			}
		}
		reader.close();
	}

	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		String command = (args.length > 0) ? args[0] : "output";
		DAO dao = new DAOAPIImpl(new APIConnector("https://fantasy-predictor.appspot.com/api"));
		Provider<DAO> daoProvider = () -> dao;
		APIDAO apiDAO = new APIDAO(new APIConnector(null));
		PlayerStore playerStore = new PlayerStore(daoProvider, apiDAO);
		GameLogStore gameLogStore = new GameLogStore(daoProvider, apiDAO);
		PlayerDayStore playerDayStore = new PlayerDayStore(daoProvider, apiDAO);
		LineUpStore lineUpStore = new LineUpStore(daoProvider);
		StatsProcessor statsProcessor = new StatsProcessor(playerStore, gameLogStore, playerDayStore, lineUpStore);
		FanDuelIO fanDuelIO = new FanDuelIO(statsProcessor, lineUpStore);
		DateTime now = DateTime.now();
		if ("output".equals(command)) {
			fanDuelIO.output(now.minusHours(8));
		} else if ("updateActuals".equals(command)) {
			statsProcessor.updateActuals(now.minusDays(1));
		} else if ("insert".equals(command)) {
			fanDuelIO.insertData(now.minusHours(8));
		}
		System.out.println("Total Time: " + FormatText.formatElapshed((System.currentTimeMillis() - startTime)));
	}
}
