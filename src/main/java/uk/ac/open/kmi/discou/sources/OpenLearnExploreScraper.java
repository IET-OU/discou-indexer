package uk.ac.open.kmi.discou.sources;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenLearnExploreScraper extends NaiveSpider {
	private final Logger log = LoggerFactory.getLogger(OpenLearnExploreScraper.class);
	private String unit = null;
	private String unitPath;
	private StringBuilder contentBuilder;
	private String content = null;
	private int maxTextLength = -1;

	public OpenLearnExploreScraper(String unit) {
		super(unit);
		this.unit = unit;
		this.unitPath = unit;
	}

	public void setMaxTextLength(int length) {
		maxTextLength = length;
	}

	public int getMaxTextLength() {
		return maxTextLength;
	}

	@Override
	public void process(String html) {
		// do nothing
		Pattern p = Pattern.compile("(?i)<p>(.+)</p>");
		Matcher m = p.matcher(html);
		while (m.find()) {
			log.info("Found content: {}",m.group(1));
			contentBuilder.append(System.getProperty("line.separator")).append(m.group(1).replaceAll("<script\\s.*?/script>", "").replaceAll("\\<.+?\\>", " ").replaceAll("\\s+", " "));
		}
	}

	@Override
	public boolean follow(String link) {
		if (maxTextLength != -1 && contentBuilder.length() > maxTextLength){
			return false;
		}
		URL u;
		try {
			u = new URL(new URL(unit), link);
			//log.info("link {} starts with {}", link, unitPath);
			if (u.toString().startsWith(unitPath)) {
				return true;
			} else {
				return false;
			}
		} catch (MalformedURLException e) {
			// not a valid url (javascript?)
		}
		return false;
	}

	@Override
	public void start() {
		this.contentBuilder = new StringBuilder();
		super.start();
		this.content = contentBuilder.toString();
		contentBuilder = null;
	}

	public String getText() {
		return content;
	}
}
