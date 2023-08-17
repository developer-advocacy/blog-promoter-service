package blogs;

import blogs.pipelines.AllPipelinesInitializedEvent;
import blogs.pipelines.PipelineInitializedEvent;
import com.joshlong.socialhubclient.SocialHub;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.PollerFactory;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.feed.dsl.Feed;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

@Slf4j
@Configuration
class IntegrationConfiguration {

    @Bean
    ApplicationListener<PipelineInitializedEvent> pipelineInitializedApplicationListener(
            Map<String, Pipeline> pipelines, ApplicationEventPublisher publisher) {
        var counter = new AtomicInteger();
        return event -> {
            counter.incrementAndGet();
            if (counter.get() == pipelines.size()) {
                log.debug("publishing " + AllPipelinesInitializedEvent.class.getSimpleName() + "!");
                publisher.publishEvent(new AllPipelinesInitializedEvent(pipelines.values().toArray(new Pipeline[0])));
            }
        };
    }

    @Bean
    MetadataStore dataSourceMetadataStore(JdbcTemplate jdbcTemplate,  TransactionTemplate transactionTemplate) {
        return new DataSourceMetadataStore(jdbcTemplate, transactionTemplate);
    }

    @Bean
    ApplicationListener<AllPipelinesInitializedEvent> feedIngestIntegrationFlowRunner(Map<String, Pipeline> pipelines,
                                                                                      IntegrationFlowContext ctx, MetadataStore metadataStore) {
        return event -> visitPipelinesAndLaunchIntegrationFlow(ctx, pipelines,
                (id, pipeline) -> buildIngestIntegrationFlow(id, pipeline, metadataStore));
    }

    @Bean
    ApplicationListener<AllPipelinesInitializedEvent> promotionIntegrationFlowRunner(Map<String, Pipeline> pipelines, IntegrationFlowContext ctx) {
        return event -> visitPipelinesAndLaunchIntegrationFlow(ctx, pipelines, IntegrationConfiguration::buildPromotionIntegrationFlow);
    }

    /**
     * for each pipeline, apply the {@link BiFunction}, and then return the result.
     */
    private static void visitPipelinesAndLaunchIntegrationFlow(IntegrationFlowContext context,
                                                               Map<String, Pipeline> pipelines, BiFunction<String, Pipeline, IntegrationFlow> mapper) {
        pipelines.forEach((id, blogPromotionPipeline) -> context.registration(mapper.apply(id, blogPromotionPipeline))
                .register().start());
    }

    private static IntegrationFlow buildPromotionIntegrationFlow(String id, Pipeline pipeline) {
        var socialHub = pipeline.socialHub();
        var promotableBlogSimpleName = PromotableBlog.class.getSimpleName();
        return IntegrationFlow//
                .from((MessageSource<PromotableBlog>) () -> {
                    var promotable = pipeline.getPromotableBlogs();
                    var size = promotable.size();
                    if (size > 0) {
                        log.debug("there are " + size + " " + promotableBlogSimpleName + "s to promote for pipeline ["
                                  + id + "]");
                        return MessageBuilder.withPayload(promotable.get(0)).build();
                    }
                    log.debug(
                            "there are no " + promotableBlogSimpleName + " blogs to promote for pipeline [" + id + "]");
                    return null;
                }, p -> p.poller(pc -> PollerFactory.fixedRate(Duration.ofMinutes(1))))//
                .handle((GenericHandler<PromotableBlog>) (payload, headers) -> {
                    try {
                        log.info(
                                "got a PromotableBlog to promote whose published date is {} and the current date is {}",
                                payload.blogPost().published() + "", Instant.now());
                        log.debug("attempting to promote " + payload.blogPost().title());
                        var tweet = pipeline.composeTweetFor(payload);
                        var resources = new SocialHub.MediaResource[0];
                        socialHub.post(new SocialHub.Post("twitter".split(","), tweet, resources));
                        log.debug("sent a tweet for " + payload.blogPost().title());
                        pipeline.promote(payload.blogPost());
                        return null;
                    } //
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }) //
                .get();
    }

    private static IntegrationFlow buildIngestIntegrationFlow(String beanName, Pipeline promotionPipeline,
                                                              MetadataStore metadataStore) {
        var inbound = Feed //
                .inboundAdapter(promotionPipeline.getFeedUrl(), beanName) //
                .metadataStore(metadataStore);
        return IntegrationFlow //
                .from(inbound, p -> p.poller(pm -> PollerFactory.fixedRate(Duration.ofSeconds(1))))//
                .transform(promotionPipeline::mapBlogPost) //
                .transform(promotionPipeline::record) //
                .handle((GenericHandler<BlogPost>) (payload, headers) -> {
                    log.debug("ingested a blogPost [" + payload.url() + "]");
                    return null;
                }) //
                .get();
    }

}
