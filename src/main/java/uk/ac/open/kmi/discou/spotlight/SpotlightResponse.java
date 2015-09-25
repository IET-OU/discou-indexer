package uk.ac.open.kmi.discou.spotlight;

public class SpotlightResponse {

	public SpotlightResponse() {
	}

	public SpotlightResponse(String xml, long ms) {
		this.xml = xml;
		this.milliseconds = ms;
	}

	public String getXml() {
		return xml;
	}

	protected void setXml(String xml) {
		this.xml = xml;
	}

	public long getMilliseconds() {
		return milliseconds;
	}

	protected void setMilliseconds(long milliseconds) {
		this.milliseconds = milliseconds;
	}

	private String xml;
	private long milliseconds;

}
