package uk.ac.open.kmi.discou;

import org.apache.http.HttpException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.open.kmi.discou.videofinder.VideofinderInputFactory;

public class VideofinderInputFactoryTest {

	private static Logger logger = LoggerFactory.getLogger(VideofinderInputFactoryTest.class);

	@Rule
	public TestName name = new TestName();

	@Test
	public void videofinder() {
		logger.info("Running {}", name.getMethodName());
		SparqlInputCollectorBuilder ic = (SparqlInputCollectorBuilder) VideofinderInputFactory.get();
		ic.limit(1);
		logger.debug("Testing Query: {}", ic.buildQuery());
		try {
			Assert.assertTrue(ic.build().hasNext());
		} catch (Exception e) {
			if (e instanceof HttpException) {
				HttpException he = (HttpException) e;
				logger.error("HTTP Message: {}", he.getMessage());

			}
			e.printStackTrace();
			Assert.assertTrue(false);
		}
	}

	@Test
	public void prepareContent() {
		logger.info("Running {}", name.getMethodName());
		SparqlInputCollectorBuilder ic = (SparqlInputCollectorBuilder) VideofinderInputFactory.get();
		ic.limit(1);
		logger.debug("Testing Query: {}", ic.buildQuery());
		try {
			DiscouInputCollector dic = ic.build();
			Assert.assertTrue(dic.hasNext());
			DiscouInputResource dir = dic.next();
			logger.info("uri: {}", dir.getUri());
			logger.info("title: {}", dir.getTitle());
			logger.info("description: {}", dir.getDescription());
			logger.info("content: {}", dir.getContent());
		} catch (Exception e) {
			if (e instanceof HttpException) {
				HttpException he = (HttpException) e;
				logger.error("HTTP Message: {}", he.getMessage());

			}
			e.printStackTrace();
			Assert.assertTrue(false);
		}
	}
}
