package uk.ac.open.kmi.discou.dataopen;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.open.kmi.discou.DiscouIndexer;
import uk.ac.open.kmi.discou.DiscouInputCollector;

public class DataOpenIndexer {

	private DiscouIndexer discouIndexer;
	private static final Logger log = LoggerFactory.getLogger(DataOpenIndexer.class);

	public DataOpenIndexer(String indexHome, String spotlight) {
		discouIndexer = new DiscouIndexer(new File(indexHome), spotlight);
	}

	public void start(){
		log.info("Start indexing");
		DiscouInputCollector openLearnCourse = DataOpenInputFactory.openlearnCourse().build();
		DiscouInputCollector openLearnCourseware = DataOpenInputFactory.openlearnCourseware().build();
		DiscouInputCollector openLearnExploreCollector = DataOpenInputFactory.openlearnexplore().build();
		DiscouInputCollector podcastCollector = DataOpenInputFactory.podcast().build();
		
		discouIndexer.open();
		try {
			discouIndexer.put(openLearnExploreCollector);
			discouIndexer.put(openLearnCourse);
			discouIndexer.put(openLearnCourseware);
			discouIndexer.put(podcastCollector);
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
		if(args.length == 2){
			new DataOpenIndexer(args[0], args[1]).start();
		}else{
			System.err.println("Invalid arguments.");
		}
	}
}
