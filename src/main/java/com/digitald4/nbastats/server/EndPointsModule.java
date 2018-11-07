package com.digitald4.nbastats.server;

import com.digitald4.common.server.service.GeneralDataService;
import com.digitald4.common.server.service.UserService;
import com.digitald4.common.server.service.Echo;
import com.digitald4.nbastats.storage.APIDAO;
import com.google.common.collect.ImmutableList;

public class EndPointsModule extends com.digitald4.common.server.EndPointsModule {

	@Override
	public void configureServlets() {
		super.configureServlets();

		bind(APIDAO.class).toProvider(() -> null);

		bind(Echo.class).toInstance(new Echo());
		bind(UserService.class);
		bind(PlayerService.class);
		configureEndpoints(API_URL_PATTERN,
				ImmutableList.of(Echo.class,
						/* FileService.class, */ GeneralDataService.class, UserService.class,
						LineUpService.class, PlayerDayService.class, PlayerService.class));
	}

	@Override
	protected String getEndPointsProjectId() {
		return "fantasy-predictor";
	}
}
