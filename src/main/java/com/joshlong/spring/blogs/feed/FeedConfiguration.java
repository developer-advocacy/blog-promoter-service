package com.joshlong.spring.blogs.feed;

import com.joshlong.spring.blogs.utils.UrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.feed.dsl.Feed;

@Slf4j
@Configuration
class FeedConfiguration {

	@Bean
	IntegrationFlow integrationFlow() {
		var atomFeedUrl = "https://spring.io/blog.atom";
		return IntegrationFlows.from(Feed.inboundAdapter(UrlUtils.buildUrl(atomFeedUrl), "spring-blog-feed"))
				.handle((payload, headers) -> {
					log.info("got a new message : " + payload.toString());
					headers.forEach((k, v) -> log.info(k + "=" + v));
					return null;
				}).get();
	}

}
