package com.joshlong.spring.blogs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.spring.blogs.feed.BlogPost;
import com.joshlong.spring.blogs.team.Teammate;
import com.joshlong.spring.blogs.twitter.TwitterClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableConfigurationProperties(JobProperties.class)
class PromotionConfiguration {

	private final PromotionService promotions;

	private final TwitterClient twitter;

	private final String twitterClientId, twitterClientSecret, twitterUsername;

	private final ObjectMapper mapper;

	PromotionConfiguration(ObjectMapper objectMapper, PromotionService promotions, TwitterClient twitter,
			JobProperties properties) {
		this.promotions = promotions;
		this.twitter = twitter;
		this.mapper = objectMapper;
		this.twitterUsername = properties.twitter().username();
		this.twitterClientId = properties.twitter().clientId();
		this.twitterClientSecret = properties.twitter().clientSecret();
	}

	@EventListener
	public void blogPost(BlogPost post) {
		this.promotions.addBlogPost(post);
	}

	@EventListener
	public void teammate(TeamEvent teamEvent) {
		this.promotions.addTeammate(teamEvent.teammates());
	}

	@Bean
	ApplicationRunner promotionApplicationRunner(TaskScheduler taskScheduler) {
		return args -> taskScheduler.schedule(this::doPromotion, new PeriodicTrigger(1, TimeUnit.MINUTES));
	}

	private void doPromotion() {
		log.debug("=============================================");
		var toPromote = this.promotions.getPromotableBlogs();
		for (var promotable : toPromote) {
			var teammate = promotable.author();
			var blog = promotable.post();
			log.debug(String.format("going to promote post [%s] by [%s]", blog.toString(), teammate.toString()));
			var tweeted = tweet(promotable);
			if (tweeted) {
				this.promotions.promote(blog);
			}
		}
	}

	private static String authorReference(Teammate teammate) {
		var twitter = teammate.twitter();
		if (StringUtils.hasText(twitter))
			return "@" + twitter.substring(1 + twitter.lastIndexOf('/'));
		var github = teammate.github();
		if (StringUtils.hasText(github))
			return github.substring(github.lastIndexOf('/'));
		return teammate.name();
	}

	@SneakyThrows
	private boolean tweet(PromotableBlog promotableBlog) {
		log.debug("'tweeting' " + promotableBlog);
		var when = Instant.now().plus(5, TimeUnit.MINUTES.toChronoUnit()).atZone(ZoneId.systemDefault()).toInstant();
		var message = TweetTextComposer.compose(String.format("new from %s: %s",
				authorReference(promotableBlog.author()), promotableBlog.post().title()),
				promotableBlog.post().url().toExternalForm());
		var json = this.mapper.writeValueAsString(Map.of("text", message));
		var sent = twitter.scheduleTweet(new TwitterClient.Client(this.twitterClientId, this.twitterClientSecret),
				Date.from(when), this.twitterUsername, json);
		return Boolean.TRUE.equals(sent.block());
	}

}

@Slf4j
abstract class TweetTextComposer {

	public static final int MAX_TWEET_LENGTH = 280;

	static String compose(String title, String url) {
		var ellipse = "...";
		var full = buildFullTweetText(title, url);
		if (full.length() <= MAX_TWEET_LENGTH)
			return full;
		var delta = full.length() - MAX_TWEET_LENGTH;
		var desiredWidth = title.length() - delta;
		return buildFullTweetText(rTrimToSpace(title, desiredWidth - ellipse.length()) + ellipse, url);
	}

	private static String buildFullTweetText(String title, String url) {
		return String.format("%s #springboot %s", title, url);
	}

	private static String rTrimToSpace(String text, int desired) {
		while (text.length() >= desired) {
			var lindx = text.lastIndexOf(' ');
			if (lindx != -1)
				text = text.substring(0, lindx);
		}
		return text;
	}

}