package com.joshlong.spring.blogs.feed;

import java.net.URL;
import java.time.Instant;
import java.util.Set;

/**
 * container for the values that come back from the feed service
 * @param url
 * @param author
 * @param published
 * @param categories
 */
public record BlogPost(String title, URL url, String author, Instant published, Set<String> categories) {
}
