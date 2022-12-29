package blogs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bootiful")
record JobProperties(Twitter twitter) {

	record Twitter(String clientId, String clientSecret) {
	}

}
