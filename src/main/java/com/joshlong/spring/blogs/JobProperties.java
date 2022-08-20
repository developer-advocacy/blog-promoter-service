package com.joshlong.spring.blogs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("bootiful")
record JobProperties(Twitter twitter) {

	record Twitter(String clientId, String clientSecret) {
	}
}
