package uk.ac.open.kmi.discou;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.open.kmi.discou.dataopen.DataOpenInputFactory;

public class DataOpenInputFactoryTest {
	private static Logger logger = LoggerFactory.getLogger(DataOpenInputFactoryTest.class);

	@Rule
	public TestName name = new TestName();

	@Test
	public void openlearn() {
		logger.info("Running {}", name.getMethodName());
		SparqlInputCollectorBuilder ic = (SparqlInputCollectorBuilder) DataOpenInputFactory.openlearn();
		ic.limit(1);
		logger.debug("Testing Query: {}", ic.buildQuery());
		Assert.assertTrue(ic.build().hasNext());
	}
}
