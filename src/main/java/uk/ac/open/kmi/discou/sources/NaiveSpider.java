package uk.ac.open.kmi.discou.sources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NaiveSpider {
	private Logger log = LoggerFactory.getLogger(NaiveSpider.class);

	public Set<String> start;
	public Set<String> tovisit;
	public Set<String> visited;
	public Map<String, Exception> broken;

	public NaiveSpider(String... startUrls) {
		pTag = Pattern.compile(HTML_TAG_PATTERN);
		pLink = Pattern.compile(HTML_HREF_TAG_PATTERN);

		start = new HashSet<String>();
		tovisit = new HashSet<String>();
		visited = new HashSet<String>();
		broken = new HashMap<String, Exception>();

		start.addAll(Arrays.asList(startUrls));
		tovisit.addAll(start);
	}

	private String getStringFromInputStream(InputStream is) {

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try {

			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return sb.toString();

	}

	private Matcher mTag, mLink;
	private Pattern pTag, pLink;

	private static final String HTML_TAG_PATTERN = "(?i)<a([^>]+)>(.+?)</a>";
	private static final String HTML_HREF_TAG_PATTERN = "\\s*(?i)href\\s*=\\s*((\"([^\"]*)\")|('([^']*)')|(([^'\">\\s]+)))";

	public Set<String> extractHTMLLinks(final String sourceHtml) {

		Set<String> elements = new HashSet<String>();

		mTag = pTag.matcher(sourceHtml);

		while (mTag.find()) {
			String href = mTag.group(1); // get the values of href
			mLink = pLink.matcher(href);
			while (mLink.find()) {
				String link = mLink.group(3);
				if(link != null && !"".equals(link)){
					elements.add(link);
				}
			}

		}

		return elements;

	}

	public void start() {
		while (!tovisit.isEmpty()) {
			String visit = tovisit.iterator().next();
			tovisit.remove(visit);
			visited.add(visit);
			try {
				log.debug("Visiting {}", visit);
				URL following = new URL(visit);
				HttpURLConnection uc = (HttpURLConnection) following.openConnection();
				uc.setInstanceFollowRedirects(true);
				
				int status = uc.getResponseCode();
				if(status == HttpURLConnection.HTTP_OK){
					String contentType = uc.getHeaderField("Content-type");
					log.debug("content-type: {}", contentType);
					if(contentType.contains("pdf")){
						PDDocument document = PDDocument.load(uc.getInputStream(), true);
						PDFTextStripper stripper = new PDFTextStripper("UTF-8");
						stripper.setForceParsing(true);
						String res = stripper.getText(document);
						process(res);
						document.close();
					}else if(contentType.contains("html")){
						InputStream is = uc.getInputStream();
						String theString = getStringFromInputStream(is);
						log.trace("HTML: {}", theString);
						Set<String> links = extractHTMLLinks(theString);
						boolean found = false;
						for (String l : links) {
							// ignore hash links
							if(l.startsWith("#")){
								continue;
							}else if(l.indexOf('#') > -1){
								l = l.substring(0, l.lastIndexOf('#'));
							}
							found = true;
							if (visited.contains(l) || broken.containsKey(l))
								continue;
							boolean follow = follow(l.toString());
							if (follow) {
								tovisit.add(l.toString());
							}
						}
						
						if (!found) {
							log.debug("No links in {}", visit);
						}
						// we pass the html page to the process method
						process(theString);
					}else{
						log.warn("Unsupported content-type: {}", contentType);
					}
				}
			} catch (Exception e) {
				log.error("FAILED", e);
				broken.put(visit, e);
			}
		}
	}

	public abstract boolean follow(String link);

	public abstract void process(String html);
}
