package com.digitald4.nbastats.storage;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.*;

import com.digitald4.common.storage.DAO;
import com.digitald4.common.util.Calculate;
import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp;
import com.digitald4.nbastats.proto.NBAStatsProtos.LineUp.LineUpPlayer;
import com.digitald4.nbastats.proto.NBAStatsProtos.Position;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.mockito.Mock;

public class LineUpStoreTest {
	@Mock private final DAO dao = mock(DAO.class);
	@Mock private final PlayerDayStore playerDayStore = mock(PlayerDayStore.class);
	private final LineUpStore lineUpStore = new LineUpStore(() -> dao, playerDayStore);

	@Test
	public void testProcessDraftKings() {
		List<LineUpPlayer> playerOptions = Arrays.asList(
				LineUpPlayer.newBuilder().setPlayerId(1).setName("A").setCost(1000).addPosition(Position.PG).setProjected(90).build(),
				LineUpPlayer.newBuilder().setPlayerId(2).setName("B").setCost(2000).addPosition(Position.SG).setProjected(110).build(),
				LineUpPlayer.newBuilder().setPlayerId(4).setName("C").setCost(2000).addPosition(Position.SF).setProjected(80).build(),
				LineUpPlayer.newBuilder().setPlayerId(8).setName("D").setCost(3000).addPosition(Position.PF).setProjected(40).build(),
				LineUpPlayer.newBuilder().setPlayerId(16).setName("E").setCost(3000).addPosition(Position.C).setProjected(100).build(),
				LineUpPlayer.newBuilder().setPlayerId(32).setName("F").setCost(3000).addPosition(Position.PG).addPosition(Position.SG).setProjected(50).build(),
				LineUpPlayer.newBuilder().setPlayerId(64).setName("G").setCost(4000).addPosition(Position.PG).addPosition(Position.SF).setProjected(120).build(),
				LineUpPlayer.newBuilder().setPlayerId(128).setName("H").setCost(4000).addPosition(Position.PF).addPosition(Position.C).setProjected(30).build(),
				LineUpPlayer.newBuilder().setPlayerId(256).setName("I").setCost(4000).addPosition(Position.SG).setProjected(70).build(),
				LineUpPlayer.newBuilder().setPlayerId(512).setName("J").setCost(4000).addPosition(Position.SG).setProjected(60).build(),
				LineUpPlayer.newBuilder().setPlayerId(1024).setName("K").setCost(5000).addPosition(Position.SF).setProjected(20).build(),
				LineUpPlayer.newBuilder().setPlayerId(2048).setName("L").setCost(5000).addPosition(Position.C).setProjected(10).build()
		);
		Set<LineUp> lineUps = lineUpStore.processDraftKings(playerOptions);
		System.out.println("LineUps: " + lineUps.size());
		lineUps.forEach(lineUp -> {
			lineUp.getPlayerList().forEach(p -> System.out.print(p.getName() + ","));
			System.out.println(lineUp.getTotalSalary() + "," + lineUp.getProjected() + "," + lineUp.getHashId());
		});
		System.out.println("Combinations: " + Calculate.combinations(12, 8));
		System.out.println("Valid Lineups: " + getValidList(playerOptions).size());
		assertEquals(100, lineUps.size());
	}

	private List<LineUp> getValidList(List<LineUpPlayer> lineUpPlayers) {
		List<LineUp> lineUps = new ArrayList<>();
		List<LineUpPlayer> utils = new ArrayList<>();
		List<LineUpPlayer> gs = new ArrayList<>();
		List<LineUpPlayer> sgs = new ArrayList<>();
		List<LineUpPlayer> pgs = new ArrayList<>();
		List<LineUpPlayer> fs = new ArrayList<>();
		List<LineUpPlayer> sfs = new ArrayList<>();
		List<LineUpPlayer> pfs = new ArrayList<>();
		List<LineUpPlayer> cs = new ArrayList<>();
		lineUpPlayers.stream().sorted(Comparator.comparing(LineUpPlayer::getProjected).reversed()).forEach(player -> {
			if (utils.add(player)) {
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
			}
		});
		Set<Integer> ids = new HashSet<>();
		for (LineUpPlayer util : utils) {
			for (LineUpPlayer g : gs) {
				if (g == util) continue;
				for (LineUpPlayer f : fs) {
					if (f == util || f == g) continue;
					for (LineUpPlayer pg : pgs) {
						if (pg == util || pg == f || pg == g) continue;
						for (LineUpPlayer sg : sgs) {
							if (sg == util || sg == g || sg == f || sg == pg) continue;
							for (LineUpPlayer sf : sfs) {
								if (sf == util || sf == g || sf == f || sf == pg || sf == sg) continue;
								for (LineUpPlayer pf : pfs) {
									if (pf == util || pf == g || pf == f || pf == pg || pf == sg || pf == sf) continue;
									for (LineUpPlayer c : cs) {
										if (c == util || c == g || c == f || c == pg || c == sg || c == sf || c == pf) continue;
										int id = util.getPlayerId() + g.getPlayerId() + f.getPlayerId()
												+ pg.getPlayerId() + sg.getPlayerId()
												+ sf.getPlayerId() + pf.getPlayerId() + c.getPlayerId();
										if (!ids.contains(id)) {
											//System.out.println(String.format("%s, %s, %s, %s, %s, %s, %s, %s", util.getName(), g.getName(),
												//	f.getName(), pg.getName(), sg.getName(), sf.getName(), pf.getName(), c.getName()));
											ids.add(id);
											lineUps.add(LineUp.newBuilder()
													.setFantasySite("draftkings")
													.setProjected(pg.getProjected() + sg.getProjected()
															+ sf.getProjected() + pf.getProjected() + c.getProjected()
															+ g.getProjected() + f.getProjected() + util.getProjected())
													.setTotalSalary(id)
													.addPlayer(util).addPlayer(g).addPlayer(f)
													.addPlayer(pg).addPlayer(sg)
													.addPlayer(sf).addPlayer(pf).addPlayer(c)
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
	}
}