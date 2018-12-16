package uk.ac.open.kmi.discou;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class DiscouReader {
	private IndexSearcher indexSearcher = null;
	private IndexReader indexReader = null;
	private File home;

	public DiscouReader(File home) throws IOException {
		this.home = home;
	}

	public void open() throws IOException {
		Directory directory = FSDirectory.open(new File(home, DiscouIndexer._SpotLightedWebResourceIndexPath));
		indexReader = IndexReader.open(directory);
		indexSearcher = new IndexSearcher(indexReader);
	}

	public void close() throws IOException {
		// indexSearcher.close();
		indexReader.close();
	}

	public boolean exists(String uri) throws IOException {
		assertOpen();
		throw new RuntimeException("Not implemented yet");
	}

	protected int getDocIdFromURI(String uri) throws ResourceDoesNotExistException, IOException {
		BooleanQuery query = new BooleanQuery();
		query.add(new TermQuery(new Term("SpotLightedWebResourceURI", uri)), BooleanClause.Occur.MUST);
		// must be opened first

		TopDocs docs = indexSearcher.search(query, 1);

		if (docs.scoreDocs.length > 0) {
			int docId = docs.scoreDocs[0].doc;
			return docId;
		} else {
			throw new ResourceDoesNotExistException();
		}
	}

	public DiscouResource getFromDocId(int luceneDocId) throws IOException {
		assertOpen();
		final Document document = indexSearcher.doc(luceneDocId);
		final String spotLightUri = document.get("SpotLightedWebResourceURI");
		final String spotLightText = document.get("SpotLightedWebResourceText");
		final String spotLightDescription = document.get("SpotLightedWebResourceDescription");
		final String spotLightContent = document.get("SpotLightedWebResourceContent");
		return new DiscouResource() {

			public String getUri() {
				return spotLightUri;
			}

			public String getTitle() {
				return spotLightText;
			}

			public String getDescription() {
				return spotLightDescription;
			}

			public String getContent() {
				return spotLightContent;
			}
		};
	}

	public DiscouResource getFromURI(String uri) throws IOException {
		assertOpen();
		return getFromDocId(getDocIdFromURI(uri));
	}

	protected void assertOpen() throws IOException {
		if (indexSearcher == null || indexReader == null) {
			throw new IOException("open the reader first");
		}
	}

	public Map<String, Float> similar(String uri, int hitsNumber) throws IOException {
		try {
			assertOpen();
			BooleanQuery q = new BooleanQuery();
			q.add(new TermQuery(new Term("SpotLightedWebResourceURI", uri)),
					BooleanClause.Occur.MUST);
			TopScoreDocCollector collector1 = TopScoreDocCollector.create(1, true);
			indexSearcher.search(q, collector1);
			ScoreDoc[] hits1 = collector1.topDocs().scoreDocs;
			int docId1 = hits1[0].doc;
			
			MoreLikeThis mlt = new MoreLikeThis(indexReader);

			String[] fieldNames = new String[3];
			fieldNames[0] = "SpotLightedWebResourceText";
			fieldNames[1] = "SpotLightedWebResourceDescription";
			fieldNames[2] = "SpotLightedWebResourceContent";
			mlt.setFieldNames(fieldNames);
			mlt.setAnalyzer(new WhitespaceAnalyzer(Version.LUCENE_47));
			mlt.setMinTermFreq(5);
			mlt.setMinDocFreq(1);

			BooleanQuery bq = new BooleanQuery();

			Query q2 = mlt.like(docId1);
			
			// Get only resources starting with http...
			WildcardQuery wcq = new WildcardQuery(new Term("SpotLightedWebResourceURI", "http*"));

			bq.add(q2, BooleanClause.Occur.MUST);
			bq.add(wcq, BooleanClause.Occur.MUST);

			TopScoreDocCollector collector = TopScoreDocCollector.create(hitsNumber * 10, true);
			
			indexSearcher.search(q2, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;
			Map<String, Float> results = new HashMap<String, Float>();

			int count = 0;
			for (int i = 0; i < hits.length; ++i) {
				int docId = hits[i].doc;
				Document document = indexSearcher.doc(docId);
				// get/set the uri of the i-result in QueryResult
				String resultUri = document.get("SpotLightedWebResourceURI");
				// FIXME WHY IT NEEDS TO BE a DATA.OPEN URI?
				if (resultUri.startsWith("http://data.open.ac.uk")) {
					// get/set the lucene score
					float luceneScore = hits[i].score;
					results.put(resultUri, luceneScore);
					count++;
					if (count == hitsNumber) {
						break;
					}
				}
			}

			indexReader.close();
			return results;
		} catch (IOException e) {
			throw e;
		}
	}

	public int count() throws IOException {
		assertOpen();
		return indexReader.numDocs();
	}
}
