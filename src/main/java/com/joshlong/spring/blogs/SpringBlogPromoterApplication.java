package com.joshlong.spring.blogs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@EnableScheduling
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
