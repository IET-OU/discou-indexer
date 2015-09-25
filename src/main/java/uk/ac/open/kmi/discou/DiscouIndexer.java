package uk.ac.open.kmi.discou;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

import uk.ac.open.kmi.discou.spotlight.SpotlightAnnotation;
import uk.ac.open.kmi.discou.spotlight.SpotlightClient;
import uk.ac.open.kmi.discou.spotlight.SpotlightResponse;
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
	private String dbpediaSoptlightServiceUrl = null; // default
														// location

	public DiscouIndexer(File indexHome) {
		this(indexHome, "http://spotlight.dbpedia.org/rest/annotate");
	}

	public DiscouIndexer(File indexHome, String dbpediaSpotlightEndpoint) {
		this.resourceIndex = new File(indexHome, _SpotLightedWebResourceIndexPath);
		this.urisIndex = new File(indexHome, _dbpediaURIindexPath);
		this.annotationsIndex = new File(indexHome, _SpotlightAnnotationIndexPath);
		this.dbpediaSoptlightServiceUrl = dbpediaSpotlightEndpoint;
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
		put(resource.getUri(), resource.getTitle(), resource.getDescription(), resource.getContent());
	}

	public void put(String uri, String title, String description, String content) throws IOException {
		putRaw(uri,
				extractEntitiesFieldValue(title),
				extractEntitiesFieldValue(description),
				extractEntitiesFieldValue(content));
	}

	private String _join(String[] arr) {
		StringBuilder builder = new StringBuilder();
		for (String s : arr) {
			builder.append(s);
		}
		return builder.toString();
	}

	public void putRaw(String uri, String[] entitiesInTitle, String[] entitiesInDescription, String[] entitiesInContent) throws IOException {
		putRaw(uri, _join(entitiesInTitle), _join(entitiesInDescription), _join(entitiesInContent));
	}

	/**
	 * This methods writes an entry in the index.
	 * 
	 * @param uri - identifier of the resource
	 * @param titleEntities - a space separate list of entities, each repeated according to its relevance
	 * @param descriptionEntities - same as above
	 * @param contentEntities - same as above
	 * @throws IOException - in case an IO problem occurs
	 */
	public void putRaw(String uri, String titleEntities, String descriptionEntities, String contentEntities) throws IOException {
		logger.info("Put {}", uri);
		Document doc = new Document();
		logger.trace("uri: {}", uri);
		logger.trace("title: {}", titleEntities);
		logger.trace("description", descriptionEntities);
		logger.trace("content: {}", contentEntities);
		doc.add(new Field("SpotLightedWebResourceURI", uri, Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field("SpotLightedWebResourceText", titleEntities, Field.Store.YES, Field.Index.ANALYZED, TermVector.YES));
		doc.add(new Field("SpotLightedWebResourceDescription", descriptionEntities, Field.Store.YES, Field.Index.ANALYZED, TermVector.YES));
		doc.add(new Field("SpotLightedWebResourceContent", contentEntities, Field.Store.YES, Field.Index.ANALYZED, TermVector.YES));
		resourcesIW.addDocument(doc);
	}

	public void put(DiscouInputCollector collector) throws IOException {
		logger.info("Putting {}", collector);
		while (collector.hasNext()) {
			put(collector.next());
		}
	}

	public String extractEntitiesFieldValue(String text) {

		if (text.trim().length() == 0) {
			logger.debug("Text is empty.");
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

	private List<SpotlightAnnotation> executeNER(String textId, String text) {
		logger.trace("executeNER {}", text);
		double confidence = 0.2;
		int support = 0;
		long sss; // service response time check
		String result = "";
		try {

			SpotlightClient c = new SpotlightClient(getDBPediaSpotlightServiceURL());
			SpotlightResponse response = c.perform(text, confidence, support);
			sss = response.getMilliseconds();
			result = response.getXml();
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
			logger.debug("Annotations have been cached (xml) {}", textId);
		} catch (Exception e) {
			logger.error("Failed executeNER", e);
			return Collections.emptyList();
		}

		List<SpotlightAnnotation> a = SpotlightClient.toList(result);
		logger.info("{} entities in {} bytes. Response in {}ms", new Object[] { Integer.toString(a.size()), Integer.toString(text.getBytes().length), Long.toString(sss) });
		return a;
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
			logger.error("", e);
			return Collections.emptyList();
		} catch (UnsupportedEncodingException e) {
			logger.error("", e);
			return Collections.emptyList();
		}catch (IllegalArgumentException e) {
			logger.error("", e);
			return Collections.emptyList();
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
				annotations = SpotlightClient.toList(decoded);
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
					// indexSearcher.close();
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
				return executeNER(textId, text);
			} catch (Exception e) {
				logger.error("ERROR", e);
				return Collections.emptyList();
			}
		}
	}

}
