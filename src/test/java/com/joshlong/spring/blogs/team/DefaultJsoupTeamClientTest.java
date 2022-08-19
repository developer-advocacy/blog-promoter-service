package com.joshlong.spring.blogs.team;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.function.Supplier;

class DefaultJsoupTeamClientTest {

	@Test
	void team() {
		var supplier = (Supplier<String>) () -> read(new ClassPathResource("/sample-team.html"));
		var client = new DefaultJsoupTeamClient(supplier);
		var team = client.team();
		Assertions.assertTrue(team.stream()
				.anyMatch(t -> t.socialMedia().containsKey(Social.TWITTER) && t.socialMedia().containsKey(Social.GITHUB)
						&& t.location().contains("aisle seat")
						&& t.page().equals(url("https://spring.io/team/joshlong")) && t.twitter().contains("starbuxman")
						&& t.name().equals("Josh Long")));

	}

	@SneakyThrows
	private static URL url(String url) {
		return new URL(url);
	}

	@SneakyThrows
	private static String read(Resource resource) {
		try (var in = new InputStreamReader(resource.getInputStream())) {
			return FileCopyUtils.copyToString(in);
		}
	}

}