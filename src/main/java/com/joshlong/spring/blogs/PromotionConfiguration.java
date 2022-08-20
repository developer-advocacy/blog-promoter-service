package com.joshlong.spring.blogs;

import com.joshlong.spring.blogs.feed.BlogPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableConfigurationProperties(JobProperties.class)
@RequiredArgsConstructor
class PromotionConfiguration {

	private final PromotionService service;

	@EventListener
	public void blogPost(BlogPost post) {
		this.service.addBlogPost(post);
	}

	@EventListener
	public void teammate(TeamEvent teamEvent) {
		this.service.addTeammate(teamEvent.teammates());
	}

	@Bean
	ApplicationRunner promotionApplicationRunner(TaskScheduler taskScheduler) {
		return args -> taskScheduler.schedule(() -> doPromotion(this.service),
				new PeriodicTrigger(1, TimeUnit.MINUTES));
	}

	private static void doPromotion(PromotionService promotionService) {
		log.debug("=============================================");
		var toPromote = promotionService.getPromotableBlogs();
		for (var promotable : toPromote) {
			var teammate = promotable.author();
			var blog = promotable.post();
			log.debug(String.format("going to promote post [%s] by [%s]", blog.toString(), teammate.toString()));
			var tweeted = tweet(promotable);
			if (tweeted) {
				promotionService.promote(blog);
			}
		}
	}

	private static boolean tweet(PromotableBlog promotableBlog) {
		log.debug("'tweeting' " + promotableBlog);
		return true;
	}

}
