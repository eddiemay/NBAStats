package com.digitald4.nbastats.server;

import com.digitald4.common.server.SingleProtoService;
import com.digitald4.nbastats.proto.NBAStatsProtos.Player;
import com.digitald4.nbastats.storage.PlayerStore;

public class PlayerService extends SingleProtoService<Player> {
	public PlayerService(PlayerStore playerStore) {
		super(playerStore);
	}

	@Override
	public boolean requiresLogin(String method) {
		return false;
	}
}
