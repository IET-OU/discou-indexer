package uk.ac.open.kmi.discou;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.open.kmi.discou.sources.OpenLearnExploreScraper;

public class OpenLearnExploreScraperTest {
	public static final Logger logger = LoggerFactory.getLogger(OpenLearnExploreScraperTest.class);

	@Ignore
	@Test
	public void test() {
		OpenLearnExploreScraper sc = new OpenLearnExploreScraper("http://www.open.edu/openlearn/body-mind/question-time-healthcare-services");
		sc.start();
		String t = sc.getText();
		logger.info(" TEXT IS :: {}", t);
	}
}
