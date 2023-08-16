package blogs.pipelines.joshlong;

import blogs.*;
import blogs.pipelines.PipelineInitializedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.socialhubclient.AuthenticatedSocialHub;
import com.joshlong.socialhubclient.SocialHubChannels;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Configuration
class JoshLongPipelineConfiguration {

	@Bean
	ApplicationListener<ApplicationReadyEvent> joshlongApplicationReadyEventListener(Pipeline joshlong,
			ApplicationEventPublisher publisher) {
		return event -> publisher.publishEvent(new PipelineInitializedEvent(joshlong));
	}

	@Bean
	Pipeline joshlong(TransactionTemplate tt, JdbcTemplate ds, JobProperties properties, RestTemplate restTemplate,
			ObjectMapper objectMapper, SocialHubChannels channels) {

		var socialHub = new AuthenticatedSocialHub(properties.socialHub().clientId(),
				properties.socialHub().clientSecret(), channels.socialHubRequestsMessageChannel(),
				channels.socialHubErrorsMessageChannel(), restTemplate, objectMapper);

		var url = UrlUtils.buildUrl("https://api.joshlong.com/feed.xml");
		return new DefaulPipeline(url, tt, ds, socialHub) {

			@Override
			public Author mapAuthor(BlogPost entry) {
				return new Author("Josh Long", Map.of(AuthorSocialMedia.TWITTER, "starbuxman"));
			}
		};

	}

}
