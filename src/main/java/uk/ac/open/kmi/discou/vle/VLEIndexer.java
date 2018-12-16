package uk.ac.open.kmi.discou.vle;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.open.kmi.discou.DiscouIndexer;
import uk.ac.open.kmi.discou.DiscouInputCollector;
import uk.ac.open.kmi.discou.SparqlInputCollectorBuilder;

public class VLEIndexer {
	private DiscouIndexer discouIndexer;
	private static final Logger log = LoggerFactory.getLogger(VLEIndexer.class);

	public VLEIndexer(String indexHome, String spotlight) {
		discouIndexer = new DiscouIndexer(new File(indexHome), spotlight);
	}
	
	public VLEIndexer(String indexHome) {
		discouIndexer = new DiscouIndexer(new File(indexHome));
	}

	public void start() {
		log.info("Start indexing");
		DiscouInputCollector vleCollector = new SparqlInputCollectorBuilder()
		.endpoint("http://sdata.kmi.open.ac.uk/vle/sparql")
		.title("http://www.w3.org/2000/01/rdf-schema#label")
		.type("http://xmlns.com/foaf/0.1/Document")
		.content("http://dbpedia.org/property/text").build();
		
		discouIndexer.open();
		try {
			discouIndexer.put(vleCollector);
		} catch (IOException e) {
			log.error("",e);
		}finally{
			try {
				discouIndexer.commit();
			} catch (IOException e) {
				log.error("",e);
			}finally{
				try {
					discouIndexer.close();
				} catch (IOException e) {
					log.error("",e);
				}				
			}
		}
	}

	public static void main(String[] args) {
		if(args.length == 1){
			new VLEIndexer(args[0]).start();			
		}else{
			new VLEIndexer(args[0], args[1]).start();
		}
	}
}
