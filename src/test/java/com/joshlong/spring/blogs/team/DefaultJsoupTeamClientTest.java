package com.joshlong.spring.blogs.team;

import com.joshlong.spring.blogs.TestUtils;
import com.joshlong.spring.blogs.utils.UrlUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultJsoupTeamClientTest {

	@Test
	void team() {
		var client = new DefaultJsoupTeamClient(TestUtils.supplier());
		var team = client.team();
		Assertions.assertTrue(team.stream()
				.anyMatch(t -> t.socialMedia().containsKey(Social.TWITTER) && t.socialMedia().containsKey(Social.GITHUB)
						&& t.location().contains("aisle seat")
						&& t.page().equals(UrlUtils.buildUrl("https://spring.io/team/joshlong"))
						&& t.twitter().contains("starbuxman") && t.name().equals("Josh Long")));

	}

}