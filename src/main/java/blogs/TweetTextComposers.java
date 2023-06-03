package blogs;

abstract class TweetTextComposers {

	public static final int MAX_TWEET_LENGTH = 280;

	static String compose(String title, String url) {
		var ellipse = "...";
		var full = buildFullTweetText(title, url);
		if (full.length() <= MAX_TWEET_LENGTH)
			return full;
		var delta = full.length() - MAX_TWEET_LENGTH;
		var desiredWidth = title.length() - delta;
		return buildFullTweetText(rTrimToSpace(title, desiredWidth - ellipse.length()) + ellipse, url);
	}

	private static String buildFullTweetText(String title, String url) {
		return String.format("%s %s", title, url);
	}

	private static String rTrimToSpace(String text, int desired) {
		while (text.length() >= desired) {
			var lastIndexOf = text.lastIndexOf(' ');
			if (lastIndexOf != -1)
				text = text.substring(0, lastIndexOf);
		}
		return text;
	}

}
