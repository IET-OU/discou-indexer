package uk.ac.open.kmi.discou;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

public class SparqlInputCollectorBuilder implements DiscouInputCollectorBuilder {

	private Logger logger = LoggerFactory.getLogger(SparqlInputCollectorBuilder.class);

	private static String var_URI = "x";
	private static String var_title = "t";
	private static String var_description = "d";
	private static String var_content = "c";
	private static String var_ptitle = "pt";
	private static String var_pdescription = "pd";
	private static String var_pcontent = "pc";

	private String endpoint = null;
	private Set<String> datasets;
	private Set<String> typeValue;
	private Set<String> titleProperty;
	private Set<String> descriptionProperty;
	private Set<String> contentProperty;
	private int limit = 0;

	public SparqlInputCollectorBuilder() {
		datasets = new HashSet<String>();
		typeValue = new HashSet<String>();
		titleProperty = new HashSet<String>();
		descriptionProperty = new HashSet<String>();
		contentProperty = new HashSet<String>();
	}

	public SparqlInputCollectorBuilder endpoint(String endpoint) {
		this.endpoint = endpoint.trim();
		return this;
	}

	public SparqlInputCollectorBuilder from(String dataset) {
		this.datasets.add(dataset.trim());
		return this;
	}

	public SparqlInputCollectorBuilder title(String property) {
		this.titleProperty.add(property.trim());
		return this;
	}

	public SparqlInputCollectorBuilder type(String value) {
		this.typeValue.add(value);
		return this;
	}

	public SparqlInputCollectorBuilder description(String property) {
		this.descriptionProperty.add(property.trim());
		return this;
	}

	public SparqlInputCollectorBuilder content(String property) {
		this.contentProperty.add(property.trim());
		return this;
	}

	public SparqlInputCollectorBuilder limit(int limit) {
		this.limit = limit;
		return this;
	}

	public String buildQuery() {
		StringBuilder sb = new StringBuilder();
		sb.append("select ").append(" ?").append(var_URI).append(" ");

		if (!titleProperty.isEmpty()) {
			sb.append(" ( GROUP_CONCAT( DISTINCT ?").append(var_title).append("_ ; separator=\" \") as ?").append(var_title).append(") ");
		}
		if (!descriptionProperty.isEmpty()) {
			sb.append(" (GROUP_CONCAT( DISTINCT ?").append(var_description).append("_ ; separator=\" \") as ?").append(var_description).append(") ");
		}
		if (!contentProperty.isEmpty()) {
			sb.append(" (GROUP_CONCAT( DISTINCT ?").append(var_content).append("_ ; separator=\" \") as ?").append(var_content).append(") ");
		}

		for (String d : datasets) {
			sb.append(" from <").append(d).append("> ");
		}
		sb.append(" where { ").append(" ?").append(var_URI).append(" ");
		boolean first = true;
		// types

		for (String t : typeValue) {
			if (first) {
				first = false;
			} else {
				sb.append(" ; ");
			}
			sb.append("a <").append(t).append("> ");
		}

		if (!titleProperty.isEmpty()) {
			if (first) {
				first = false;
			} else {
				sb.append(" ; ");
			}
			sb.append("?").append(var_ptitle).append(" ?").append(var_title).append("_ ");
		}

		if (!descriptionProperty.isEmpty()) {
			if (first) {
				first = false;
			} else {
				sb.append(" ; ");
			}
			sb.append("?").append(var_pdescription).append(" ?").append(var_description).append("_ ");
		}

		if (!contentProperty.isEmpty()) {
			if (first) {
				first = false;
			} else {
				sb.append(" ; ");
			}
			sb.append("?").append(var_pcontent).append(" ?").append(var_content).append("_ ");
		}

		if (first) {
			sb.append(" [] [] ");
		}
		sb.append(".");

		// title options
		if (!titleProperty.isEmpty()) {
			sb.append(" FILTER ( ?").append(var_ptitle).append(" in (");

			first = true;
			for (String tp : titleProperty) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append("<").append(tp).append(">");
			}
			sb.append(") ) . ");
		}
		// description options
		if (!descriptionProperty.isEmpty()) {
			sb.append(" FILTER ( ?").append(var_pdescription).append(" in (");
			first = true;
			for (String tp : descriptionProperty) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append("<").append(tp).append(">");
			}
			sb.append(") ) . ");
		}
		// content options
		if (!contentProperty.isEmpty()) {
			sb.append(" FILTER ( ?").append(var_pcontent).append(" in (");

			first = true;
			for (String tp : contentProperty) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append("<").append(tp).append(">");
			}
			sb.append(") ) . ");
		}
		sb.append("} GROUP BY ?").append(var_URI);
		if (limit > 0) {
			sb.append(" LIMIT ").append(limit);
		}
		return sb.toString();
	}

	private void checkPreconditions() {
		if (endpoint != null) {
		} else {
			throw new RuntimeException("Invalid State. Missing one of these: endpoint");
		}
	}

	public DiscouInputCollector build() {
		checkPreconditions();
		String q = buildQuery();
		logger.debug("Query: {}", q);
		final Query query = QueryFactory.create(q);
		final QueryExecution qe = QueryExecutionFactory.sparqlService(endpoint, query);
		ResultSet rsa;
		rsa = qe.execSelect();
		final Map<String, DiscouInputResource> resources = new HashMap<String, DiscouInputResource>();
		final ResultSet rs = rsa;
		while (rs.hasNext()) {
			final QuerySolution qs = rs.next();
			// XXX for some reasons aggregations return empty lines with Jena
			// Fuseki...
			if (qs.getResource(var_URI) == null) {
				continue;
			}
			String _uri = qs.getResource(var_URI).getURI();
			if (resources.containsKey(_uri)) {
				// this should never happen because we aggregate
				logger.warn("Two rows with uri {} (should never happen)", _uri);
			} else {
				final String title = (qs.contains(var_title) ? qs.getLiteral(var_title).getString() : "");
				final String description = (qs.contains(var_description) ? qs.getLiteral(var_description).getString() : "");
				final String content = (qs.contains(var_content) ? qs.getLiteral(var_content).getString() : "");
				resources.put(_uri, new DiscouInputResource() {
					private String _uri = prepareUri(qs.getResource(var_URI).getURI());
					private String _title = null;
					private String _desc = null;
					private String _content = null;
					@Override
					public String getUri() {
						return _uri;
					}
					
					@Override
					public String getTitle() {
						if(_title==null){
							_title = prepareTitle(title);
						}
						return _title;
					}
					
					@Override
					public String getDescription() {
						if(_desc==null){
							_desc = prepareDescription(description);
						}
						return _desc;
					}
					
					@Override
					public String getContent() {
						if(_content == null){
							_content = prepareContent(content);
						}
						return _content;
					}
				});
			}
		}
		logger.debug("{} resources", resources.values().size());
		return new DiscouInputCollector() {
			private Iterator<DiscouInputResource> collection = Collections.unmodifiableCollection(resources.values()).iterator();

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public DiscouInputResource next() {
				return collection.next();
			}

			public boolean hasNext() {
				boolean r = collection.hasNext();
				logger.trace("hasNext {}", r);
				return r;
			}
		};
	}

	protected String prepareUri(String uri) {
		return uri;
	}

	protected String prepareTitle(String title) {
		return title;
	}

	protected String prepareDescription(String description) {
		return description;
	}

	protected String prepareContent(String content) {
		return content;
	}
}
