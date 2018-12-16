package uk.ac.open.kmi.discou;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.open.kmi.discou.sources.OpenLearnExploreScraper;
import uk.ac.open.kmi.discou.sources.OpenLearnUnitScraper;

//@Ignore("requires http connection")
public class OpenLearnUnitIndexerTest {

	public static final Logger logger = LoggerFactory.getLogger(OpenLearnUnitIndexerTest.class);
	private static URL testdir = OpenLearnUnitIndexerTest.class.getClassLoader().getResource(".");
	private static File index;
	private static File dir;

	@Rule
	public TestName name = new TestName();

	@BeforeClass
	public static void beforeClass() throws URISyntaxException {
		logger.info("[start] ");
		dir = new File(testdir.toURI());
		dir.mkdirs();
		logger.info("[init] Test dir is: {}", dir.getAbsolutePath());
	}

	@AfterClass
	public static void afterClass() {
		logger.info("[end] ");
	}

	private static boolean deleteDir(File dir) {
		// logger.info(" deleting {}", dir);
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}

	@Before
	public void before() {
		String indexName = "DP_indexes";
		index = new File(dir, indexName);
		index.mkdirs();
		index.deleteOnExit();
	}

	@After
	public void after() {
		logger.info("cleanup test dir: {}", deleteDir(index));
	}

	//@Ignore("requires http connection")
	@Test
	public void singleUnit() {
		logger.info("start {}", name.getMethodName());

		try {
			DiscouIndexer i = new DiscouIndexer(index, "http://anne.kmi.open.ac.uk/rest/annotate");
			Assert.assertTrue(index.exists());

			i.open();
			i.put(new DiscouInputResource() {

				@Override
				public String getUri() {
					return "http://data.open.ac.uk/openlearn/ad281_1";
				}

				@Override
				public String getTitle() {
					return "What is heritage?";
				}

				@Override
				public String getDescription() {
					return "<p>This unit introduces the concept of heritage and examines its various uses in contemporary society. It then provides a background to the development of critical heritage studies as an area of academic interest, and in particular the way in which heritage studies has developed in response to various critiques of contemporary politics and culture in the context of deindustrialisation, globalisation and transnationalism. Drawing on a case study in the official documentation surrounding the Harry S. Truman Historic Site in Missouri, USA, it describes the concept of authorised heritage discourses (AHD) in so far as they are seen to operate in official, state-sanctioned heritage initiatives.</p><p>This unit is an adapted extract from the course <span class=\"oucontent-linkwithtip\"><a class=\"oucontent-hyperlink\" href=\"http://www3.open.ac.uk/study/undergraduate/course/ad281.htm\"><i>Understanding global heritage</i> (AD281)</a></span>. </p>";
				}

				@Override
				public String getContent() {
					OpenLearnUnitScraper sc = new OpenLearnUnitScraper(
							"http://www.open.edu/openlearn/history-the-arts/culture/literature-and-creative-writing/literature/christopher-marlowe-doctor-faustus/content-section-0");
					sc.start();
					String t = sc.getText();
					logger.info(" TEXT IS :: {}", t);
					return t;
				}
			});
			i.close();

			DiscouReader reader = new DiscouReader(index);
			reader.open();
			DiscouResource res = reader.getFromURI("http://data.open.ac.uk/openlearn/ad281_1");
			logger.info(" URI: {}\n TITLE: {}\n DESC: {}\n CONTENT: {}", new Object[] { res.getUri(), res.getTitle(), res.getDescription(), res.getContent() });
			reader.close();
		} catch (IOException e) {
			logger.error("", e);
			Assert.assertTrue(false);
		}
		logger.info("end {}", name.getMethodName());
	}

	@Test
	public void singleExplore() {
		logger.info("start {}", name.getMethodName());

		try {
			DiscouIndexer i = new DiscouIndexer(index);
			Assert.assertTrue(index.exists());

			i.open();
			i.put(new DiscouInputResource() {

				public String getUri() {
					return "http://data.open.ac.uk/openlearn/body-mind/question-time-healthcare-services";
				}

				public String getTitle() {
					return "Question time for healthcare services?";
				}

				public String getDescription() {
					return "Former midwife Pam Foley asks questions about the effects a changing society has on the quality and provision of healthcare<link rel=\"canonical\" href=\"http://www.open.edu/openlearn/body-mind/question-time-healthcare-services\" /> Dr Pam Foley. Pam Foley is a Senior Lecturer at The Open University&rsquo;s Faculty of Health and Social Care, writing and teaching undergraduate courses for students and practitioners working with children, young people and families. Her research interests focus on social policy affecting children and young people and practice that recognises children&rsquo;s agency.<br />First published on Mon, 20 Feb 2012 as <a href=\"http://www.open.edu/openlearn/body-mind/question-time-healthcare-services\">Question time for healthcare services?</a>. To find out more visit The Open University's <a href=\"http://www.open.edu/openlearn/ole-home-page\">Openlearn</a> website. Copyright 2012";
				}

				public String getContent() {
					OpenLearnExploreScraper sc = new OpenLearnExploreScraper("http://www.open.edu/openlearn/body-mind/question-time-healthcare-services");
					sc.start();
					String t = sc.getText();
					logger.info(" TEXT IS :: {}", t);
					return t;
				}
			});
			i.close();

			DiscouReader reader = new DiscouReader(index);
			reader.open();
			DiscouResource res = reader.getFromURI("http://data.open.ac.uk/openlearn/body-mind/question-time-healthcare-services");
			logger.info(" URI: {}\n TITLE: {}\n DESC: {}\n CONTENT: {}", new Object[] { res.getUri(), res.getTitle(), res.getDescription(), res.getContent() });
			reader.close();
			Assert.assertTrue(res.getUri().equals("http://data.open.ac.uk/openlearn/body-mind/question-time-healthcare-services"));
		} catch (IOException e) {
			logger.error("", e);
			Assert.assertTrue(false);
		}
		logger.info("end {}", name.getMethodName());
	}
}
