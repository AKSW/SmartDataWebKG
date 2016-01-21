package de.uni.leipzig.sdw.dbpedia.slicing

/**
  * Created by Markus Ackermann.
  * No rights reserved.
  */


import java.io.InputStream
import java.nio.file.{Path, Files}

import com.hp.hpl.jena.graph.Graph
import com.hp.hpl.jena.query.Query
import de.uni.leipzig.sdw.dbpedia.slicing.config.SliceConfig
import de.uni.leipzig.sdw.dbpedia.slicing.rdf.JenaBanana
import de.uni.leipzig.sdw.dbpedia.slicing.util.{IRIStr, DebugDurations}
import grizzled.slf4j.Logging
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.w3.banana.Prefix
import de.uni.leipzig.sdw.dbpedia.slicing.util._


import scala.io.{Codec, Source}

/**
  * Created by Markus Ackermann.
  * No rights reserved.
  */

trait DBpediaOntologyQuerying extends JenaBanana with DebugDurations with Logging {
  this: JenaBanana =>

  lazy val prefixes = Seq(
    Prefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
    Prefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#"),
    Prefix("owl", "http://www.w3.org/2002/07/owl#"),
    Prefix("xsd", "http://www.w3.org/2001/XMLSchema#"),
    Prefix("dbp", "http://dbpedia.org/resource/"),
    Prefix("dbo", "http://dbpedia.org/ontology/")
  )

  val eventByPathBGP = "{ ?event rdf:type/rdfs:subClassOf* dbo:Event. }"

  val personByPathBGP = "{ ?event rdf:type/rdfs:subClassOf* dbo:Event. }"

  def subClassesQuery(classURI: String) = qu(s"SELECT ?class { ?class rdfs:subClassOf* $classURI }")

  lazy val companyClassesQuery = subClassesQuery("dbo:Company")

  lazy val eventClassesQuery = subClassesQuery("dbo:Event")

  lazy val personClassesQuery = subClassesQuery("dbo:Event")

  lazy val placeClassesQuery = subClassesQuery("dbo:Place")

  protected[sdw] def qu(quStr: String) = sparqlOps.parseSelect(quStr.stripMargin, prefixes).get
}


trait DBOTypeHierachies extends DBpediaOntologyQuerying with SliceConfig {

  import sparqlGraph._
  import sparqlOps._

  protected[sdw] lazy val ontGraph: Rdf#Graph = {

    def parseStream(is: InputStream): Graph = debugDuration("reading ontology graph") {
      ntriplesReader.read(is, "http://dbpedia.org/ontology/").get
    }

    def fromClassPath = {
      val bzipStream = getClass.getResourceAsStream("dbpedia_2015-04.nt.bz2")
      new CompressorStreamFactory().createCompressorInputStream(bzipStream).loanTo(parseStream)
    }

    def hasCompressionExtension = externalDBpediaOntologyPath.fold(false) { path =>
      val compressedRegex = """\.(gz)|(gzip)|(bz2)|(bzip2)$""".r

      compressedRegex.findFirstIn(path.getFileName.toString).isDefined
    }

    externalDBpediaOntologyPath.fold(fromClassPath) { path =>
      Files.newInputStream(path) loanTo { inputStream =>
        if(hasCompressionExtension) {
          new CompressorStreamFactory().createCompressorInputStream(inputStream).loanTo(parseStream)
        } else {
          inputStream.loanTo(parseStream)
        }
      }
    }
  }

  protected[sdw] def classSolutions(qu: Query): Set[IRIStr] = {
    executeSelect(ontGraph, qu, Map.empty) map { result =>
      solutionIterator(result).map(_.getResource("class").toString).toSet
    } get
  }

  lazy val eventSubClassIRIs = classSolutions(eventClassesQuery)

  lazy val companySubClassIRIs = classSolutions(companyClassesQuery)

  lazy val personSubClasseIRIs = classSolutions(personClassesQuery)

  lazy val placeSubClassIRIs = classSolutions(placeClassesQuery)
}
