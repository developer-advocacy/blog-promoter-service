package blogs.pipelines.spring;

import blogs.*;
import blogs.pipelines.PipelineInitializedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.socialhubclient.AuthenticatedSocialHub;
import com.joshlong.socialhubclient.SocialHubChannels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Configuration
class SpringPipelineConfiguration {

    private final AtomicReference<Set<Teammate>> teammateSet = new AtomicReference<>();


    @Bean
    Pipeline spring( JobProperties properties,TransactionTemplate tx, JdbcTemplate ds, RestTemplate restTemplate, ObjectMapper objectMapper, SocialHubChannels channels) {
        var socialHub = new AuthenticatedSocialHub( properties.socialHub().clientId(),
                properties.socialHub().clientSecret(), channels.socialHubRequestsMessageChannel(),
                channels.socialHubErrorsMessageChannel(), restTemplate, objectMapper);
        var url = UrlUtils.buildUrl("https://spring.io/blog.atom");
        return new DefaulPipeline(url, tx, ds, socialHub) {

            @Override
            public Author mapAuthor(BlogPost entry) {
                var teammates = teammateSet.get();
                Assert.state(teammates != null && !teammates.isEmpty(), "no teammates found");
                var name = entry.author();
                Assert.hasText(name, "the name can't be null");
                for (var teammate : teammates) {
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
                return null;
            }
        };
    }

    @Bean
    ApplicationListener<TeamRefreshedEvent> teamRefreshedEventApplicationListener(Pipeline spring,
                                                                                  ApplicationEventPublisher publisher) {
        return teamRefreshedEvent -> {
            var teammates = teamRefreshedEvent.getSource();
            Assert.notNull(teammates, "the teammates collection should not be null");
            Assert.state(!teammates.isEmpty(), "there should be a non-zero number of teammates");
            log.debug("got a " + TeamRefreshedEvent.class.getSimpleName() + " with " + teammates.size() + " entries");
            this.teammateSet.set(Collections.synchronizedSet(new HashSet<>(teammates)));
            publisher.publishEvent(new PipelineInitializedEvent(spring));
        };
    }

}
