package com.joshlong.spring.blogs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties(prefix = "bootiful")
record JobProperties(Twitter twitter) {

	record Twitter(String username, String clientId, String clientSecret) {
	}

}
