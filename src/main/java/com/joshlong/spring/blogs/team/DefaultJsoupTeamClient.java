package com.joshlong.spring.blogs.team;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.util.Assert;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
class DefaultJsoupTeamClient implements TeamClient {

	private final Supplier<String> htmlSupplier;

	@Override
	@SneakyThrows
	public Collection<Teammate> team() {
		var html = this.htmlSupplier.get();
		Assert.notNull(html, "the html must be valid");
		var doc = Jsoup.parse(html);
		var list = new ArrayList<Teammate>();
		for (var e : doc.select(".team-member--info")) {
			var url = "https://spring.io" + e.select("a").attr("href");
			var name = e.select(".team-member--name").text();
			var bio = e.select(".team-member--bio");
			var position = bio.select(".team-member--position").text();
			var location = bio.select(".team-member--location").text();
			var socialElement = e.select(".team-member--social");
			var map = new HashMap<Social, String>();
			for (var socialType : Social.values()) {
				var cssClazzName = socialType.className();
				var icon = socialElement.select("." + cssClazzName);
				for (var i : icon)
					map.put(socialType, i.attr("href"));
			}
			var teammate = new Teammate(from(url), name, position, location, map);
			list.add(teammate);
			log.debug(teammate.toString());
		}
		return list;
	}

	@SneakyThrows
	private static URL from(String s) {
		return new URL(s);
	}

}
