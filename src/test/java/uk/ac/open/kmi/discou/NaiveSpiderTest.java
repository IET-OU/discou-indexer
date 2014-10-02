package uk.ac.open.kmi.discou;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.open.kmi.discou.sources.NaiveSpider;

public class NaiveSpiderTest {
	private Logger log = LoggerFactory.getLogger(NaiveSpiderTest.class);

	@Test
	public void test() {
		final String ol = "http://www.open.edu/openlearn/history-the-arts/culture/literature-and-creative-writing/literature/christopher-marlowe-doctor-faustus/content-section-0";

		final StringBuilder content = new StringBuilder();
		NaiveSpider ns = new NaiveSpider(ol) {

			@Override
			public void process(String html) {
				// do nothing
				Pattern p = Pattern.compile("(?i)<div\\s+class=\"oucontent-content\">(.+)?</div>");
				Matcher m = p.matcher(html);
				if(m.find()){
					content.append(System.getProperty("line.separator")).append(m.group(1).replaceAll("<script\\s.*?/script>", "").replaceAll("\\<.+?\\>", " "));
				}
			}

			@Override
			public boolean follow(String link) {
				URL u;
				try {
					u = new URL(new URL(ol), link);
					if (u.toString().startsWith("http://www.open.edu/openlearn/history-the-arts/culture/literature-and-creative-writing/literature/christopher-marlowe-doctor-faustus/")) {
						return true;
					} else {
						return false;
					}
				} catch (MalformedURLException e) {
					// not a valid url (javascript?)
				}
				return false;
			}
		};
		ns.start();
		log.info("Content is: \n\n{}\n\n", content.toString());
	}
}
