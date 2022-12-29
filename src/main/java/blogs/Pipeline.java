package blogs;

import com.rometools.rome.feed.synd.SyndEntry;

import java.net.URL;
import java.util.List;

/**
 * The idea is that this will handle all the work of sourcing, parsing, and enriching
 * text, giving the parent pipeline itself implementations of well-known types.
 */
public interface Pipeline {

	/**
	 * the source of the feed to be passed into Spring Integration
	 */
	URL getFeedUrl();

	String getTwitterUsername();

	/**
	 * the new entries from the feed need to be mapped into {@link BlogPost}
	 */
	BlogPost mapBlogPost(SyndEntry entry);

	/**
	 * the idea is that this may require some enrichment so provide a plug for this
	 * @param entry
	 * @return
	 */
	Author mapAuthor(BlogPost entry);

	List<PromotableBlog> getPromotableBlogs();

	BlogPost record(BlogPost post);

	String composeTweetFor(PromotableBlog pb);

}
