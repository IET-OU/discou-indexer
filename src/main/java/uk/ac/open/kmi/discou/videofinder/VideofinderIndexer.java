package uk.ac.open.kmi.discou.videofinder;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.open.kmi.discou.DiscouIndexer;
import uk.ac.open.kmi.discou.DiscouInputCollector;

public class VideofinderIndexer {

	private DiscouIndexer discouIndexer;
	private static final Logger log = LoggerFactory.getLogger(VideofinderIndexer.class);

	public VideofinderIndexer(String indexHome) {
		discouIndexer = new DiscouIndexer(new File(indexHome));
	}

	public void start(){
		log.info("Start indexing");
		DiscouInputCollector videofinderCollector = VideofinderInputFactory.get().build();
		
		discouIndexer.open();
		try {
			discouIndexer.put(videofinderCollector);
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

	public static void main(String[] args) throws IOException {
		new VideofinderIndexer(args[0]).start();
	}
}
