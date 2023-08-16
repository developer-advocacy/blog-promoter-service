package blogs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bootiful")
public
record JobProperties(SocialHub socialHub) {

    public record SocialHub(String clientId, String clientSecret) {
    }

}
