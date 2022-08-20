package com.joshlong.spring.blogs;

import com.joshlong.spring.blogs.team.Teammate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
class PromotionRepository {

	private final JdbcTemplate template;

	@EventListener
	public void blogPost(BlogPost post) {
		log.debug("going to persist " + post);
		var sql = """

				""";
		this.template.execute(sql);

	}

	@EventListener
	public void teammate(Teammate teammate) {
		log.debug("going to persist " + teammate);
	}

}
