package blogs.pipelines;

import blogs.Pipeline;
import org.springframework.context.ApplicationEvent;

import java.util.concurrent.atomic.AtomicInteger;

public class PipelineInitializedEvent extends ApplicationEvent {

	private static final AtomicInteger COUNTER = new AtomicInteger();

	public record PipelineCount(Pipeline pipeline, int count) {
	}

	public PipelineInitializedEvent(Pipeline pipeline) {
		super(new PipelineCount(pipeline, COUNTER.incrementAndGet()));
	}

}
