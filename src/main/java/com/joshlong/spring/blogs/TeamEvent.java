package com.joshlong.spring.blogs;

import com.joshlong.spring.blogs.spring.team.Teammate;

import java.util.Set;

public record TeamEvent(Set<Teammate> teammates) {
}
