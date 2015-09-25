package uk.ac.open.kmi.discou;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscouReaderTest {
	private static Logger logger = LoggerFactory.getLogger(DiscouReaderTest.class);
	private static URL testdir = DiscouIndexerTest.class.getClassLoader().getResource(".");
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

	private boolean deleteDir(File dir) {
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

	@Test
	public void search() throws IOException {
		// Put some data in
		DiscouIndexer indexer = new DiscouIndexer(index);
		indexer.open();
		indexer.putRaw("http://data.open.ac.uk/item1", "titolo titolo titolo titolo titolo titolo titolo house house house", "titolo titolo titolo titolo titolo titolo titolo house house house", "titolo titolo titolo titolo titolo titolo titolo house house house titolo titolo titolo titolo titolo titolo titolo house house house titolo titolo titolo titolo titolo titolo titolo house house house titolo titolo titolo titolo titolo titolo titolo house house house titolo titolo titolo titolo titolo titolo titolo house house house");
		indexer.putRaw("http://data.open.ac.uk/item2", "regular regular regular titolo titolo titolo titolo house house house", "tat regular regular titolo titolo titolo titolo house house house", "tat regular regular titolo titolo titolo titolo house house house regular regular regular titolo titolo titolo titolo house house house regular regular regular titolo titolo titolo titolo house house house regular regular regular titolo titolo titolo titolo house house house");
		indexer.putRaw("http://data.open.ac.uk/item3", "house house house house house house house house house house", "house house house house house house house house house house", "house house house house house house house house house house house house house house house house house house house house house house house house house house house house house house house house house house house house house house house house");
		indexer.putRaw("http://data.open.ac.uk/item4", "celine celine celine celine celine celine celine xxx xxx xxx", "celine celine celine celine celine celine celine xxx xxx xxx", "celine celine celine celine celine celine celine xxx xxx xxx celine celine celine celine celine celine celine xxx xxx xxx celine celine celine celine celine celine celine xxx xxx xxx celine celine celine celine celine celine celine xxx xxx xxx");
		indexer.commit();
		indexer.close();
		// Search

		DiscouReader reader = new DiscouReader(index);
		reader.open();
		logger.info("items {}", reader.count());
		Assert.assertTrue(reader.count() == 4);
		logger.info("item1 {}", reader.getFromURI("http://data.open.ac.uk/item1").getUri());

		Map<String, Float> results = reader.similar("http://data.open.ac.uk/item1", 10);
		reader.close();
		logger.info("{} results", results.size());
		
		// Lookup results
		for (Entry<String, Float> r : results.entrySet()) {
			logger.info("{} : {}", r.getKey(), r.getValue());
		}
		
		Assert.assertTrue(results.size() == 3);
	}
}
