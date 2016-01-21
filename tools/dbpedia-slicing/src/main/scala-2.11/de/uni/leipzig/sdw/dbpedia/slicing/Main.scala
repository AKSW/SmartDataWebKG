package de.uni.leipzig.sdw.dbpedia.slicing

import java.nio.file.Paths

import de.uni.leipzig.sdw.dbpedia.slicing.config.SliceConfig
import de.uni.leipzig.sdw.dbpedia.slicing.rdf.RDFManagerNTriplesIO
import de.uni.leipzig.sdw.dbpedia.slicing.util.SliceOps
import grizzled.slf4j.Logging
import org.apache.flink.api.scala._

import scala.language.postfixOps

trait AKSWNC7ConfigCanonicalizedDE extends SliceConfig {

  override val dbpediaDumpDir = Paths.get("/data/dbpedia-dumps/de-2015-03-en-uris")
  override val sliceDestinationDir = Paths.get("/data/dbp-slicing/de-en-uris")
  override val distributionDescriptionInfix: String = "_de_en-uris"
}

object CompanyFacts extends RDFManagerNTriplesIO with DBOTypeHierachies with Logging with SliceOps
  with AKSWNC7ConfigCanonicalizedDE {

  lazy val jobName: String = "company facts"

  lazy val env = ExecutionEnvironment.getExecutionEnvironment

  def main(args: Array[String]) {

    val env = ExecutionEnvironment.getExecutionEnvironment

    lazy val companyInstances = selectViaSubClasses(companySubClassIRIs)
    val companyFacts = factsForSubjects(companyInstances)
    writeTripleDataset(companyFacts, sinkPathStr("company-facts"))

    env.execute(jobName)
  }
}

object CombinedSlice extends Logging with RDFManagerNTriplesIO with DBOTypeHierachies with SliceOps
  with AKSWNC7ConfigCanonicalizedDE {

  val jobName = "combined-slice"

  lazy val env = ExecutionEnvironment.getExecutionEnvironment

  lazy val companyInstances = selectViaSubClasses(companySubClassIRIs)

  lazy val eventInstances = selectViaSubClasses(eventSubClassIRIs)

  lazy val personInstances = selectViaSubClasses(personSubClasseIRIs)

  lazy val placeInstances = selectViaSubClasses(placeSubClassIRIs)

  def main(args: Array[String]) {

    val companyFacts = factsForSubjects(companyInstances)
    writeTripleDataset(companyFacts, sinkPathStr("company-facts"))

    val companyEventFacts = mentionedEntitiesFacts(companyFacts, eventInstances)
    writeTripleDataset(companyEventFacts, sinkPathStr("company-event-facts"))

    val companyPersonFacts = mentionedEntitiesFacts(companyFacts, personInstances)
    writeTripleDataset(companyPersonFacts, sinkPathStr("company-person-facts"))

    val placeFacts = mentionedEntitiesFacts(companyFacts, placeInstances)
    writeTripleDataset(placeFacts, sinkPathStr("company-place-facts"))

    env.execute(jobName)
  }
}

