package com.joshlong.spring.blogs.team;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.function.Supplier;

@Configuration
class ScreenScraperConfiguration {

	@Bean
	Supplier<String> httpHtmlSupplier(WebClient webClient) {
		return () -> webClient.get().uri("http://spring.io/team").retrieve().bodyToMono(String.class).block();
	}

	@Bean
	TeamClient springTeamClient(Supplier<String> supplier) {
		return new DefaultJsoupTeamClient(supplier);
	}

}
