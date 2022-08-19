package com.joshlong.spring.blogs.utils;

import lombok.SneakyThrows;

import java.net.URL;

/**
 * checked exceptions. not even once.
 */
public abstract class UrlUtils {

	@SneakyThrows
	public static URL buildUrl(String url) {
		return new URL(url);
	}

}
