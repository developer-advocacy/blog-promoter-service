package blogs;

import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndPerson;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.sql.Array;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * This class captures that which changes from one blog promotion campaign to another,
 * things like the RSS/ATOM feed from which the posts are drawn, how authors of the blog
 * are reference in the tweets, the text of the tweets, the username that shall be used to
 * issue the tweets, etc.
 *
 * @author Josh Long
 */
@Slf4j
@RequiredArgsConstructor
public class DefaulPipeline implements BeanNameAware, Pipeline {

	private final AtomicReference<String> beanName = new AtomicReference<>();

	private final URL url;

	private final TransactionTemplate tx;

	private final JdbcTemplate ds;

	private final String twitterUsername;

	@Override
	public String getTwitterUsername() {
		return this.twitterUsername;
	}

	@Override
	public BlogPost promote(BlogPost post) {
		var sql = """
				    update blog_posts set promoted = ? where url = ?
				""";
		return tx.execute(tx -> {
			ds.update(sql, ps -> {
				ps.setDate(1, new java.sql.Date(System.currentTimeMillis()));
				ps.setString(2, post.url().toExternalForm());
				ps.execute();
			});
			return post;
		});
	}

	@Override
	public URL getFeedUrl() {
		return this.url;
	}

	@Override
	public String composeTweetFor(PromotableBlog promotableBlog) {
		var authorReferenceForTweet = buildAuthorReferenceForTweet(promotableBlog.author());
		return TweetTextComposers.compose(
				String.format("new from %s: %s", authorReferenceForTweet, promotableBlog.blogPost().title()),
				promotableBlog.blogPost().url().toExternalForm());
	}

	@Override
	public BlogPost mapBlogPost(SyndEntry source) {
		var title = source.getTitle();
		var published = publishedDate(source);
		var uri = source.getLink();
		var categories = source.getCategories().stream().map(SyndCategory::getName).map(String::toLowerCase)
				.collect(Collectors.toSet());
		var authors = source.getAuthors().stream().map(SyndPerson::getName).distinct().toList();
		var authorName = authors.size() > 0 ? authors.get(0) : null;
		return new BlogPost(title, UrlUtils.buildUrl(uri), authorName, published, categories);
	}

	@Override
	public Author mapAuthor(BlogPost entry) {
		return new Author(entry.author(), Map.of());
	}

	@Override
	public List<PromotableBlog> getPromotableBlogs() {
		var sql = """
				select b.title, b.url as blog_url , b.author, b.published, b.categories
				from blog_posts b
				where  b.promoted is null and blog_id = ?
				order by b.published desc
				""";
		return this.ds.query(sql, (rs, rowNum) -> {
			var post = new BlogPost(rs.getString("title"), UrlUtils.buildUrl(rs.getString("blog_url")),
					rs.getString("author"), new Date(rs.getDate("published").getTime()).toInstant(),
					typedArrayFromJdbcArray(rs.getArray("categories")));
			var author = mapAuthor(post);
			Assert.notNull(author, "the author must never be null!");
			return new PromotableBlog(post, author);
		}, beanName.get());
	}

	@SneakyThrows
	private static Set<String> typedArrayFromJdbcArray(Array array) {
		var stringArray = (String[]) array.getArray();
		return new HashSet<>(Arrays.asList(stringArray));
	}

	@Override
	public BlogPost record(BlogPost post) {
		var sql = """
				    insert into blog_posts(
				        url,
				        published,
				        title,
				        categories ,
				        author,
				        blog_id
				    )
				    values ( ?, ?, ?, ?, ? ,? )
				    on conflict (url) do update set
				        published=excluded.published ,
				        title=excluded.title,
				        categories=excluded.categories ,
				        author=excluded.author ,
				        blog_id=excluded.blog_id
				""";
		tx.execute(tx -> {
			ds.update(sql, ps -> {
				ps.setString(1, post.url().toString());
				ps.setDate(2, new java.sql.Date(post.published().toEpochMilli()));
				ps.setString(3, post.title());
				ps.setArray(4, ps.getConnection().createArrayOf("text", post.categories().toArray(new String[0])));
				ps.setString(5, post.author());
				ps.setString(6, this.beanName.get());
				ps.execute();
			});
			return null;
		});
		log.debug(post.url() + " " + post.title());
		return post;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName.set(name);
	}

	private static Instant publishedDate(SyndEntry entry) {
		var dates = new Date[] { entry.getUpdatedDate(), entry.getPublishedDate() };
		for (var date : dates)
			if (null != date)
				return date.toInstant();
		return null;
	}

	protected String buildAuthorReferenceForTweet(Author author) {
		Assert.notNull(author, "the author must not be null");
		var txt = new StringBuilder();
		var socialMediaStringMap = author.socialMedia();

		if (StringUtils.hasText(author.name())) {
			txt.append(author.name());
		}

		if (!socialMediaStringMap.isEmpty()) {
			var twitter = socialMediaStringMap.getOrDefault(AuthorSocialMedia.TWITTER, "");
			if (StringUtils.hasText(twitter)) {
				if (!twitter.startsWith("@"))
					twitter = "@" + twitter;
				txt.append(String.format(" (%s)", twitter));
			}
		}
		return txt.toString();
	}

}
