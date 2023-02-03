package blogs.pipelines.spring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.HashSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Configuration
class TeamConfiguration {

	private static WebClient redirectFollowingWebClient(WebClient.Builder builder) {
		var httpClient = HttpClient.create().compress(true) //
				.followRedirect(true);
		var client = new ReactorClientHttpConnector(httpClient);
		var exchangeStrategies = ExchangeStrategies//
				.builder() //
				.codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))//
				.build();
		return builder.exchangeStrategies(exchangeStrategies).clientConnector(client).build();
	}

	@Bean
	TeamClient springTeamClient(WebClient.Builder http) {
		var client = redirectFollowingWebClient(http);
		var supplier = buildHttpHtmlSupplier(client);
		return new DefaultJsoupTeamClient(supplier);
	}

	static Supplier<String> buildHttpHtmlSupplier(WebClient webClient) {
		return () -> webClient//
				.get() //
				.uri("https://spring.io/team") //
				.retrieve() //
				.bodyToMono(String.class) //
				.block();
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> teamApplicationReadyEventListener(TeamClient teamClient,
			ApplicationEventPublisher publisher, ScheduledExecutorService ses) {
		return new TeamApplicationReadyEventListener(teamClient, publisher, ses);
	}

	@RequiredArgsConstructor
	static class TeamApplicationReadyEventListener implements ApplicationListener<ApplicationReadyEvent> {

		private final TeamClient teamClient;

		private final ApplicationEventPublisher publisher;

		private final ScheduledExecutorService ses;

		private void refresh() {
			publisher.publishEvent(new TeamRefreshedEvent(new HashSet<>(teamClient.team())));
		}

		@Override
		public void onApplicationEvent(ApplicationReadyEvent event) {
			refresh();
			ses.scheduleAtFixedRate(this::refresh, 1, 1, TimeUnit.HOURS);
		}

	}

}
