package uk.ac.open.kmi.discou;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class DiscouReader {
	private IndexSearcher indexSearcher = null;
	private IndexReader indexReader = null;
	private File home;
	
	public DiscouReader(File home) throws IOException {
		this.home = home;
	}
	
	public void open() throws IOException{
		Directory directory = FSDirectory.open(new File(home, DiscouIndexer._SpotLightedWebResourceIndexPath));
		indexReader = IndexReader.open(directory);
		indexSearcher = new IndexSearcher(indexReader);
	}
	
	public void close() throws IOException{
		//indexSearcher.close();
		indexReader.close();
	}

	public boolean exists(String uri) throws IOException {
		assertOpen();
		throw new RuntimeException("Not implemented yet");
	}
	
	protected int getDocIdFromURI(String uri) throws ResourceDoesNotExistException,IOException {
		BooleanQuery query = new BooleanQuery();
		query.add(new TermQuery(new Term("SpotLightedWebResourceURI", uri)), BooleanClause.Occur.MUST);
		// must be opened first
		
		TopDocs docs = indexSearcher.search(query, 1);

		if (docs.scoreDocs.length > 0) {
			int docId = docs.scoreDocs[0].doc;
			return docId;
		}else{
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
	

	protected void assertOpen() throws IOException{
		if(indexSearcher == null || indexReader == null){
			throw new IOException("open the reader first");
		}
	}
	
}
