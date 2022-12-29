package blogs.pipelines.joshlong;

import blogs.*;
import blogs.pipelines.UrlUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

@Configuration
class JoshLongBlogPipelineConfiguration {

	@Bean
	Pipeline joshlong(TransactionTemplate tt, JdbcTemplate ds) {
		var url = UrlUtils.buildUrl("https://api.joshlong.com/feed.xml");
		return new DefaulPipeline(url, tt, ds, "starbuxman") {

			@Override
			public Author mapAuthor(BlogPost entry) {
				return new Author("Josh Long", Map.of(AuthorSocialMedia.TWITTER, "starbuxman"));
			}
		};
	}

}
