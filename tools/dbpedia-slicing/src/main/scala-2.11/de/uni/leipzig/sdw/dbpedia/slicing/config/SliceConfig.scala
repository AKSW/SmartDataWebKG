package de.uni.leipzig.sdw.dbpedia.slicing.config

import java.nio.file.{Path, Paths}

/**
  * Created by Markus Ackermann.
  * No rights reserved.
  */
trait SliceConfig {

  /** place to look for uncompressed dbpedia NTriples data to load and slice*/
  def dbpediaDumpDir:Path

  /** output destination for slice result files (NTriples)*/
  def sliceDestinationDir: Path

  /** filename of the input NTriples file containing the concatenation of all DBpedia dump files the slice
    * should be cut from */
  val combinedDataFileName: String = "combined.nt"

  /** filename of the input NTriples file containing mapping-based ontology types for DBpedia resources */
  val instanceTypesFileName: String = "instance-types.nt"

  /** a filename infix to distinguish slice results, e.g. by dbpedia language and URI scheme (_de_en-uris) */
  val distributionDescriptionInfix = ""

  /** if defined, this path is used to load the DBpedia ontology to query for type hierarchies instead of using
    * the bundled DBO file (reversion 2015/04) */
  val externalDBpediaOntologyPath: Option[Path] = None
}
