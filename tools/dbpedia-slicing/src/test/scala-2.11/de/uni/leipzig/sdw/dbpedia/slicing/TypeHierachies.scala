package de.uni.leipzig.sdw.dbpedia.slicing

import java.nio.file.Path

import org.scalatest.{Matchers, FlatSpec, FunSuite}

/**
  * Created by Markus Ackermann.
  * No rights reserved.
  */
class DBOTypeHierachiesTest extends FlatSpec with Matchers{

  new DBOTypeHierachies {
    override def sliceDestinationDir: Path = ???

    override def dbpediaDumpDir: Path = ???

    "SPARQL query components for the DBpedia ontology" should
      "retrieve some sub-classes for dbo:Company, dbo:Event, dbo:Person, dbo:Place" in {

      Set(companySubClassIRIs, eventSubClassIRIs, personSubClasseIRIs, personSubClasseIRIs) foreach {
        _ should not be empty
      }
    }
  }
}
