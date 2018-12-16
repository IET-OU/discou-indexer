package uk.ac.open.kmi.discou;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

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

@Ignore("requires http connection")
public class SpotlightGETandPOSTTest {
	private static Logger logger = LoggerFactory.getLogger(SpotlightGETandPOSTTest.class);
	static File testdir;
	static File index;

	@Rule
	public TestName name = new TestName();

	@Ignore
	@Test
	public void test() throws IOException {
		logger.info("start {}", name.getMethodName());

		Query query = QueryFactory.create("select distinct ?x ?t ?d ?u from <http://data.open.ac.uk/context/openlearn> where {"
				+ "?x <http://purl.org/dc/terms/title> ?t ; <http://purl.org/dc/terms/description> ?d ; <http://purl.org/dc/terms/description> ?u" + "} limit 10");
		QueryExecution qe = QueryExecutionFactory.sparqlService("http://data.open.ac.uk/sparql", query);
		ResultSet rs = qe.execSelect();
//		String url = "http://kmi-dev04.open.ac.uk:6081/rest/annotate";
		String url = "http://spotlight.dbpedia.org/rest/annotate";
		while (rs.hasNext()) {
			QuerySolution qs = rs.next();
			final String uri = qs.getResource("x").getURI();
			final String content = qs.getLiteral("u").getString();
			logger.info("\n\turi: {}\n\tcontent: {}", new Object[] { uri, content });
			String querystring = "text=" + URLEncoder.encode(content, "UTF-8") + "&confidence=0.2&support=0";
			HttpURLConnection urlConn = (HttpURLConnection) new URL(url).openConnection();
			// DO POST
			// Let the run-time system (RTS) know that we want input.
			urlConn.setDoInput(true);
			// Let the RTS know that we want to do output.
			urlConn.setDoOutput(true);
			// No caching, we want the real thing.
			urlConn.setUseCaches(false);
			// Request method
			urlConn.setRequestMethod("POST");
			// Specify the content type.
			urlConn.setRequestProperty("Accept", "text/xml");

			// Send POST output.
			DataOutputStream printout = new DataOutputStream(urlConn.getOutputStream());

			printout.writeBytes(querystring);
			printout.flush();
			printout.close();

			String fromPOST = readOutput(urlConn.getInputStream());

			// NOW DO GET
			urlConn = (HttpURLConnection) new URL(url + '?' + querystring).openConnection();
			urlConn.setRequestProperty("Accept", "text/xml");
			String fromGET = readOutput(urlConn.getInputStream());

			logger.info("EQUALS? {}", (fromPOST.equals(fromGET)));
		}

		qe.close();

		logger.info("end {}", name.getMethodName());
	}

	private String readOutput(InputStream is) throws IOException {

		String line;
		String test = "";
		boolean httpHeader = true;
		StringBuilder result = new StringBuilder();
		BufferedReader input = new BufferedReader(new InputStreamReader(is));

		while ((line = input.readLine()) != null) {
			if (httpHeader) {
				if (line.length() > 5) {
					test = line.substring(0, 5);
				}
				if (test.equals("<?xml")) {
					httpHeader = false;
					result.append(line);
				}
			} else {
				result.append(line);
			}
		}

		input.close();
		is.close();
		return result.toString();
	}
}
