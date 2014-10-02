package uk.ac.open.kmi.discou;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import uk.ac.open.kmi.discou.spotlight.SpotlightAnnotation;
import uk.ac.open.kmi.discou.utils.MD5Generator;
import uk.ac.open.kmi.discou.utils.zipUtil;

public class DiscouIndexer {

	public final static String _SpotlightAnnotationIndexPath = "DBpediaSpotlightAnnotationsIndex";
	public final static String _dbpediaURIindexPath = "DBpediaURIRDFDescriptionIndex";
	public final static String _SpotLightedWebResourceIndexPath = "SpotLightedWebResourceIndex";
	private Logger logger = LoggerFactory.getLogger(DiscouIndexer.class);

	private IndexWriter resourcesIW = null;
	private IndexWriter urisIW = null;
	private IndexWriter annotationsIW = null;
	private File resourceIndex;
	private File urisIndex;
	private File annotationsIndex; // local cache of entities extracted from
									// text
	private String dbpediaSoptlightServiceUrl = "http://spotlight.dbpedia.org/rest/annotate"; // default
																								// location

	public DiscouIndexer(File indexHome) {
		this.resourceIndex = new File(indexHome, _SpotLightedWebResourceIndexPath);
		this.urisIndex = new File(indexHome, _dbpediaURIindexPath);
		this.annotationsIndex = new File(indexHome, _SpotlightAnnotationIndexPath);
	}

	// public DiscouIndexer(File indexHome) {
	// this.resourceIndex = new File(indexHome,
	// _SpotLightedWebResourceIndexPath);
	// this.urisIndex = new File(indexHome, _dbpediaURIindexPath);
	// this.annotationsIndex = new File(indexHome,
	// _SpotlightAnnotationIndexPath);
	// }

	protected IndexWriter open(File location) {
		Analyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_35);
		Directory directory;
		try {
			directory = FSDirectory.open(location);
			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_35, analyzer);
			config.setOpenMode(OpenMode.CREATE_OR_APPEND);
			return new IndexWriter(directory, config);
		} catch (IOException e) {
			logger.error("Cannot initialize or open index location {}", location);
			throw new RuntimeException(e);
		}
	}

	public void commit() throws IOException {
		resourcesIW.commit();
		urisIW.commit();
		annotationsIW.commit();
	}

	public void open() {
		resourcesIW = open(resourceIndex);
		urisIW = open(urisIndex);
		annotationsIW = open(annotationsIndex);
	}

	public void close() throws IOException {
		if (resourcesIW != null) {
			resourcesIW.close();
			resourcesIW = null;
		}
		if (urisIW != null) {
			urisIW.close();
			urisIW = null;
		}
		if (annotationsIW != null) {
			annotationsIW.close();
			annotationsIW = null;
		}
	}

	public void put(DiscouInputResource resource) throws IOException {

		String uri = resource.getUri();
		logger.info("Put {}", uri);
		// extract entities from title
		String title = extractEntitiesFieldValue(resource.getTitle());
		// extract entities from description
		String description = extractEntitiesFieldValue(resource.getDescription());
		// extract entities from content
		String content = extractEntitiesFieldValue(resource.getContent());
		//
		Document doc = new Document();
		logger.trace("uri: {}", uri);
		logger.trace("title: {}", title);
		logger.trace("description", description);
		logger.trace("content: {}", content);
		doc.add(new Field("SpotLightedWebResourceURI", uri, Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field("SpotLightedWebResourceText", title, Field.Store.YES, Field.Index.ANALYZED, TermVector.YES));
		doc.add(new Field("SpotLightedWebResourceDescription", description, Field.Store.YES, Field.Index.ANALYZED, TermVector.YES));
		doc.add(new Field("SpotLightedWebResourceContent", content, Field.Store.YES, Field.Index.ANALYZED, TermVector.YES));
		resourcesIW.addDocument(doc);
	}

	public void put(DiscouInputCollector collector) throws IOException {
		logger.info("Putting {}", collector);
		while (collector.hasNext()) {
			put(collector.next());
		}
	}

	private String extractEntitiesFieldValue(String text) {

		if(text.trim().equals("")){
			logger.warn("Text is empty. We do not attempt to extract entities, returning empty string.");
			return "";
		}
		List<SpotlightAnnotation> annotations = getAnnotations(text);

		StringBuilder scoredTextBuilder = new StringBuilder();
		// Eliminate the Spotlight annotations which secondRankPct >= 0.5
		List<SpotlightAnnotation> slAnnsMinorZeroPointFive = new ArrayList<SpotlightAnnotation>();
		for (SpotlightAnnotation sa : annotations) {
			if (sa.getSecondRankPct() < 0.5) {
				slAnnsMinorZeroPointFive.add(sa);
			}
		}
		// Score each Spotlight Annotation left. Here the score =
		// Integer(similarity x 100)
		int score;
		for (SpotlightAnnotation sa : slAnnsMinorZeroPointFive) {
			double sim = sa.getSimilarity();
			String dbpediaEntityUri = sa.getUri();
			score = (int) (sim * 100.);
			for (int i = 0; i < score - 1; i++) {
				scoredTextBuilder.append(" ").append(dbpediaEntityUri);
			}
		}

		return scoredTextBuilder.toString();
	}

	protected List<SpotlightAnnotation> xmlTextToSpotlightAnnotationList(String xml) {
		logger.debug("reading {}", xml);
		List<SpotlightAnnotation> annotations;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			org.w3c.dom.Document dom = db.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
			NodeList nl = dom.getElementsByTagName("Resource");
			annotations = new ArrayList<SpotlightAnnotation>();
			for (int i = 0; i < nl.getLength(); ++i) {
				Node n = nl.item(i);
				// XXX Where to get the confidence parameter
				SpotlightAnnotation annotation = new SpotlightAnnotation(n, 0.2);
				annotations.add(annotation);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return annotations;
	}

	private List<SpotlightAnnotation> executeNER(String text) {
		logger.trace("executeNER {}", text);
		double confidence = 0.2;
		int support = 0;
		String result = "";
		try {

			// URL connection channel.
			HttpURLConnection urlConn;
			String querystring = "text=" + URLEncoder.encode(text, "UTF-8") + "&confidence=" + confidence + "&support=" + support;
			// 8192 bytes as max URL length
			boolean doPost = false;

			if (getDBPediaSpotlightServiceURL().getBytes().length + querystring.getBytes().length > 8192) {
				doPost = true;
			}

			if (true) {
				urlConn = (HttpURLConnection) new URL(getDBPediaSpotlightServiceURL()).openConnection();

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
			}
			// else {
			// urlConn = (HttpURLConnection) new
			// URL(getDBPediaSpotlightServiceURL() + '?' + querystring
			// ).openConnection();
			// urlConn.setRequestProperty("Accept", "text/xml");
			// }
			long sss = System.currentTimeMillis();
			// Get response data.
			BufferedReader input = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
			logger.info("spotlight input stream obtained in {}ms", (System.currentTimeMillis() - sss));
			String line;
			String test = "";
			boolean httpHeader = true;
			while ((line = input.readLine()) != null) {
				if (httpHeader) {
					if (line.length() > 5) {
						test = line.substring(0, 5);
					}
					if (test.equals("<?xml")) {
						httpHeader = false;
						result = result + line;
					}
				} else {
					result = result + line;
				}
			}

			input.close();

			// cache result

			String textId;
			try {
				textId = MD5Generator.getMD5(text);
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			Document doc = new Document();
			// Add code to compress the resultTextXml string
			zipUtil zU = new zipUtil();
			// encode the string.
			String encoded;
			try {
				encoded = zU.gzipStringEncode(result);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			doc.add(new Field("TextXml", encoded, Field.Store.YES, Field.Index.NOT_ANALYZED));
			doc.add(new Field("MD5", textId, Field.Store.YES, Field.Index.NOT_ANALYZED));
			annotationsIW.addDocument(doc);
			// force commit
			annotationsIW.commit();
			// force close and reopen
			annotationsIW.close();
			annotationsIW = open(annotationsIndex);
			logger.debug("Cached annotations (xml text) {}", textId);
		} catch (IOException e) {
			logger.error("Failed executeNER", e);
			return Collections.emptyList();
		}

		return xmlTextToSpotlightAnnotationList(result);
	}

	public String getDBPediaSpotlightServiceURL() {
		// TODO Auto-generated method stub
		return dbpediaSoptlightServiceUrl;
	}

	public void setDBPediaSpotlightServiceURL(String url) {
		dbpediaSoptlightServiceUrl = url;
	}

	protected List<SpotlightAnnotation> getAnnotations(String text) {
		List<SpotlightAnnotation> annotations = null;
		// entities extracted from text are cached in index annotationsIndex
		String textId;
		try {
			textId = MD5Generator.getMD5(text);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		// check if the text is in the annotations' cache
		IndexReader indexReader = null;
		IndexSearcher indexSearcher = null;
		try {
			Directory directory = FSDirectory.open(annotationsIndex);
			// logger.debug("Opening {}", directory);
			indexReader = IndexReader.open(directory);
			indexSearcher = new IndexSearcher(indexReader);
			logger.debug("seeking {} in cache", textId);
			BooleanQuery query = new BooleanQuery();
			query.add(new TermQuery(new Term("MD5", textId)), BooleanClause.Occur.MUST);
			TopDocs topDocs = indexSearcher.search(query, 1);
			// logger.debug("hits {}", topDocs.totalHits);
			if (topDocs.totalHits > 0) {
				Document d = indexSearcher.doc(topDocs.scoreDocs[0].doc);
				String TextXml = d.get("TextXml");
				zipUtil zU = new zipUtil();
				String decoded = zU.gzipStringDecode(TextXml);
				annotations = xmlTextToSpotlightAnnotationList(decoded);
			}
		} catch (IndexNotFoundException e) {
			logger.warn("Index does not exists :(");
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (Exception e1) {
			// FIXME This is too bad and is a consequence of the toooo bad
			// zipUtils()
			throw new RuntimeException(e1);
		} finally {
			if (indexSearcher != null) {
				try {
					//indexSearcher.close();
				} catch (Exception e) {
				}
			}
			;
			if (indexReader != null) {
				try {
					indexReader.close();
				} catch (Exception e) {
				}
			}
			;
		}

		// if annotations is null, the item is not in the cache
		if (annotations != null) {
			logger.debug("Read annotations from cached index");
			return annotations;
		} else {
			try {
				logger.debug("Extract annotations from remote system");
				return executeNER(text);
			} catch (Exception e) {
				logger.error("ERROR", e);
				return Collections.emptyList();
			}
		}
	}

}
