package com.joshlong.spring.blogs;

import java.net.URL;
import java.time.Instant;
import java.util.List;

/**
 * container for the values that come back from the feed service
 * @param url
 * @param authors
 * @param published
 * @param categories
 */
public record BlogPost(URL url, List<String> authors, Instant published, List<String> categories) {
}
