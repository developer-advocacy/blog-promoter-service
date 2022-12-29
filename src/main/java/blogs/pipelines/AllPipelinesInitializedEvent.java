package blogs.pipelines;

import blogs.Pipeline;
import org.springframework.context.ApplicationEvent;

public class AllPipelinesInitializedEvent extends ApplicationEvent {

	public AllPipelinesInitializedEvent(Pipeline[] pipelines) {
		super(pipelines);
	}

}
