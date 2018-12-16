package uk.ac.open.kmi.discou.dataopen;

import java.net.URL;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

import uk.ac.open.kmi.discou.DiscouInputCollectorBuilder;
import uk.ac.open.kmi.discou.SparqlInputCollectorBuilder;
import uk.ac.open.kmi.discou.sources.OpenLearnExploreScraper;
import uk.ac.open.kmi.discou.sources.OpenLearnUnitScraper;

public class DataOpenInputFactory {

	public static DiscouInputCollectorBuilder podcast() {
		return new SparqlInputCollectorBuilder() {
			@Override
			protected String prepareContent(String content) {
				PDDocument document = null;
				try {
					URL url = new URL(content);
					document = PDDocument.load(url, true);
					PDFTextStripper stripper = new PDFTextStripper("UTF-8");
					stripper.setForceParsing(true);
					String res = stripper.getText(document);
					document.close();
					return res;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return "";
			}
		}
				.endpoint("http://data.open.ac.uk/sparql")
				.from("http://data.open.ac.uk/context/podcast")
				.title("http://purl.org/dc/terms/title")
				.description("http://purl.org/dc/terms/description")
				.content("http://data.open.ac.uk/podcast/ontology/transcript");
	}

	public static DiscouInputCollectorBuilder openlearnexplore() {
		// open learn explore
		return new SparqlInputCollectorBuilder() {
			protected String prepareContent(String locator) {
				OpenLearnExploreScraper sc = new OpenLearnExploreScraper(locator);
				sc.start();
				return sc.getText();
			};
		}.endpoint("http://data.open.ac.uk/sparql")
				.from("http://data.open.ac.uk/context/openlearn2")
				.type("http://data.open.ac.uk/openlearn/ontology/OpenLearnArticle")
				.title("http://purl.org/dc/terms/title")
				.description("http://purl.org/dc/terms/description")
				.content("http://www.w3.org/TR/2010/WD-mediaont-10-20100608/locator");
	}

	public static DiscouInputCollectorBuilder openlearnCourse() {
		// open learn units
		return new SparqlInputCollectorBuilder() {
			protected String prepareContent(String locator) {
				OpenLearnUnitScraper sc = new OpenLearnUnitScraper(locator);
				sc.start();
				return sc.getText();
			};
		}.endpoint("http://data.open.ac.uk/sparql")
				.from("http://data.open.ac.uk/context/openlearn2")
				.type("http://data.open.ac.uk/openlearn/ontology/OpenCourse")
				.title("http://purl.org/dc/terms/title")
				.description("http://purl.org/dc/terms/description")
				.content("http://www.w3.org/TR/2010/WD-mediaont-10-20100608/locator");
	}

	public static DiscouInputCollectorBuilder openlearnCourseware() {
		// open learn units
		return new SparqlInputCollectorBuilder() {
			protected String prepareContent(String locator) {
				OpenLearnUnitScraper sc = new OpenLearnUnitScraper(locator);
				sc.start();
				return sc.getText();
			};
		}.endpoint("http://data.open.ac.uk/sparql")
				.from("http://data.open.ac.uk/context/openlearn2")
				.type("http://data.open.ac.uk/openlearn/ontology/OpenCourseware")
				.title("http://purl.org/dc/terms/title")
				.description("http://purl.org/dc/terms/description")
				.content("http://www.w3.org/TR/2010/WD-mediaont-10-20100608/locator");
	}
}
