package org.aksw.sdw.ingestion.csv.normalizer;

import java.util.List;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.aksw.sdw.ingestion.ConversionStats;
import org.aksw.sdw.ingestion.IngestionException;
import org.aksw.sdw.ingestion.csv.constants.W3COrg;
import org.aksw.sdw.ingestion.csv.constants.W3CProvenance;
import org.apache.jena.datatypes.xsd.impl.XSDDateTimeType;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;

import aksw.org.kg.entity.Entity;

public class GenericEventNormalizer implements PropertyNormalizer {
	
	/** source predicate which is found in original entity */
	protected final String sourcePredicate;
	
	/** target predicate name */
	protected final String targetPredicate;
	
	/** suffix which is ended to sub-entity URI */
	protected final String enitySuffix;
	
	public GenericEventNormalizer(final String sourcePredicate, final String targetPredicate, final String enitySuffix) {
		this.sourcePredicate = sourcePredicate;
		this.targetPredicate = targetPredicate;
		
		this.enitySuffix = enitySuffix;
	}

	@Override
	public void normalize(Entity entity, ConversionStats stats) throws IngestionException {

		List<RDFNode> eventDates = entity.getRdfObjects(this.sourcePredicate);
		if (null != eventDates && false == eventDates.isEmpty()) {
			
			int index = 0;
			for (RDFNode eventDate : eventDates) {
				Entity establishedEntity = this.createNewEventEntity(entity, index++);
				
				String date = eventDate.asLiteral().getLexicalForm();				
				try {
					DatatypeFactory df = DatatypeFactory.newInstance();
					XMLGregorianCalendar dateTime = df.newXMLGregorianCalendar(date);
					
					establishedEntity.addTripleWithLiteral(W3CProvenance.startedAtTime, dateTime.toXMLFormat(), XSDDateTimeType.XSDdateTime);
				} catch (Exception e) {
					// ignore
				}
			}
		}
		
		entity.deleteProperty(this.sourcePredicate);
	}
	
	protected Entity createNewEventEntity(final Entity parentEntity, final int index) {
		Entity establishedEntity = new Entity(false);
		establishedEntity.setSubjectUri(parentEntity.getSubjectUri() + this.enitySuffix + index);
		
		establishedEntity.addTriple(RDF.type.getURI(), new ResourceImpl(this.targetPredicate));
		establishedEntity.addTriple(RDF.type.getURI(), new ResourceImpl(W3COrg.ChangeEvent));
		
		parentEntity.addTriple(W3COrg.resultedFrom, establishedEntity.getSubjectUriObject());
		parentEntity.addSubEntity(establishedEntity);
		
		//entity.addTriple(CorpDbpedia.prefixOntology + "hasIPODate", ipoDate);
		parentEntity.addTriple(W3COrg.resultedFrom, establishedEntity.getSubjectUriObject());
		establishedEntity.addTriple(W3COrg.resultingOrganization, parentEntity.getSubjectUriObject());
		parentEntity.addSubEntity(establishedEntity);
		
		return establishedEntity;
	}

}
