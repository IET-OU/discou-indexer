package uk.ac.open.kmi.discou;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore("requires http connection")
public class SparqlInputCollectionBuilderTest {

	private static Logger logger = LoggerFactory.getLogger(SparqlInputCollectionBuilderTest.class);

	@Rule 
	public TestName name = new TestName();
	
	@Test
	public void test() {
		logger.info("[start] {}", name.getMethodName());
		SparqlInputCollectorBuilder icb = new SparqlInputCollectorBuilder().endpoint("http://data.open.ac.uk/sparql").from("http://data.open.ac.uk/context/openlearnexplore")
				.title("http://purl.org/dc/terms/title").description("http://purl.org/dc/terms/description").content("http://dbpedia.org/property/url").limit(100);

		logger.info("Query: {}", icb.buildQuery());
		DiscouInputCollector ic = icb.build();
		while (ic.hasNext()) {
			DiscouResource r = ic.next();
			logger.info("{} - {} {} {}", new Object[] { r.getUri(), r.getTitle(), r.getDescription(), r.getContent() });
			Assert.assertNotNull(r.getUri());
			Assert.assertNotNull(r.getTitle());
			Assert.assertNotNull(r.getDescription());
			Assert.assertNotNull(r.getContent());
		}
		logger.info("[end] {}", name.getMethodName());
	}

	@Test
	public void testMissingEndpoint() {
		logger.info("[start] {}", name.getMethodName());
		SparqlInputCollectorBuilder icb = new SparqlInputCollectorBuilder();
		// this should throw an exception
		Exception ex = null;
		try {
			icb.build();
		} catch (Exception e) {
			logger.error("***EXPECTED EXCEPTION***", e);
			ex = e;
		} finally {
			Assert.assertNotNull(ex);
		}
		logger.info("[end] {}", name.getMethodName());
	}

	@Test
	public void testUrisOnly(){
		logger.info("[start] {}", name.getMethodName());
		SparqlInputCollectorBuilder icb = new SparqlInputCollectorBuilder();
		icb.endpoint("http://data.open.ac.uk/sparql");
		icb.type("http://data.open.ac.uk/openlearn/ontology/OpenLearnArticle");
		logger.info("Query: {}", icb.buildQuery());
		DiscouInputCollector ic = icb.build();
		int c = 0;
		while(ic.hasNext()){
			c++;
			DiscouInputResource dir = ic.next();
			Assert.assertNotNull(dir.getUri());
			Assert.assertEquals(dir.getTitle(), "");
			Assert.assertEquals(dir.getContent(), "");
			Assert.assertEquals(dir.getDescription(), "");
		}
		logger.info("{} URIs found.", c);
		logger.info("[end] {}", name.getMethodName());
	}
	
	@Test
	public void testPrepare(){
		logger.info("[start] {}", name.getMethodName());
		SparqlInputCollectorBuilder icb = new SparqlInputCollectorBuilder(){
			@Override
			protected String prepareTitle(String t) {
				return "prepared title";
			}
			@Override
			protected String prepareDescription(String d) {
				return "prepared description";
			}
			@Override
			protected String prepareContent(String c) {
				return "prepared content";
			}
		};
		icb.endpoint("http://data.open.ac.uk/sparql");
		icb.type("http://data.open.ac.uk/openlearn/ontology/OpenLearnArticle");
		logger.info("Query: {}", icb.buildQuery());
		DiscouInputCollector ic = icb.build();
		int c = 0;
		while(ic.hasNext()){
			c++;
			DiscouInputResource dir = ic.next();
			Assert.assertNotNull(dir.getUri());
			Assert.assertEquals(dir.getTitle(), "prepared title");
			Assert.assertEquals(dir.getContent(), "prepared content");
			Assert.assertEquals(dir.getDescription(), "prepared description");
		}
		logger.info("{} URIs found.", c);
		logger.info("[end] {}", name.getMethodName());
	}
}
