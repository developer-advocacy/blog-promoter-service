package com.joshlong.spring.blogs;

import com.joshlong.spring.blogs.utils.UrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.feed.dsl.Feed;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.web.reactive.function.client.WebClient;

// todo (X) build a teams page screen scraper
// todo build a spring integration rss feed reader
// todo build out DB schema to persist the blogs
// todo build a templating thing to compose the tweets and ensure that the thing we tweet out fits in 280c

@Slf4j
@SpringBootApplication
public class SpringBlogPromoterApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBlogPromoterApplication.class, args);
	}

	@Bean
	WebClient webClient(WebClient.Builder builder) {
		return builder.build();
	}

}
