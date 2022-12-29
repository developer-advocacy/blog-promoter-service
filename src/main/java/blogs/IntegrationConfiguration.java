package blogs;

import com.joshlong.spring.blogs.metadata.DataSourceMetadataStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.feed.dsl.Feed;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.util.Map;
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
	/*
	 *
	 * @Bean ApplicationEventPublishingMessageHandler
	 * applicationEventPublishingMessageHandler() { var handler = new
	 * ApplicationEventPublishingMessageHandler(); handler.setPublishPayload(true); return
	 * handler; }
	 */

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	ApplicationListener<ApplicationReadyEvent> promotionIntegrationFlowApplicationReadyListener(
			Map<String, Pipeline> pipelines, IntegrationFlowContext ctx, MetadataStore metadataStore) {
		Assert.state(pipelines.size() > 0, () -> "you must configure at least one " + Pipeline.class.getName());
		log.debug("kicking off " + getClass().getName() + '.');
		return event -> pipelines.forEach((id, blogPromotionPipeline) -> {
			log.info("the id [" + id + "] is mapped to [" + blogPromotionPipeline + "]");
			var flow = this.buildPromotionIntegrationFlow(id, blogPromotionPipeline, metadataStore);
			ctx.registration(flow).register().start();
		});
	}

	// todo build something that pops a PromotableBlog from a collection one message at a
	// time
	// todo we don't want the MessagweSource to constantly be re-polling the DB.
	// todo for now we'll be dumb about it to make sure it works and do the worst possible
	// thing: constantly re-run the query
	private IntegrationFlow buildPromotionIntegrationFlow(String id, Pipeline pipeline, MetadataStore metadataStore) {
		return IntegrationFlow.from((MessageSource<PromotableBlog>) () -> {
			var promotable = pipeline.getPromotableBlogs();
			if (promotable != null && promotable.size() > 0) {
				return MessageBuilder.withPayload(promotable.get(0)).build();
			}
			log.debug("there are no " + PromotableBlog.class.getName() + "s to promote for pipeline [" + id
					+ "]. returning null");
			return null;
		}).get();
	}

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
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
				.transform(promotionPipeline::record).get();
	}

}
