package uk.ac.open.kmi.discou;

public class DiscouInputResourceImpl implements DiscouInputResource {

	String uri;
	String title;
	String description;
	String content;

	public DiscouInputResourceImpl(String uri, String title, String description, String content) {
		this.uri = uri;
		this.title = title;
		this.description = description;
		this.content = content;
	}

	public String getUri() {
		return uri;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getContent() {
		return content;
	}
}
