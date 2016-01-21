package de.uni.leipzig.sdw.dbpedia.slicing.rdf

import java.io.StringReader

import scala.collection.convert.decorateAll._

import com.hp.hpl.jena.graph.Triple
import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.flink.api.scala._
import org.apache.flink.core.fs.FileSystem.WriteMode
import org.apache.jena.riot.{RDFLanguages, RDFDataMgr}

/**
  * Created by Markus Ackermann.
  * No rights reserved.
  */
trait NTriplesWindowedIO {

  def readTripleLines(lines: Iterator[String]): Iterator[Triple]

  def serializeTripleLines(triples: Iterator[Triple]): Iterator[String]

  val READ_CHUNK_SIZE = 10000
  val SERIALISATION_CHUNK_SIZE = 10000
}

trait RDFManagerNTriplesIO extends NTriplesWindowedIO with JenaBanana {

  override def readTripleLines(lines: Iterator[String]): Iterator[Triple] = {

    lines.grouped(READ_CHUNK_SIZE) map { tripleLines =>
      val reader = new StringReader(tripleLines.mkString("\n"))
      val graph = ops.makeEmptyMGraph()

      RDFDataMgr.read(graph, reader, "http://synthetic.base/", RDFLanguages.NTRIPLES)

      ops.getTriples(graph)
    } flatten
  }

  override def serializeTripleLines(triples: Iterator[Triple]): Iterator[String] = {
    triples.grouped(SERIALISATION_CHUNK_SIZE) map { tripleChunk =>
      val graph = ops.makeEmptyMGraph()
      tripleChunk foreach (graph.add)
      val outputStream = new ByteArrayOutputStream()
      RDFDataMgr.writeTriples(outputStream, tripleChunk.toIterator.asJava)
      outputStream.toString
    }
  }
}

trait BananaNTriplesIO extends NTriplesWindowedIO with JenaBanana {

  override def readTripleLines(lines: Iterator[String]): Iterator[Triple] = {

    lines.grouped(READ_CHUNK_SIZE) map { tripleLines =>
      val reader = new StringReader(tripleLines.mkString("\n"))

      val graph = ntriplesReader.read(reader, "http://synthetic.base/").get
      ops.getTriples(graph)
    } flatten
  }

  override def serializeTripleLines(triples: Iterator[Triple]): Iterator[String] = {
    triples.grouped(SERIALISATION_CHUNK_SIZE) map { tripleChunk =>
      val graph = ops.makeEmptyMGraph()
      tripleChunk foreach (graph.add)

      val outputStream = new ByteArrayOutputStream()
      ntriplesWriter.write(graph, outputStream, "http://synthetic.base/")
      outputStream.toString
    }
  }
}