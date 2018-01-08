package com.digitald4.nbastats.compute;

import com.digitald4.common.jdbc.DBConnectorThreadPoolImpl;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.DAOCloudDS;
import com.digitald4.common.storage.DAOSQLImpl;
import com.digitald4.common.tools.DataImporter;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.Pair;
import com.digitald4.common.util.Provider;
import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp;
import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp.LineUpPlayer;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerDay.FantasySiteInfo;
import com.digitald4.nbastats.proto.NBAStatsProtos.Position;
import com.digitald4.nbastats.storage.APIDAO;
import com.digitald4.nbastats.storage.GameLogStore;
import com.digitald4.nbastats.storage.LineUpStore;
import com.digitald4.nbastats.storage.PlayerDayStore;
import com.digitald4.nbastats.storage.PlayerStore;
import com.digitald4.nbastats.util.Constaints;
import com.digitald4.nbastats.util.Constaints.FantasyLeague;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

public class FanDuelIO {
	private static final String PLAYER_OUTPUT = "%d,%d,%s";
	private final StatsProcessor statsProcessor;
	private final PlayerDayStore playerDayStore;
	private final LineUpStore lineUpStore;

	public FanDuelIO(StatsProcessor statsProcessor, PlayerDayStore playerDayStore, LineUpStore lineUpStore) {
		this.statsProcessor = statsProcessor;
		this.playerDayStore = playerDayStore;
		this.lineUpStore = lineUpStore;
	}

	public void output(DateTime date) throws IOException {
		String league = FantasyLeague.FAN_DUEL.name;
		List<PlayerDay> pgs = new ArrayList<>();
		List<PlayerDay> sgs = new ArrayList<>();
		List<PlayerDay> sfs = new ArrayList<>();
		List<PlayerDay> pfs = new ArrayList<>();
		List<PlayerDay> cs = new ArrayList<>();

		statsProcessor.processStats(date)
				.stream()
				.filter(playerDay -> playerDay.getFantasySiteInfoOrDefault(league, FantasySiteInfo.getDefaultInstance()).getProjectionCount() != 0)
				.forEach(player -> {
					switch (player.getFantasySiteInfoOrThrow(league).getPosition(0)) {
						case PG: pgs.add(player); break;
						case SG: sgs.add(player); break;
						case SF: sfs.add(player); break;
						case PF: pfs.add(player); break;
						case C: cs.add(player); break;
					}
				});
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
		System.out.println("Iterations: " +
				FormatText.formatCurrency(((long) pgPs.size()) * sgPs.size() * sfPs.size() * pfPs.size() * cs.size()));

		String processPath = ProcessFanDuel.PROCESS_PATH + "pg_pairs.txt";
		String dataPath = ProcessFanDuel.DATA_PATH;
		write(processPath, pgPs.subList(0, Math.min(pgPs.size(), 465)));
		write(dataPath + "sg_pairs.txt", sgPs);
		write(dataPath + "sf_pairs.txt", sfPs);
		write(dataPath + "pf_pairs.txt", pfPs);
		FileWriter fw = new FileWriter("input/fanduel/data/centers.txt");
		for (PlayerDay center : cs) {
			fw.write(write(center) + "\n");
		}
		fw.close();
	}

	private static void write(String fileName, List<Pair<PlayerDay, PlayerDay>> pairs) throws IOException {
		FileWriter fw = new FileWriter(fileName);
		PlayerDay player = pairs.get(0).getLeft();
		if (player.getFantasySiteInfoOrThrow(FantasyLeague.FAN_DUEL.name).getPosition(0).equals(Position.SG)) {
			for (String method : player.getFantasySiteInfoOrThrow(FantasyLeague.FAN_DUEL.name).getProjectionMap().keySet()) {
				fw.write(method + ",");
			}
			fw.write("\n");
		}
		for (Pair<PlayerDay, PlayerDay> pair : pairs) {
			fw.write(write(pair.getLeft()) + "|" + write(pair.getRight()) + "\n");
		}
		fw.close();
	}

	private static String write(PlayerDay player) {
		StringBuilder sb = new StringBuilder();
		FantasySiteInfo fantasySiteInfo = player.getFantasySiteInfoOrThrow(FantasyLeague.FAN_DUEL.name);
		fantasySiteInfo.getProjectionMap().values().forEach(projection -> sb.append(projection).append(","));
		return String.format(PLAYER_OUTPUT, player.getPlayerId(), fantasySiteInfo.getCost(), sb.toString());
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
		String league = FantasyLeague.FAN_DUEL.name;
		Map<Integer, PlayerDay> playerDayMap = playerDayStore.list(date).getResultList()
				.stream()
				.collect(Collectors.toMap(PlayerDay::getPlayerId, Function.identity()));
		BufferedReader reader = new BufferedReader(new FileReader(
				String.format(ProcessFanDuel.OUTPUT_PATH, date.toString(DateTimeFormat.forPattern("yyyy-MM-dd"))) + "part-r-00000"));
		Map<String, AtomicInteger> methodCounts = new HashMap<>();
		playerDayMap.values().iterator().next().getFantasySiteInfoOrThrow(league).getProjectionMap().keySet()
				.forEach(method -> methodCounts.put(method, new AtomicInteger()));
		String line;
		while ((line = reader.readLine()) != null && !done(methodCounts.values())) {
			String[] parts = line.split(",");
			String method = parts[1];
			if (methodCounts.get(method).get() < Constaints.LINEUP_LIMIT) {
				methodCounts.get(method).incrementAndGet();
				LineUp.Builder lineUp = LineUp.newBuilder()
						.setDate(date.toString(Constaints.COMPUTER_DATE))
						.setFantasySite(FantasyLeague.FAN_DUEL.name)
						.setProjected(Double.parseDouble(parts[0].trim()))
						.setProjectionMethod(method)
						.setTotalSalary(Integer.parseInt(parts[2]));
				double projected = 0;
				for (int i = 3; i < parts.length; i++) {
					PlayerDay playerDay = playerDayMap.get(Integer.parseInt(parts[i]));
					double pp = playerDay.getFantasySiteInfoOrThrow(league).getProjectionOrThrow(method);
					projected += pp;
					lineUp.addPlayer(LineUpPlayer.newBuilder()
							.setPlayerId(playerDay.getPlayerId())
							.setName(playerDay.getName())
							.addAllPosition(playerDay.getFantasySiteInfoOrThrow(league).getPositionList())
							.setCost(playerDay.getFantasySiteInfoOrThrow(league).getCost())
							.setProjected(pp)
							.build());
				}
				lineUpStore.create(lineUp.setProjected(projected).build());
			}
		}
		reader.close();
	}

	public static boolean done(Collection<AtomicInteger> counts) {
		for (AtomicInteger count : counts) {
			if (count.get() < Constaints.LINEUP_LIMIT) {
				return false;
			}
		}
		return true;
	}

	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();
		System.out.println("CA Lottery Combinations: " + (Calculate.combinations(70, 5) * 25));
		String dataStore = "cloud";
		for (int a = 0; a < args.length; a++) {
			if (args[a].equals("--datastore")) {
				dataStore = args[++a];
			}
		}
		DAO dao = dataStore.equals("cloud") ? new DAOCloudDS()
				: new DAOSQLImpl(new DBConnectorThreadPoolImpl("org.gjt.mm.mysql.Driver",
				"jdbc:mysql://localhost/NBAStats?autoReconnect=true",
				"dd4_user", "getSchooled85"));
		Provider<DAO> daoProvider = () -> dao;
		APIDAO apiDAO = new APIDAO(new DataImporter(null, null));
		PlayerDayStore playerDayStore = new PlayerDayStore(daoProvider, apiDAO);
		LineUpStore lineUpStore = new LineUpStore(daoProvider);
		StatsProcessor statsProcessor =
				new StatsProcessor(new PlayerStore(daoProvider, apiDAO), new GameLogStore(daoProvider, apiDAO), playerDayStore);
		FanDuelIO fanDuelIO = new FanDuelIO(statsProcessor, playerDayStore, lineUpStore);
		DateTime date = DateTime.now().minusHours(8);
		fanDuelIO.output(date);
		fanDuelIO.insertData(date);
		System.out.println("Total Time: " + ((System.currentTimeMillis() - startTime) / 1000) + " secs");
	}
}
