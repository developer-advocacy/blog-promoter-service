package blogs;

import com.rometools.rome.feed.synd.SyndEntry;

import java.net.URL;
import java.util.List;

/**
 * The idea is that this will handle all the work of sourcing, parsing, and enriching
 * text, giving the parent pipeline itself implementations of well-known types.
 */
public interface Pipeline {

	URL getFeedUrl();

	String getTwitterUsername();

	BlogPost mapBlogPost(SyndEntry entry);

	Author mapAuthor(BlogPost entry);

	List<PromotableBlog> getPromotableBlogs();

	BlogPost record(BlogPost post);

	BlogPost promote(BlogPost post);

	String composeTweetFor(PromotableBlog pb);

}
