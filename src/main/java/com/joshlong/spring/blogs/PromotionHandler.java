package com.joshlong.spring.blogs;

import com.joshlong.spring.blogs.feed.BlogPost;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PromotionHandler {

	private final PromotionService service;

	@EventListener
	public void blogPost(BlogPost post) {
		this.service.addBlogPost(post);
	}

	@EventListener
	public void teammate(TeamEvent teamEvent) {
		this.service.addTeammate(teamEvent.teammates());
	}

}
