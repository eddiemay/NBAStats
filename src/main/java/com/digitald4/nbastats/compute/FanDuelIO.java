package com.digitald4.nbastats.compute;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.*;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.FormatText;
import com.digitald4.common.util.Pair;
import com.digitald4.nbastats.distributed.FantasyProcessor;
import com.digitald4.nbastats.model.LineUp;
import com.digitald4.nbastats.model.PlayerDay;
import com.digitald4.nbastats.model.PlayerDay.FantasySiteInfo;
import com.digitald4.nbastats.storage.NBAApiDAO;
import com.digitald4.nbastats.storage.PlayerGameLogStore;
import com.digitald4.nbastats.storage.LineUpStore;
import com.digitald4.nbastats.storage.PlayerDayStore;
import com.digitald4.nbastats.storage.PlayerStore;
import com.digitald4.nbastats.util.Constaints;
import com.digitald4.nbastats.util.Constaints.FantasyLeague;
import com.digitald4.nbastats.util.DistinictSalaryList;
import com.google.common.collect.ImmutableList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.joda.time.DateTime;

public class FanDuelIO {
	private static final int PAIR_LIMIT = 6;
	private static final int SINGLE_LIMIT = 6;
	private static final String league = FantasyLeague.FAN_DUEL.name;
	private final StatsProcessor statsProcessor;
	private final LineUpStore lineUpStore;
	private final PlayerDayStore playerDayStore;

	private FanDuelIO(StatsProcessor statsProcessor, LineUpStore lineUpStore, PlayerDayStore playerDayStore) {
		this.statsProcessor = statsProcessor;
		this.lineUpStore = lineUpStore;
		this.playerDayStore = playerDayStore;
	}

	public void output(DateTime date) throws IOException {
		List<PlayerDay> pgs = new DistinictSalaryList(PAIR_LIMIT, league);
		List<PlayerDay> sgs = new DistinictSalaryList(PAIR_LIMIT, league);
		List<PlayerDay> sfs = new DistinictSalaryList(PAIR_LIMIT, league);
		List<PlayerDay> pfs = new DistinictSalaryList(PAIR_LIMIT, league);
		List<PlayerDay> cs = new DistinictSalaryList(SINGLE_LIMIT, league);

		ImmutableList<PlayerDay> selected = statsProcessor.processStats(date)
				.stream()
				.filter(playerDay -> playerDay.getFantasySiteInfo(league).getProjections().size() > 4)
				.sorted((p1, p2) -> Double.compare(
						p2.getFantasySiteInfo(league).getProjection("RotoG Proj").getProjected(),
						p1.getFantasySiteInfo(league).getProjection("RotoG Proj").getProjected()))
				.peek(player -> {
					switch (player.getFantasySiteInfo(league).getPositions().get(0)) {
						case "PG": pgs.add(player); break;
						case "SG": sgs.add(player); break;
						case "SF": sfs.add(player); break;
						case "PF": pfs.add(player); break;
						case "C": cs.add(player); break;
					}
				})
				.collect(toImmutableList());

		write(selected, date);

		output(date, pgs, sgs, sfs, pfs, cs);
	}

	private void outputActuals(DateTime date) throws IOException {
		List<PlayerDay> pgs = new ArrayList<>();
		List<PlayerDay> sgs = new ArrayList<>();
		List<PlayerDay> sfs = new ArrayList<>();
		List<PlayerDay> pfs = new ArrayList<>();
		List<PlayerDay> cs = new ArrayList<>();

		ImmutableList<PlayerDay> selected = statsProcessor.updateActuals(date)
				.stream()
				.filter(playerDay -> playerDay.getFantasySiteInfo(league).getActual() > 0)
				.sorted((p1, p2) -> Double.compare(
						p2.getFantasySiteInfo(league).getActual(),
						p1.getFantasySiteInfo(league).getActual()))
				.peek(player -> {
					switch (player.getFantasySiteInfo(league).getPositions().get(0)) {
						case "PG": pgs.add(player); break;
						case "SG": sgs.add(player); break;
						case "SF": sfs.add(player); break;
						case "PF": pfs.add(player); break;
						case "C": cs.add(player); break;
					}
				})
				.collect(toImmutableList());

		writeActual(selected, date);

		output(date, pgs, sgs, sfs, pfs, cs);
	}

	private void output(
			DateTime date, List<PlayerDay> pgs, List<PlayerDay> sgs, List<PlayerDay> sfs, List<PlayerDay> pfs,
			List<PlayerDay> cs) throws IOException {
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
		long bcI = (long) pgPs.size() * sgPs.size();
		long fcI = (long) sfPs.size() * pfPs.size() * cs.size();
		System.out.println("BackCourt Iterations: " + bcI);
		System.out.println("FrontCourt Iterations: " + fcI);
		System.out.println("Iterations: " + NumberFormat.getInstance().format(bcI * fcI));

		FileWriter fw = new FileWriter(String.format(FantasyProcessor.BACK_COURT_PATH, date.toString(Constaints.COMPUTER_DATE)));
		long backCourtCount = 0;
		final int FOUR_PLAYER_SALARY_LIMIT = FantasyLeague.FAN_DUEL.salaryCap - (4 * 3500);
		for (Pair<PlayerDay, PlayerDay> pgPair : pgPs) {
			PlayerDay pg1 = pgPair.getLeft();
			PlayerDay pg2 = pgPair.getRight();
			int pgCost = pg1.getFantasySiteInfo(league).getCost() + pg2.getFantasySiteInfo(league).getCost();
			for (Pair<PlayerDay, PlayerDay> sgPair : sgPs) {
				PlayerDay sg1 = sgPair.getLeft();
				PlayerDay sg2 = sgPair.getRight();
				int sgCost = sg1.getFantasySiteInfo(league).getCost() + sg2.getFantasySiteInfo(league).getCost();
				if (pgCost + sgCost < FOUR_PLAYER_SALARY_LIMIT) {
					backCourtCount++;
					fw.write(pg1.getPlayerId() + "," + pg2.getPlayerId() + "," + sg1.getPlayerId() + "," + sg2.getPlayerId()
							+ "\n");
				}
			}
		}
		fw.close();

		fw = new FileWriter(String.format(FantasyProcessor.FRONT_COURT_PATH, date.toString(Constaints.COMPUTER_DATE)));
		long frontCourtCount = 0;
		final int FIVE_PLAYER_SALARY_LIMIT = FantasyLeague.FAN_DUEL.salaryCap - (4 * 3500);
		for (Pair<PlayerDay, PlayerDay> sfPair : sfPs) {
			PlayerDay sf1 = sfPair.getLeft();
			PlayerDay sf2 = sfPair.getRight();
			int sfCost = sf1.getFantasySiteInfo(league).getCost() + sf2.getFantasySiteInfo(league).getCost();
			for (Pair<PlayerDay, PlayerDay> pfPair : pfPs) {
				PlayerDay pf1 = pfPair.getLeft();
				PlayerDay pf2 = pfPair.getRight();
				int pfCost = pf1.getFantasySiteInfo(league).getCost() + pf2.getFantasySiteInfo(league).getCost();
				for (PlayerDay c : cs) {
					if (sfCost + pfCost + c.getFantasySiteInfo(league).getCost() < FIVE_PLAYER_SALARY_LIMIT) {
						frontCourtCount++;
						fw.write(sf1.getPlayerId() + "," + sf2.getPlayerId() + "," + pf1.getPlayerId() + "," + pf2.getPlayerId()
								+ "," + c.getPlayerId() + "\n");
					}
				}
			}
		}
		fw.close();

		System.out.println("BackCourt Final Count: " + backCourtCount);
		System.out.println("FrontCourt Final Count: " + frontCourtCount);
		System.out.println("Iterations Final: " + NumberFormat.getInstance().format(backCourtCount * frontCourtCount));
	}

	private static void write(List<PlayerDay> players, DateTime date) throws IOException {
		FileWriter fw = new FileWriter(String.format(FantasyProcessor.PLAYERS_PATH, date.toString(Constaints.COMPUTER_DATE)));
		for (FantasySiteInfo.Projection projection : players.get(0).getFantasySiteInfo(league).getProjections()) {
			fw.write(projection.getBasis() + ",");
		}
		for (PlayerDay playerDay : players) {
			fw.write("\n" + write(playerDay));
		}
		fw.close();
	}

	private static String write(PlayerDay player) {
		FantasySiteInfo fantasySiteInfo = player.getFantasySiteInfo(league);
		return player.getPlayerId() + "," + fantasySiteInfo.getCost() + "," + fantasySiteInfo.getProjections()
				.stream()
				.map(proj -> String.valueOf(Calculate.round(proj.getProjected(), 1)))
				.collect(Collectors.joining(","));
	}

	private static void writeActual(List<PlayerDay> players, DateTime date) throws IOException {
		FileWriter fw = new FileWriter(String.format(FantasyProcessor.PLAYERS_PATH, date.toString(Constaints.COMPUTER_DATE)));
		fw.write("Actual");
		for (PlayerDay player : players) {
			FantasySiteInfo fantasySiteInfo = player.getFantasySiteInfo(league);
			fw.write("\n" + player.getPlayerId() + "," + fantasySiteInfo.getCost() + "," + fantasySiteInfo.getActual());
		}
		fw.close();
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

	private void insertData(DateTime date) {
		playerDayStore
				.list(
						new Query()
								.setFilters(new Filter().setColumn("date").setValue(date.toString(Constaints.COMPUTER_DATE)))
								.setLimit(1))
				.getResults()
				.get(0)
				.getFantasySiteInfo(league)
				.getProjections()
				.parallelStream()
				.map(FantasySiteInfo.Projection::getBasis)
				.forEach(basis -> {
					lineUpStore.delete(
							lineUpStore
									.list(
											new Query().setFilters(
													new Filter().setColumn("fantasy_site").setValue(league),
													new Filter().setColumn("date").setValue(date.toString(Constaints.COMPUTER_DATE)),
													new Filter().setColumn("projection_method").setValue(basis)))
									.getResults()
									.stream()
									.map(LineUp::getId)
									.collect(toImmutableList()));
					String fileName = String.format(FantasyProcessor.OUTPUT_PATH, date.toString(Constaints.COMPUTER_DATE), basis);
					try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
						String line;
						while ((line = reader.readLine()) != null) {
							String[] parts = line.split(",");
							lineUpStore.create(new LineUp()
									.setDate(date.toString(Constaints.COMPUTER_DATE))
									.setFantasySite(league)
									.setProjected(Double.parseDouble(parts[0].trim()))
									.setProjectionMethod(parts[1])
									.setTotalSalary(Integer.parseInt(parts[2]))
									.setPlayerIds(
											Arrays.asList(parts).subList(3, parts.length).stream()
													.map(Integer::parseInt)
													.collect(Collectors.toList())));
						}
					} catch (IOException e) {
						System.out.println("Error opening file: " + fileName);
					}
				});
	}

	private void insertActuals(DateTime date) {
		lineUpStore.delete(
				lineUpStore
						.list(
								new Query().setFilters(
										new Filter().setColumn("fantasy_site").setValue(league),
										new Filter().setColumn("date").setValue(date.toString(Constaints.COMPUTER_DATE)),
										new Filter().setColumn("projection_method").setValue("Actual")))
						.getResults()
						.stream()
						.map(LineUp::getId)
						.collect(toImmutableList()));

		String fileName = String.format(FantasyProcessor.OUTPUT_PATH, date.toString(Constaints.COMPUTER_DATE), "Actual");
		try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(",");
				lineUpStore.create(
						new LineUp()
								.setDate(date.toString(Constaints.COMPUTER_DATE))
								.setFantasySite(league)
								.setActual(Double.parseDouble(parts[0].trim()))
								.setProjectionMethod("Actual")
								.setTotalSalary(Integer.parseInt(parts[2]))
								.setPlayerIds(
										Arrays.asList(parts).subList(3, parts.length).stream()
												.map(Integer::parseInt)
												.collect(Collectors.toList())));
			}
		} catch (IOException e) {
			System.out.println("Error opening file: " + fileName);
		}
	}

	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		DateTime processDate = null;
		String action = "output";
		for (int a = 0; a < args.length; a++) {
			if (args[a].equals("--date")) {
				if (a + 1 < args.length) {
					processDate = DateTime.parse(args[++a]);
				}
			} else {
				action = args[a];
			}
		}

		APIConnector apiConnector = new APIConnector("https://fantasy-predictor.appspot.com/_ah/api", "v1");
		DAOApiProtoImpl messageDAO = new DAOApiProtoImpl(apiConnector);
		DAORouterImpl dao = new DAORouterImpl(messageDAO, new HasProtoDAO(messageDAO), new DAOApiImpl(apiConnector));
		Provider<DAO> daoProvider = () -> dao;
		NBAApiDAO apiDAO = new NBAApiDAO(new APIConnector(null, null, 500));
		PlayerStore playerStore = new PlayerStore(daoProvider, apiDAO);
		PlayerGameLogStore playerGameLogStore = new PlayerGameLogStore(daoProvider, apiDAO);
		PlayerDayStore playerDayStore = new PlayerDayStore(daoProvider, apiDAO);
		LineUpStore lineUpStore = new LineUpStore(daoProvider);
		StatsProcessor statsProcessor = new StatsProcessor(playerStore, playerGameLogStore, playerDayStore, lineUpStore);
		FanDuelIO fanDuelIO = new FanDuelIO(statsProcessor, lineUpStore, playerDayStore);
		if ("output".equals(action)) {
			fanDuelIO.output(processDate != null ? processDate : DateTime.now().minusHours(8));
		} else if ("updateActuals".equals(action)) {
			fanDuelIO.outputActuals(processDate != null ? processDate : DateTime.now().minusDays(1));
		} else if ("insert".equals(action)) {
			fanDuelIO.insertData(processDate != null ? processDate : DateTime.now().minusHours(8));
		} else if ("insertActuals".equals(action)) {
			fanDuelIO.insertActuals(processDate != null ? processDate : DateTime.now().minusDays(1));
		}
		System.out.println("Total Time: " + FormatText.formatElapshed((System.currentTimeMillis() - startTime)));
	}
}

//  1 Game  2017-11-21                   0
//  2 Games 2018-01-04           5,080,320
//  3 Games 2017-12-19         177,724,800
//  4 Games 2018-01-25       2,495,001,600
//  5 Games 2018-02-01      12,158,407,800
//  6 Games 2018-02-13     115,847,584,776
//  7 Games 2018-02-10     140,806,575,000
//  8 Games 2018-02-11     946,586,414,592
//  9 Games 2018-02-09   1,882,112,453,760
// 10 Games 2018-01-26   4,385,412,757,440
// 11 Games 2018-01-10   3,882,158,280,000
// 12 Games 2018-02-14
// 13 Games 2017-12-23  38,266,469,287,200
// 14 Games 2017-11-22 201,411,011,856,600
