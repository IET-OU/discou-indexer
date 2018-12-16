package uk.ac.open.kmi.discou.sources;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenLearnUnitScraper extends NaiveSpider {
	private String unit = null;
	private String unitPath;
	private StringBuilder contentBuilder;
	private String content = null;
	private int maxTextLength = -1;
	private final Logger log = LoggerFactory.getLogger(OpenLearnUnitScraper.class);
	public OpenLearnUnitScraper(String unit) {
		super(unit);
		this.unit = unit;
		this.unitPath = unit.substring(0, unit.lastIndexOf('/'));
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
		Pattern p = Pattern.compile("(?i)<div\\s+class=\"oucontent-content\">(.+)?</div>");
		Matcher m = p.matcher(html);
		if (m.find()) {
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
			if (u.toString().startsWith(unitPath)) {
				return true;
			} else {
				return false;
			}
		} catch (MalformedURLException e) {
			// not a valid url (javascript?)
			log.debug("Not a valid URL? <{}>", link);
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
