package com.joshlong.spring.blogs;

import com.joshlong.spring.blogs.feed.BlogPost;
import com.joshlong.spring.blogs.team.Social;
import com.joshlong.spring.blogs.team.Teammate;
import com.joshlong.spring.blogs.utils.UrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
class PromotionServiceTest {

	private final PromotionService service;

	PromotionServiceTest(@Autowired PromotionService service) {
		this.service = service;
	}

	@Test
	void promotable() throws Exception {

		this.service.addBlogPost(new BlogPost("this week in spring",
				UrlUtils.buildUrl("http://adobe.com/blog/this-week-in-" + UUID.randomUUID()), "Josh Long",
				Instant.now().minus(1, TimeUnit.HOURS.toChronoUnit()), Set.of("engineering")));
		this.service.addTeammate(Set.of(new Teammate(UrlUtils.buildUrl("https://spring.io/team/joshlong"), "Josh Long",
				"Spring Developer Advocate", "an aisle seat or san francisco", Map.of(Social.TWITTER,
						"https://twitter.com/starbuxman", Social.GITHUB, "http://github.com/joshlong"))));

		var blogs = this.service.getPromotableBlogs();
		Assertions.assertTrue(blogs.size() > 0);
		var josh = blogs.stream().anyMatch(PromotionServiceTest::confirm);
		Assertions.assertTrue(josh);
	}

	private static boolean confirm(PromotableBlog promotableBlog) {
		var teammate = promotableBlog.author();
		var author = teammate.socialMedia().containsKey(Social.TWITTER)
				&& teammate.socialMedia().containsKey(Social.GITHUB) && teammate.location().contains("aisle seat")
				&& teammate.page().equals(UrlUtils.buildUrl("https://spring.io/team/joshlong"))
				&& teammate.twitter().contains("starbuxman") && teammate.name().equals("Josh Long");
		var blog = promotableBlog.post().title().toLowerCase().contains("this week in")
				&& promotableBlog.post().url().toString().contains("this-week-in")
				&& promotableBlog.post().categories().contains("engineering")
				&& promotableBlog.post().published().isBefore(Instant.now());
		return blog && author;
	}

}