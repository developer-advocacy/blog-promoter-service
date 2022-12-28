package com.joshlong.spring.blogs.feed;

import com.joshlong.spring.blogs.metadata.DataSourceMetadataStore;
import com.joshlong.spring.blogs.utils.UrlUtils;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndPerson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.event.outbound.ApplicationEventPublishingMessageHandler;
import org.springframework.integration.feed.dsl.Feed;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Configuration
class FeedConfiguration {

	@Bean
	DataSourceMetadataStore dataSourceMetadataStore(JdbcTemplate jdbcTemplate,
			TransactionTemplate transactionTemplate) {
		return new DataSourceMetadataStore(jdbcTemplate, transactionTemplate);
	}

	@Bean
	ApplicationEventPublishingMessageHandler eventHandler() {
		var handler = new ApplicationEventPublishingMessageHandler();
		handler.setPublishPayload(true);
		return handler;
	}

	@Bean
	IntegrationFlow integrationFlow(MetadataStore metadataStore,
			ApplicationEventPublishingMessageHandler eventHandler) {
		var atomFeedUrl = "https://spring.io/blog.atom";
		var inbound = Feed //
				.inboundAdapter(UrlUtils.buildUrl(atomFeedUrl), "spring-blog-feed") //
				.metadataStore(metadataStore);
		return IntegrationFlow //
				.from(inbound, p -> p.poller(pm -> pm.fixedRate(1, TimeUnit.SECONDS)))//
				.transform((GenericTransformer<SyndEntry, BlogPost>) source -> {
					var title = source.getTitle();
					var published = publishedDate(source);
					var uri = source.getLink();
					var authors = source.getAuthors().stream().map(SyndPerson::getName).distinct().toList();
					var categories = source.getCategories().stream().map(SyndCategory::getName).map(String::toLowerCase)
							.collect(Collectors.toSet());
					return new BlogPost(title, UrlUtils.buildUrl(uri), authors.get(0), published, categories);
				}) //
				.handle(eventHandler) //
				.get();
	}

	private static Instant publishedDate(SyndEntry entry) {
		var dates = new Date[] { entry.getUpdatedDate(), entry.getPublishedDate() };
		for (var date : dates)
			if (null != date)
				return date.toInstant();
		return null;
	}

}
