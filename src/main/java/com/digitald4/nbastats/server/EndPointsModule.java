package com.digitald4.nbastats.server;

import com.digitald4.common.server.service.Echo;
import com.digitald4.nbastats.util.FakeWebFetcher;
import com.digitald4.nbastats.util.WebFetcher;
import com.google.common.collect.ImmutableList;

public class EndPointsModule extends com.digitald4.common.server.EndPointsModule {

	public EndPointsModule() {
		super("fantasy-predictor");
	}

	@Override
	public void configureServlets() {
		super.configureServlets();

		bind(WebFetcher.class).to(FakeWebFetcher.class);

		configureEndpoints(getApiUrlPattern(),
				ImmutableList.of(
						Echo.class, LineUpService.class, PlayerDayService.class, PlayerGameLogService.class, PlayerService.class));
	}
}
