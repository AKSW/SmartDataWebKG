package de.uni.leipzig.sdw.dbpedia.slicing.rdf

import org.w3.banana._
import org.w3.banana.jena.{Jena, JenaModule}


trait BananaModules extends RDFOpsModule with RecordBinderModule
with TurtleReaderModule with RDFXMLReaderModule with NTriplesReaderModule
with XmlQueryResultsReaderModule with NTriplesWriterModule

trait JenaBanana extends BananaModules with JenaModule {

  override type Rdf = Jena
}
