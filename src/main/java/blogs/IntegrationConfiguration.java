package blogs;

import com.joshlong.twitter.Twitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.feed.dsl.Feed;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * this is stuff that'll apply to all Spring Integration flows.
 */
@Slf4j
@Configuration
class IntegrationConfiguration {

	@Bean
	DataSourceMetadataStore dataSourceMetadataStore(JdbcTemplate jdbcTemplate,
			TransactionTemplate transactionTemplate) {
		return new DataSourceMetadataStore(jdbcTemplate, transactionTemplate);
	}

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE + 100)
	ApplicationListener<ApplicationReadyEvent> promotionIntegrationFlowApplicationReadyListener(Twitter twitter,
			Twitter.Client client, Map<String, Pipeline> pipelines, IntegrationFlowContext ctx) {
		Assert.state(pipelines.size() > 0, () -> "you must configure at least one " + Pipeline.class.getName());
		log.debug("kicking off " + getClass().getName() + '.');
		return event -> pipelines.forEach((id, blogPromotionPipeline) -> {
			log.info("the id [" + id + "] is mapped to [" + blogPromotionPipeline + "]");
			var flow = this.buildPromotionIntegrationFlow(twitter, client, id, blogPromotionPipeline);
			ctx.registration(flow).register().start();
		});
	}

	// todo is there a smart way to avoid re-polling the DB on each fetch?
	private IntegrationFlow buildPromotionIntegrationFlow(Twitter twitter, Twitter.Client client, String id,
			Pipeline pipeline) {

		return IntegrationFlow//
				.from((MessageSource<PromotableBlog>) () -> {
					var promotable = pipeline.getPromotableBlogs();
					if (promotable != null && promotable.size() > 0) {
						return MessageBuilder.withPayload(promotable.get(0)).build();
					}
					log.debug("there are no " + PromotableBlog.class.getName() + "s to promote for pipeline [" + id
							+ "], returning null");
					return null;
				}, p -> p.poller(pc -> pc.fixedRate(1, TimeUnit.MINUTES)))//
				.filter(PromotableBlog.class,
						promotableBlog -> promotableBlog.blogPost().published()
								.isAfter(Instant.now().minus(Duration.ofDays(2))))
				.handle((GenericHandler<PromotableBlog>) (payload, headers) -> {
					var tweet = pipeline.composeTweetFor(payload);
					var sent = twitter.scheduleTweet(client, new Date(), pipeline.getTwitterUsername(), tweet, null);
					if (Objects.equals(sent.block(), Boolean.TRUE)) {
						log.debug("sent a tweet for " + payload.blogPost().title());
						pipeline.promote(payload.blogPost());
					}
					return null;
				}).get();
	}

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE + 100)
	ApplicationListener<ApplicationReadyEvent> ingestFeedIntegrationFlowApplicationReadyListener(
			Map<String, Pipeline> pipelines, IntegrationFlowContext ctx, MetadataStore metadataStore) {
		Assert.state(pipelines.size() > 0, () -> "you must configure at least one " + Pipeline.class.getName());
		log.debug("kicking off " + getClass().getName() + '.');
		return event -> pipelines.forEach((id, blogPromotionPipeline) -> {
			log.info("the id [" + id + "] is mapped to [" + blogPromotionPipeline + "]");
			var flow = this.buildIngestIntegrationFlow(id, blogPromotionPipeline, metadataStore);
			ctx.registration(flow).register().start();
		});
	}

	private IntegrationFlow buildIngestIntegrationFlow(String beanName, Pipeline promotionPipeline,
			MetadataStore metadataStore) {
		log.debug("launching " + IntegrationFlow.class.getName() + " for " + beanName + '.');
		var inbound = Feed //
				.inboundAdapter(promotionPipeline.getFeedUrl(), beanName) //
				.metadataStore(metadataStore);
		return IntegrationFlow //
				.from(inbound, p -> p.poller(pm -> pm.fixedRate(1, TimeUnit.SECONDS)))//
				.transform(promotionPipeline::mapBlogPost) //
				.transform(promotionPipeline::record) //
				.handle((GenericHandler<BlogPost>) (payload, headers) -> {
					if (log.isDebugEnabled()) {
						var url = payload.url();
						log.debug("ingested a blogPost [" + url + "]");
						headers.forEach((key, value) -> log.debug(url + ":" + key + '=' + value));
					}
					return null;
				}).get();
	}

}
