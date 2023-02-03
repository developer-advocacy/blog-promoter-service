package blogs.pipelines.spring;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.util.Assert;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
class DefaultJsoupTeamClient implements TeamClient {

	private final Supplier<String> htmlSupplier;

	@Override
	@SneakyThrows
	public Set<Teammate> team() {
		var html = this.htmlSupplier.get();
		Assert.notNull(html, "the html must be valid");
		var doc = Jsoup.parse(html);
		var teammates = new HashSet<Teammate>();
		for (var e : doc.select(".is-one-third")) {
			log.debug("-----------");
			var a = e.select("a");
			var url = "https://spring.io" + a.attr("href");
			var name = a.text().trim();
			var bio = e.select(".team-content");
			var position = bio.select(".job").text();
			var location = bio.select(".my-2").get(1).text();
			if (log.isDebugEnabled())
				log.debug(Map.of("url", url, "name", name, "position", position, "location", location) + "");
			// so far so good

			var socialStringHashMap = new HashMap<Social, String>();
			var teamSocial = e.select(".team-social");
			for (var socialLink : teamSocial.select("a")) {
				var href = socialLink.attr("href");
				var svg = socialLink.select("svg");
				var svgClassName = svg.attr("class").split(" ")[1].split("-")[1].trim().toUpperCase();
				try {
					var key = Social.valueOf(svgClassName);
					socialStringHashMap.put(key, href);
				} //
				catch (IllegalArgumentException iae) { //
					log.warn("no social media type for: {} ", svgClassName);
				}
			}
			log.debug("social: " + socialStringHashMap);

			var teammate = new Teammate(from(url), name, position, location, socialStringHashMap);
			teammates.add(teammate);
		}
		return teammates;
	}

	@SneakyThrows
	private static URL from(String s) {
		return new URL(s);
	}

}
