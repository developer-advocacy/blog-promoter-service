package com.joshlong.spring.blogs.feed;

import com.joshlong.spring.blogs.metadata.DataSourceMetadataStore;
import com.joshlong.spring.blogs.utils.UrlUtils;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndPerson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.event.outbound.ApplicationEventPublishingMessageHandler;
import org.springframework.integration.feed.dsl.Feed;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static IntegrationFlow feedFlowFor(MetadataStore metadataStore, URL atomFeedUrl, String feedAdapterName,
                                               Function<SyndEntry, BlogPost> mapper, ApplicationEventPublishingMessageHandler eventHandler) {
        log.info("registering IntegrationFlow for " + feedAdapterName + " with URL " + atomFeedUrl.toExternalForm());
        var inbound = Feed //
                .inboundAdapter(atomFeedUrl, feedAdapterName) //
                .metadataStore(metadataStore);
        return IntegrationFlow //
                .from(inbound, p -> p.poller(pm -> pm.fixedRate(1, TimeUnit.SECONDS)))//
                .transform(new GenericTransformer<SyndEntry, BlogPost>() {
                    @Override
                    public BlogPost transform(SyndEntry source) {
                        return mapper.apply(source);
                    }
                }) //
                .filter((Predicate<BlogPost>) entry -> entry.published().isAfter(Instant.now().minus(7, TimeUnit.DAYS.toChronoUnit())))
                .handle(eventHandler) //
                .get();
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> ready(BlogSource[] blogSources, MetadataStore metadataStore,
                                                     ApplicationEventPublishingMessageHandler eventHandler, IntegrationFlowContext ctx) {
        Assert.state(blogSources.length > 0, () -> "you must configure at least one " + BlogSource.class.getName());
        return event -> Stream //
                .of(blogSources) //
                .map(blogSourceInstance -> feedFlowFor(metadataStore, blogSourceInstance.url(),
                        idFrom(blogSourceInstance.url()), blogSourceInstance::mapSyndEntryToBlogPost, eventHandler)) //
                .map(ctx::registration)//
                .map(IntegrationFlowContext.IntegrationFlowRegistrationBuilder::register)//
                .forEach(registration -> System.out.println("registered a flow for  " + registration));
    }

    private String idFrom(URL url) {
        var ef = url.toExternalForm();
        var nb = new StringBuilder();
        for (var c : ef.toCharArray())
            if (Character.isAlphabetic(c))
                nb.append(c);
        return nb + "Feed";
    }

}

@Configuration
class BlogSourceConfiguration {

    @Bean
    BlogSource joshlongBlogSource() {
        return new BlogSource(UrlUtils.buildUrl("https://api.joshlong.com/feed.xml"));
    }

    @Bean
    BlogSource springBlogSource() {
        return new BlogSource(UrlUtils.buildUrl("https://spring.io/blog.atom"));
    }

}

record BlogSource(URL url, Function<SyndEntry, BlogPost> syndEntryBlogPostMapper) {

    BlogSource(URL url) {
        this(url, null);
    }

    BlogSource(URL url, Function<SyndEntry, BlogPost> syndEntryBlogPostMapper) {
        this.url = url;
        this.syndEntryBlogPostMapper = (null != syndEntryBlogPostMapper) ? syndEntryBlogPostMapper
                : this::mapSyndEntryToBlogPost;
    }

    BlogPost mapSyndEntryToBlogPost(SyndEntry source) {
        var title = source.getTitle();
        var published = publishedDate(source);
        var uri = source.getLink();
        var authors = source.getAuthors().stream().map(SyndPerson::getName).distinct().toList();
        var categories = source.getCategories().stream().map(SyndCategory::getName).map(String::toLowerCase)
                .collect(Collectors.toSet());
        var author = authors.size() > 0 ? authors.get(0) : null;
        return new BlogPost(title, UrlUtils.buildUrl(uri), author, published, categories);
    }

    private Instant publishedDate(SyndEntry entry) {
        var dates = new Date[]{entry.getUpdatedDate(), entry.getPublishedDate()};
        for (var date : dates)
            if (null != date)
                return date.toInstant();
        return null;
    }

}
