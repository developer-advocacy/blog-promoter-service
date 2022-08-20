package com.joshlong.spring.blogs.twitter;

import com.joshlong.spring.blogs.twitter.TwitterClient;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TwitterConfiguration {

	@Bean
	TwitterClient twitterClient(StreamBridge streamBridge) {
		return new TwitterClient(streamBridge);
	}

}
