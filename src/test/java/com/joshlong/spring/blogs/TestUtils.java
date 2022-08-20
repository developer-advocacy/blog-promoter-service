package com.joshlong.spring.blogs;

import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.util.function.Supplier;

public abstract class TestUtils {

	public static Supplier<String> supplier() {
		return () -> read(new ClassPathResource("/sample-team.html"));
	}

	@SneakyThrows
	private static String read(Resource resource) {
		try (var in = new InputStreamReader(resource.getInputStream())) {
			return FileCopyUtils.copyToString(in);
		}
	}

}
