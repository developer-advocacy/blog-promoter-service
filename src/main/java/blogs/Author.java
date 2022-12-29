package blogs;

import java.util.Map;

public record Author(String name, Map<AuthorSocialMedia, String> socialMedia) {
}
