package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import aksw.org.kg.entity.Entity;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.CorpDbpedia;
import org.aksw.sdw.ingestion.csv.constants.W3CProvenance;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;

import org.apache.jena.datatypes.xsd.impl.XSDDateTimeType;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;

public class EventNormalizer implements PropertyNormalizer {

	

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {
		/// TODO km: add IPO Date to Public companies in ontology!
		/// TODO km: add check that a correct company was assigned!!
		/// TODO km: check LTD!!!!
		List<RDFNode> ipoDates = entity.getRdfObjects("http://permid.org/ontology/organization/hasIPODate");
		if (null != ipoDates &&  false == ipoDates.isEmpty()) {
			
			// create change event for IPO event and relate back to this company
			/// TODO km: think about, if this is correct!
			int index = 0;
			for (RDFNode ipoDate : ipoDates) {
				
				Entity ipoEvent = new Entity(false);
				String newUri = new StringBuffer().append(entity.getSubjectUri()).
						append("_ipoEvent").append(Integer.toString(index)).toString();
				ipoEvent.setSubjectUri(newUri);
				
				ipoEvent.addTriple(RDF.type.getURI(), new ResourceImpl(CorpDbpedia.IPO));
				ipoEvent.addTriple(RDF.type.getURI(), new ResourceImpl(W3COrg.ChangeEvent));
				
				String date = ipoDate.asLiteral().getLexicalForm();
				
				try {
					DatatypeFactory df = DatatypeFactory.newInstance();
					XMLGregorianCalendar dateTime = df.newXMLGregorianCalendar(date);
					
					//RdfObjectLiteral dateLiteral = new RdfObjectLiteral(dateTime.toXMLFormat(), XSD.dateTime.getURI());
					ipoEvent.addTripleWithLiteral(W3CProvenance.startedAtTime, dateTime.toXMLFormat(), XSDDateTimeType.XSDdateTime);
				} catch (Exception e) {
					// ignore
				}
								
				//entity.addTriple(CorpDbpedia.prefixOntology + "hasIPODate", ipoDate);
				entity.addTriple(W3COrg.changedBy, ipoEvent.getSubjectUriObject());
				ipoEvent.addTriple(W3COrg.originalOrganization, entity.getSubjectUriObject());
				entity.addSubEntity(ipoEvent);
				
				++index;
			}
			
					
		}
		
		entity.deleteProperty("http://permid.org/ontology/organization/hasIPODate");
		
		List<RDFNode> establishedDates = entity.getRdfObjects("established");
		if (null != establishedDates && false == establishedDates.isEmpty()) {
			
			int index = 0;
			for (RDFNode establishedDate : establishedDates) {
				Entity establishedEntity = new Entity(false);
				
				String newUri = new StringBuffer().append(entity.getSubjectUri()).
						append("_foundationEvent").append(Integer.toString(index)).toString();
				establishedEntity.setSubjectUri(newUri);
				
				establishedEntity.addTriple(RDF.type.getURI(), new ResourceImpl(CorpDbpedia.CompanyFoundation));
				establishedEntity.addTriple(RDF.type.getURI(), new ResourceImpl(W3COrg.ChangeEvent));
				
				++index;
				
				String date = establishedDate.asLiteral().getLexicalForm();
				
				try {
					DatatypeFactory df = DatatypeFactory.newInstance();
					XMLGregorianCalendar dateTime = df.newXMLGregorianCalendar(date);
					
					establishedEntity.addTripleWithLiteral(W3CProvenance.startedAtTime, dateTime.toXMLFormat(), XSDDateTimeType.XSDdateTime);
				} catch (Exception e) {
					// ignore
				}
								
				//entity.addTriple(CorpDbpedia.prefixOntology + "hasIPODate", ipoDate);
				entity.addTriple(W3COrg.resultedFrom, establishedEntity.getSubjectUriObject());
				establishedEntity.addTriple(W3COrg.resultingOrganization, entity.getSubjectUriObject());
				entity.addSubEntity(establishedEntity);
			}
		}
		
		entity.deleteProperty("established");
	}
}
