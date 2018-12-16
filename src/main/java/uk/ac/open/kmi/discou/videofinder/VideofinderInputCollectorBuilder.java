package uk.ac.open.kmi.discou.videofinder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import uk.ac.open.kmi.discou.SparqlInputCollectorBuilder;

public class VideofinderInputCollectorBuilder extends SparqlInputCollectorBuilder {
	@Override
	protected String prepareContent(String contentIn) {
		// we assume this to be a space-separated list of URLs
		List<String> urls = Arrays.asList(contentIn.split(" "));
		StringBuilder toReturn = new StringBuilder();
		for (String content : urls) {
			if (content.trim().toLowerCase().endsWith(".pdf")) {
				content = VideofinderInputFactory.pdfHelper(content);
			} else if (content.trim().toLowerCase().endsWith(".html")) {
				URL following;
				try {
					following = new URL(content);
					HttpURLConnection uc = (HttpURLConnection) following.openConnection();
					InputStream is = uc.getInputStream();
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

					content = sb.toString();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			toReturn.append(content);
		}
		return super.prepareContent(toReturn.toString());
	}
}
