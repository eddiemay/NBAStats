package com.digitald4.nbastats.server;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.model.User;
import com.digitald4.common.server.service.GeneralDataService;
import com.digitald4.common.server.service.Echo;
import com.digitald4.nbastats.model.PlayerGameLog;
import com.digitald4.nbastats.storage.APIDAO;
import com.google.common.collect.ImmutableList;

public class EndPointsModule extends com.digitald4.common.server.EndPointsModule {

	@Override
	public void configureServlets() {
		super.configureServlets();

		// bind(User.class).to(BasicUser.class);
		bind(APIDAO.class).toProvider(() -> null);

		bind(Echo.class).toInstance(new Echo());
		configureEndpoints(API_URL_PATTERN,
				ImmutableList.of(Echo.class, PlayerService.class, PlayerDayService.class, LineUpService.class));
						// FileService.class, GeneralDataService.class));
	}

	@Override
	protected String getEndPointsProjectId() {
		return "fantasy-predictor";
	}
}
