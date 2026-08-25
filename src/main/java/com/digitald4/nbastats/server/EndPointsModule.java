package com.digitald4.nbastats.server;

import com.digitald4.common.model.BasicUser;
import com.digitald4.common.model.Company;
import com.digitald4.common.model.User;
import com.digitald4.common.server.service.Echo;
import com.digitald4.common.storage.Annotations;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericUserStore;
import com.digitald4.common.storage.LoginResolver;
import com.digitald4.common.storage.SearchIndexer;
import com.digitald4.common.storage.SearchIndexerAppEngineImpl;
import com.digitald4.common.storage.SessionStore;
import com.digitald4.common.storage.UserStore;
import com.digitald4.common.util.ProviderThreadLocalImpl;
import com.digitald4.nbastats.util.FakeWebFetcher;
import com.digitald4.nbastats.util.WebFetcher;
import com.google.common.collect.ImmutableList;
import com.google.inject.TypeLiteral;
import java.time.Duration;

public class EndPointsModule extends com.digitald4.common.server.EndPointsModule {

	public EndPointsModule() {
		super("fantasy-predictor");
	}

	@Override
	public void configureServlets() {
		super.configureServlets();

		bind(Duration.class).annotatedWith(Annotations.SessionDuration.class).toInstance(Duration.ofHours(8));
		bind(Boolean.class).annotatedWith(Annotations.SessionCacheEnabled.class).toInstance(false);
		bind(Company.class).toInstance(new Company().setName("Fantasy Predictor"));

		ProviderThreadLocalImpl<BasicUser> userProvider = new ProviderThreadLocalImpl<>();
		bind(User.class).toProvider(userProvider);
		bind(BasicUser.class).toProvider(userProvider);
		bind(new TypeLiteral<ProviderThreadLocalImpl<BasicUser>>(){}).toInstance(userProvider);
		UserStore<BasicUser> userStore = new GenericUserStore<>(BasicUser.class, getProvider(DAO.class));
		bind(new TypeLiteral<UserStore<BasicUser>>(){}).toInstance(userStore);
		bind(new TypeLiteral<UserStore<? extends com.digitald4.common.model.User>>(){}).toInstance(userStore);
		bind(LoginResolver.class).to(new TypeLiteral<SessionStore<BasicUser>>(){}).asEagerSingleton();
		bind(SearchIndexer.class).to(SearchIndexerAppEngineImpl.class);

		bind(WebFetcher.class).to(FakeWebFetcher.class);

		configureEndpoints(getApiUrlPattern(),
				ImmutableList.of(
						Echo.class,
						LineUpService.class, PlayerDayService.class, PlayerGameLogService.class, PlayerService.class));
	}
}
