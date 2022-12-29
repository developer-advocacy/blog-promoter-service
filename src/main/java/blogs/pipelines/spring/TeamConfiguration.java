package blogs.pipelines.spring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Configuration
class TeamConfiguration {

	@Bean
	TeamClient springTeamClient(WebClient.Builder http) {
		var supplier = buildHttpHtmlSupplier(http.build());
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
			var copy = new HashSet<>(teamClient.team());
			log.debug("publishing " + TeamRefreshedEvent.class.getName());
			publisher.publishEvent(new TeamRefreshedEvent(copy));
		}

		@Override
		public void onApplicationEvent(ApplicationReadyEvent event) {
			refresh();
			ses.scheduleAtFixedRate(this::refresh, 1, 1, TimeUnit.HOURS);
		}

	}

}
