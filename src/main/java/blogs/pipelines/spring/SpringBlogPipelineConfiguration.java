package blogs.pipelines.spring;

import blogs.*;
import com.joshlong.spring.blogs.utils.UrlUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Configuration
class SpringBlogPipelineConfiguration {

	private final Set<Teammate> teammateSet = new ConcurrentSkipListSet<>(
			Comparator.comparing(o -> o.page().toExternalForm()));

	private final Object monitor = new Object();

	@EventListener
	public void team(TeamRefreshedEvent teamRefreshedEvent) {
		synchronized (this.monitor) {
			this.teammateSet.clear();
			this.teammateSet.addAll(teamRefreshedEvent.teammates());
		}
	}

	@Bean
	Pipeline spring(TransactionTemplate tx, JdbcTemplate ds) {
		var url = UrlUtils.buildUrl("https://spring.io/blog.atom");
		return new DefaulPipeline(url, tx, ds) {

			@Override
			public Author mapAuthor(BlogPost entry) {
				var name = entry.author();
				Assert.hasText(name, "the name can't be null");
				for (var teammate : teammateSet) {
					if (teammate.name().equals(name)) {
						var map = new HashMap<AuthorSocialMedia, String>();
						teammate.socialMedia().forEach((social, username) -> {
							var authorSocialMedia = switch (social) {
							case TWITTER -> AuthorSocialMedia.TWITTER;
							case GITHUB -> AuthorSocialMedia.GITHUB;
							};
							map.put(authorSocialMedia, username);
						});
						return new Author(name, map);
					}
				}
			}
		};
	}

}
