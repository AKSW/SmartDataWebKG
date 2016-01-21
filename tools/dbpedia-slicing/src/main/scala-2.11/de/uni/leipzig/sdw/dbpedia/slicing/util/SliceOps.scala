package de.uni.leipzig.sdw.dbpedia.slicing.util

import java.io.File

import com.hp.hpl.jena.graph.Triple
import com.hp.hpl.jena.vocabulary.RDF
import de.uni.leipzig.sdw.dbpedia.slicing.config.SliceConfig
import de.uni.leipzig.sdw.dbpedia.slicing.rdf.NTriplesWindowedIO
import org.apache.flink.api.scala._
import org.apache.flink.core.fs.FileSystem.WriteMode

/**
  * Created by Markus Ackermann.
  * No rights reserved.
  */
trait SliceOps extends SliceConfig with NTriplesWindowedIO  {
  this: { def env: ExecutionEnvironment } =>

  lazy val combinedFacts = env.readTextFile(dumpFilePathStr(combinedDataFileName)).mapPartition(readTripleLines _)

  lazy val instanceTypes = env.readTextFile(dumpFilePathStr(instanceTypesFileName)).mapPartition(readTripleLines _)

  type IRIDataset = DataSet[Tuple1[IRIStr]]

  def selectViaSubClasses(subClassIRIs: Set[IRIStr]): DataSet[Tuple1[IRIStr]] = {

    instanceTypes filter { triple =>
      (subClassIRIs contains triple.getObject.getURI) &&
        (triple.getPredicate.getURI == typePropertyStr)
    } map { triple =>
      Tuple1(triple.getSubject.getURI)
    } distinct
  }

  def factsForSubjects(subjects: IRIDataset): DataSet[Triple] = {
    val join = subjects joinWithHuge combinedFacts where 0 equalTo { _.getSubject.getURI }

    join apply { (_, triple) => triple }
  }

  def mentionedEntitiesFacts(statements: DataSet[Triple], entitySubjects: IRIDataset): DataSet[Triple] = {
    val join = statements.filter(_.getObject.isURI) join entitySubjects where { _.getObject.getURI } equalTo 0

    val mentionedInstances = join apply { (_, subjUri) => subjUri } distinct

    factsForSubjects(mentionedInstances)
  }

  def writeTripleDataset(tds: DataSet[Triple], filePath:String,
                         writeMode: WriteMode = WriteMode.OVERWRITE,
                         parallelism: Int = 1) = {

    val sink = tds.mapPartition(serializeTripleLines _).writeAsText(filePath, writeMode)

    if(parallelism > 0) {
      sink.setParallelism(parallelism)
    }

    sink
  }

  protected val typePropertyStr = RDF.`type`.getURI

  protected def dumpFilePathStr(fileName: String) =
    s"$dbpediaDumpDir${File.separator}$fileName"

  protected def sinkPathStr(contentDesc: String, distDesc: String = distributionDescriptionInfix, ext:String = "nt") =
    s"$sliceDestinationDir${File.separator}$contentDesc$distDesc.$ext"
}
