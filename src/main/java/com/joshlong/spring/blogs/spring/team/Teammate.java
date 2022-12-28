package com.joshlong.spring.blogs.spring.team;

import java.net.URL;
import java.util.Map;

public record Teammate(URL page, String name, String position, String location, Map<Social, String> socialMedia) {

	public String twitter() {
		return this.socialMedia.getOrDefault(Social.TWITTER, null);
	}

	public String github() {
		return this.socialMedia.getOrDefault(Social.GITHUB, null);
	}
}
