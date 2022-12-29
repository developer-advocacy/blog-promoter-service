package blogs;

import java.net.URL;
import java.time.Instant;
import java.util.Set;

public record BlogPost(String title, URL url, String author, Instant published, Set<String> categories) {
}
