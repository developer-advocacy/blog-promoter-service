package blogs;

import com.joshlong.twitter.Twitter;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SpringBootApplication
@ImportRuntimeHints(Main.Hints.class)
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
		return Executors.newScheduledThreadPool(2 * Runtime.getRuntime().availableProcessors());
	}

	static class Hints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

			var mcs = MemberCategory.values();

			// rome
			for (var c : new Class<?>[] { com.rometools.rome.feed.module.DCModuleImpl.class })
				hints.reflection().registerType(c, mcs);

			var resource = new ClassPathResource("/com/rometools/rome/rome.properties");
			hints.resources().registerResource(resource);
			try (var in = resource.getInputStream()) {
				var props = new Properties();
				props.load(in);
				props.propertyNames().asIterator().forEachRemaining(pn -> {
					var classes = loadClasses((String) pn, props.getProperty((String) pn));
					classes.forEach(cn -> hints.reflection().registerType(TypeReference.of(cn), mcs));
				});
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private static List<String> loadClasses(String propertyName, String propertyValue) {
			Assert.hasText(propertyName, "the propertyName must not be null");
			Assert.hasText(propertyValue, "the propertyValue must not be null");
			return Arrays //
					.stream((propertyValue.contains(" ")) ? propertyValue.split(" ") : new String[] { propertyValue }) //
					.map(String::trim).filter(xValue -> !xValue.strip().equals("")).toList();
		}

	}

}
