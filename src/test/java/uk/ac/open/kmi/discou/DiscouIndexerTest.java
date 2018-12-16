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

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

public class DiscouIndexerTest {
	private static Logger logger = LoggerFactory.getLogger(DiscouIndexerTest.class);
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

	@Test
	public void test() {
		logger.info("start {}", name.getMethodName());
		try {
			DiscouIndexer i = new DiscouIndexer(index);
			Assert.assertTrue(index.exists());
			i.open();
			i.close();
			// No exception here
			Assert.assertTrue(true);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		logger.info("end {}", name.getMethodName());
	}

	@Test
	public void testReopen() {
		logger.info("start {}", name.getMethodName());
		try {
			DiscouIndexer i = new DiscouIndexer(index);
			i.open();
			i.close();
			i.open(); // reopen
			// No exception here
			Assert.assertTrue(true);
			i.close();

			i.open();
			Exception e = null;
			try {
				i.open();
			} catch (Exception ee) {
				e = ee;
			}
			Assert.assertNotNull(e);
			i.close();
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}

		logger.info("end {}", name.getMethodName());
	}

	@Ignore("requires http connection")
	@Test
	public void testOneResource() throws IOException {
		logger.info("start {}", name.getMethodName());

		// String uri = "http://data.open.ac.uk/openlearn/aa100_1";
		String uri = "http://data.open.ac.uk/openlearn/d867_1";

		logger.info("indexing {}", uri);
		Query query = QueryFactory.create("select distinct ?t ?d ?u where {" + "<" + uri + "> <http://purl.org/dc/terms/title> ?t. " + "<" + uri + "> <http://purl.org/dc/terms/description> ?d." + "<"
				+ uri + "> <http://purl.org/dc/terms/description> ?u" + "}");
		logger.info("Quesy: {}", query);
		QueryExecution qe = QueryExecutionFactory.sparqlService("http://data.open.ac.uk/sparql", query);
		ResultSet rs = qe.execSelect();
		DiscouInputResource ir = null;
		Assert.assertTrue(rs.hasNext());
		QuerySolution qs = rs.next();
		final String title = qs.getLiteral("t").getString();
		final String description = qs.getLiteral("d").getString();
		final String content = qs.getLiteral("u").getString();
		logger.info("\n\turi: {}\n\ttitle: {}\n\tdescription: {}\n\tcontent: {}", new Object[] { uri, title, description, content });
		ir = new DiscouInputResourceImpl(uri, title, description, content);
		qe.close();
		Assert.assertNotNull(ir);
		long start = System.currentTimeMillis();
		DiscouIndexer i = new DiscouIndexer(index);
		// i.setDBPediaSpotlightServiceURL("http://kmi-dev04.open.ac.uk:6081/rest/annotate");
		i.open();
		i.put(ir);
		i.commit();
		i.close();
		logger.info("written in {} ms", (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		// test read now
		DiscouReader reader = new DiscouReader(index);
		reader.open();
		DiscouResource ires = reader.getFromURI(uri);
		// Assert.assertEquals("Read URI is the same as written", uri,
		// ires.getUri());
		// Assert.assertFalse("Description contains some entities",
		// "".equals(ires.getDescription()));
		// Assert.assertFalse("Titlecontains some entities",
		// "".equals(ires.getTitle()));
		Assert.assertFalse("Content contains some entities", "".equals(ires.getContent()));
		logger.info("Indexed content: {}", ires.getContent());
		Assert.assertTrue(ires.getUri().equals(uri));
		reader.close();
		logger.info("read in {} ms", (System.currentTimeMillis() - start));
		logger.info("end {}", name.getMethodName());
	}

	@Ignore("requires http connection")
	@Test
	public void testMultiResource() throws IOException {

		logger.info("start {}", name.getMethodName());

		Query query = QueryFactory.create("select distinct ?x ?t ?d ?u from <http://data.open.ac.uk/context/openlearn> where {"
				+ "?x <http://purl.org/dc/terms/title> ?t ; <http://purl.org/dc/terms/description> ?d ; <http://purl.org/dc/terms/description> ?u" + "} limit 2");
		QueryExecution qe = QueryExecutionFactory.sparqlService("http://data.open.ac.uk/sparql", query);
		ResultSet rs = qe.execSelect();
		DiscouInputResource ir = null;
		DiscouIndexer i = new DiscouIndexer(index);
		//i.setDBPediaSpotlightServiceURL("http://kmi-dev04.open.ac.uk:6081/rest/annotate");
		i.open();
		while (rs.hasNext()) {
			QuerySolution qs = rs.next();
			final String uri = qs.getResource("x").getURI();
			final String title = qs.getLiteral("t").getString();
			final String description = qs.getLiteral("d").getString();
			final String content = qs.getLiteral("u").getString();
			logger.info("\n\turi: {}\n\ttitle: {}\n\tdescription: {}\n\tcontent: {}", new Object[] { uri, title, description, content });
			ir = new DiscouInputResourceImpl(uri, title, description, content);
			Assert.assertNotNull(ir);
			i.put(ir);
		}
		i.commit();
		i.close();

		qe.close();

		logger.info("end {}", name.getMethodName());
	}

}
