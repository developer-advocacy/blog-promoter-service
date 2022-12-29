package blogs;

import com.joshlong.twitter.Twitter;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SpringBootApplication
// @ImportRuntimeHints(Main.Hints.class)
@EnableConfigurationProperties(JobProperties.class)
public class Main {

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

	@Bean
	Twitter.Client client(JobProperties jp) {
		return new Twitter.Client(jp.twitter().clientId(), jp.twitter().clientSecret());
	}

	@Bean(destroyMethod = "")
	ScheduledExecutorService scheduledExecutorService() {
		return Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
	}

	/*
	 * static class Hints implements RuntimeHintsRegistrar {
	 *
	 * @Override public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
	 * var values = MemberCategory.values();
	 * hints.serialization().registerType(com.rometools.rome.feed.synd.SyndFeedImpl.class)
	 * ; for (var c : new Class<?>[] { com.rometools.rome.feed.synd.SyndFeedImpl.class })
	 * hints.reflection().registerType(c, values); }
	 *
	 * }
	 */

}
