package com.digitald4.nbastats.server;

import com.digitald4.common.proto.DD4UIProtos.ListResponse;
import com.digitald4.common.server.SingleProtoService;
import com.digitald4.nbastats.proto.NBAStatsProtos.Player;
import com.digitald4.nbastats.proto.NBAStatsProtos.PlayerListRequest;
import com.digitald4.nbastats.storage.PlayerStore;
import org.json.JSONObject;

public class PlayerService extends SingleProtoService<Player> {
	private final PlayerStore playerStore;
	public PlayerService(PlayerStore playerStore) {
		super(playerStore);
		this.playerStore = playerStore;
	}

	@Override
	public JSONObject list(JSONObject request) {
		return convertToJSON(list(transformJSONRequest(PlayerListRequest.getDefaultInstance(), request)));
	}

	public ListResponse list(PlayerListRequest request) {
		return toListResponse(playerStore.list(request.getSeason()));
	}

	@Override
	public boolean requiresLogin(String method) {
		return false;
	}
}
