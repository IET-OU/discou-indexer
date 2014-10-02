package uk.ac.open.kmi.discou.videofinder;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

import uk.ac.open.kmi.discou.DiscouInputCollectorBuilder;

public class VideofinderInputFactory {
	protected static String pdfHelper(String urlstr){
		PDDocument document = null;
		try {
		    URL url = new URL( urlstr );
		    document = PDDocument.load(url, true);
		    PDFTextStripper stripper = new PDFTextStripper("UTF-8");	
		    stripper.setForceParsing( true );
		    String res = stripper.getText( document );		    
		    document.close();
		    return res;
		} catch(Exception e ){
		    e.printStackTrace();
		}
		return "";
	}
	
	public static String prepareHtml(String html) {
		StringBuilder sb = new StringBuilder();
		// do nothing
		Pattern p = Pattern.compile("(?i)<p>(.+)</p>");
		Matcher m = p.matcher(html);
		while (m.find()) {
			sb.append(System.getProperty("line.separator")).append(m.group(1).replaceAll("<script\\s.*?/script>", "").replaceAll("\\<.+?\\>", " ").replaceAll("\\s+", " "));
		}
		return sb.toString();
	}
	public static DiscouInputCollectorBuilder get() {
		return new VideofinderInputCollectorBuilder().endpoint("http://sdata.kmi.open.ac.uk/videofinder/sparql").from("http://data.open.ac.uk/context/videofinder")
				.title("http://purl.org/dc/terms/title").type("http://data.open.ac.uk/videofinder/ontology/VideofinderObject").description("http://purl.org/dc/terms/description")
				.content("http://data.open.ac.uk/videofinder/ontology/synopsis").content("http://data.open.ac.uk/videofinder/ontology/transcript");
	}
}
