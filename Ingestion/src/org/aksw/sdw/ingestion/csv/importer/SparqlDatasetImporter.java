package org.aksw.sdw.ingestion.csv.importer;

import java.io.IOException;
import java.util.List;

import org.aksw.sdw.ingestion.IngestionException;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import aksw.org.sdw.kg.handler.sparql.SparqlHandler;

/**
 * This class can be used to load data from an SPARQL endpoint.
 * 
 * @author kay
 *
 */
public class SparqlDatasetImporter implements DatasetImporter {
	
	protected SparqlHandler sparqlHandler;
	
	protected QueryExecution queryExecutionUnit;
	
	protected QueryExecution queryExecutionUnitInstance;
	
	protected ResultSet resultSet;
	protected StmtIterator tripleIteratorInstance;
	
	protected final int limitTriple = 20_000;
	protected int offsetTriple = 0;
	
	protected final int limitInstance = 300;
	protected int offsetInstance = 0;
	protected List<String> filterUris;
	
	protected final String constructQuery;
	protected final String selectQuery;
	protected final String instanceFilterQuery;
	
	protected boolean increasedInstanceCount = false;
	
	//Model model;
	
	public SparqlDatasetImporter(final String sparqlEndPoint, final String graphName, final String instanceFilterQuery, final String construct, final String select) {
		this.sparqlHandler = new SparqlHandler(sparqlEndPoint, graphName);		
		
		this.constructQuery = construct;
		this.selectQuery = select;
		this.instanceFilterQuery = instanceFilterQuery;
	}
	
	protected String createFinalQuery(final String construct, final String select, final List<String> instanceUris, final int offset, final int limit) {
		StringBuffer buffer = new StringBuffer();
		
		String newSelect = select;//.replace("?subjectUri", "<" + instanceUri + ">");
		
		int lastPos = newSelect.lastIndexOf("}");
		
		StringBuffer filterUris = new StringBuffer();
		
		filterUris.append(" FILTER ( ");
		boolean notFirst = false;
		for (String instanceUri : instanceUris) {
			if (notFirst) {
				filterUris.append(" || ");
			}
			
			filterUris.append("STR(?subjectUri) = STR(\"");
			filterUris.append(instanceUri);
			filterUris.append("\") ");
			
			notFirst = true;
		}
		filterUris.append(" )");
		
		newSelect = newSelect.substring(0, lastPos) + filterUris.toString() + " }";
		
		buffer.append(construct);
		buffer.append("\nWHERE {\n{");
		buffer.append(newSelect);
		buffer.append("}\n");
		buffer.append("} LIMIT ").append(limit).append(" OFFSET ").append(offset);
		
		return buffer.toString();
	}
	
	protected String getSearchQuery(final String construct, final String select, final String instanceFilterQuery,
									final int offsetTriple, final int limitTriple, final int offsetInstance, final int limitInstance) {
		StringBuffer query = new StringBuffer();
		
		String finalInstanceFilterQuery = instanceFilterQuery + " LIMIT " + limitInstance + " OFFSET " + offsetInstance;
		
		int pos = select.indexOf("{");
		
		String mergedSelectQuery = select.substring(0, pos + 1) + "\n{ " + finalInstanceFilterQuery + " } \n" + select.substring(pos + 2) + " ORDER BY ?subjectUri ";
		
		query.append(construct);
		query.append("\nWHERE {\n{");
		query.append(mergedSelectQuery);
		query.append("}\n");
		query.append("} LIMIT ").append(limitTriple).append(" OFFSET ").append(offsetTriple);
		
		return query.toString();
		
	}

	@Override
	public void close() throws IOException {
		
//		if (null == this.tripleIterator) {
//			this.tripleIterator = null;
//		}		
		
		if (null != this.queryExecutionUnit) {
			this.queryExecutionUnit.close();
			this.queryExecutionUnit = null;
		}
		
		if (null != this.sparqlHandler) {
			this.sparqlHandler = null;
		}
	}
	
	protected boolean hasNextInstance() throws IngestionException {
		
		if  (null == this.resultSet || false == this.resultSet.hasNext()) {
			
			if (null != this.queryExecutionUnit) {
				this.queryExecutionUnit.close();
				this.queryExecutionUnit = null;
			}
			
			String finalQuery = instanceFilterQuery + " LIMIT " + Integer.toString(this.limitTriple) + " OFFSET " + Integer.toString(this.offsetTriple);
			this.queryExecutionUnit = this.sparqlHandler.createQueryExecuter(finalQuery);
			this.offsetTriple += this.limitTriple;
			
			this.resultSet = queryExecutionUnit.execSelect();
			return this.resultSet.hasNext();
		}
		
		return true;
	}
	
	protected String nextInstance() {
		QuerySolution queryResult = this.resultSet.next();
		RDFNode uri = queryResult.get("subjectUri");
		return uri.toString();
	}
	
	

	@Override
	public boolean hasNext() throws IngestionException {
		if (null == this.tripleIteratorInstance || false == this.tripleIteratorInstance.hasNext()) {
			
			if (null != this.queryExecutionUnit) {
				this.queryExecutionUnit.close();
				this.queryExecutionUnit = null;
			}
			
			// if we still do not get any result after increasing the instance count
			// --> stop
			if (increasedInstanceCount) {
				return false;
			}
			
			String query = this.getSearchQuery(this.constructQuery, this.selectQuery, this.instanceFilterQuery, this.offsetTriple, this.limitTriple, this.offsetInstance, this.limitInstance);
			this.queryExecutionUnit = this.sparqlHandler.createQueryExecuter(query);

			this.offsetInstance += this.limitInstance;
			
			increasedInstanceCount = true; // mark that we have increased instance limit
			this.offsetTriple = 0; // reset offset for triples
				
			Model model = this.queryExecutionUnit.execConstruct();
			if (null != model && 0 < model.size()) {
				 this.tripleIteratorInstance = model.listStatements();
				 return this.hasNext();
			} else {				
				return false;
			}
		}
		
		increasedInstanceCount = false;
		
		return true;
	}

	@Override
	public Triple next() throws IngestionException {
		
		Statement statement = this.tripleIteratorInstance.next();
		return statement.asTriple();
	}
	
}