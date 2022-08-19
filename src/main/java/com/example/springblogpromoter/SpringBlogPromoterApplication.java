package com.example.springblogpromoter;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

// todo build a teams page screen scraper
// todo build a spring integration rss feed reader
// todo build out DB schema to persist the blogs
// todo build a templating thing to compose the tweets and ensure that the thing we tweet out fits in 280c

@SpringBootApplication
public class SpringBlogPromoterApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBlogPromoterApplication.class, args);
	}

	@Bean
	WebClient webClient(WebClient.Builder builder) {
		return builder.build();
	}

}

@Configuration
class ScreenScraperConfiguration {

	@Bean
	Supplier<String> httpHtmlSupplier(WebClient webClient) {
		return () -> webClient.get().uri("http://spring.io/team").retrieve().bodyToMono(String.class).block();
	}

}

enum Social {

	TWITTER("twitter-small"), GITHUB("github-small");

	private final String cn;

	Social(String className) {
		this.cn = className;
	}

	public String className() {
		return this.cn;
	}

}

record Teammate(URL page, String name, String position, String location, Map<Social, String> socialMedia) {

	public String twitter() {
		return this.socialMedia.getOrDefault(Social.TWITTER, null);
	}

	public String github() {
		return this.socialMedia.getOrDefault(Social.GITHUB, null);
	}
}

@Slf4j
@Component // todo extract this into its own config and dont rely on component scanning
@RequiredArgsConstructor
class DefaultJsoupTeamClient implements TeamClient {

	private final Supplier<String> htmlSupplier;

	@Override
	@SneakyThrows
	public Collection<Teammate> team() {
		var html = this.htmlSupplier.get();
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

interface TeamClient {

	Collection<Teammate> team();

}