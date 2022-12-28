package com.joshlong.spring.blogs.team;

import com.joshlong.spring.blogs.RefreshEvent;
import com.joshlong.spring.blogs.TeamEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Configuration
@RequiredArgsConstructor
class TeamConfiguration {

	@Bean
	ApplicationRunner scheduledTeammateRunner(ApplicationEventPublisher publisher, TaskScheduler taskScheduler) {
		return args -> {
			publisher.publishEvent(new RefreshEvent(new Date())); // startup
			taskScheduler.schedule(() -> publisher.publishEvent(new RefreshEvent()),
					new PeriodicTrigger(Duration.of(1, TimeUnit.HOURS.toChronoUnit()))); // henceforth
		};
	}

	@Bean
	TeamClient springTeamClient(WebClient http) {
		var supplier = buildHttpHtmlSupplier(http);
		return new DefaultJsoupTeamClient(supplier);
	}

	private static Supplier<String> buildHttpHtmlSupplier(WebClient webClient) {
		return () -> webClient//
				.get() //
				.uri("https://spring.io/team") //
				.retrieve() //
				.bodyToMono(String.class) //
				.block();
	}

	@Bean
	ApplicationListener<RefreshEvent> refreshEventApplicationListener(ApplicationEventPublisher publisher,
			TeamClient client) {
		return event -> publisher.publishEvent(new TeamEvent(client.team()));
	}

}
