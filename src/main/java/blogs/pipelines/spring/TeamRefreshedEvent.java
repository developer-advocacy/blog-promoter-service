package blogs.pipelines.spring;

import java.util.Set;

record TeamRefreshedEvent(Set<Teammate> teammates) {
}
