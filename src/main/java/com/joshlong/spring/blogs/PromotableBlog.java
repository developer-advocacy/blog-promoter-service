package com.joshlong.spring.blogs;

import com.joshlong.spring.blogs.feed.BlogPost;
import com.joshlong.spring.blogs.team.Teammate;

public record PromotableBlog(BlogPost post, Teammate author) {
}
