package blogs.pipelines.spring;

import org.springframework.context.ApplicationEvent;

import java.util.Set;

class TeamRefreshedEvent extends ApplicationEvent {

	public TeamRefreshedEvent(Set<Teammate> teammates) {
		super(teammates);
	}

	@Override
	public Set<Teammate> getSource() {
		return (Set<Teammate>) super.getSource();
	}

}
