package de.uni.leipzig.sdw.dbpedia.slicing

import java.io.Closeable
import java.nio.file.Paths

import com.google.common.base.Stopwatch
import grizzled.slf4j.Logging

/**
  * Created by Markus Ackermann.
  * No rights reserved.
  */
package object util {

  type IRIStr = String


  val INSTANCE_TYPES_EN = "/opt/datasets/dbpedia/2015-03/instance-types_en.nt"
  val COMBINED_EN = "/opt/datasets/dbpedia/2015-03/combined.nt"
  val INSTANCE_TYPES_DE_LOC = "/opt/datasets/dbpedia/2015-04-de-loc/instance-types_de.nt"

  implicit class ClosableOps[C <: AutoCloseable](val self: C) {

    def loanTo[T](operation: C => T) = try {
      operation(self)
    } finally {
      self.close()
    }
  }

  trait DebugDurations { this: Logging =>

    def debugDuration[T](workDescription: String)(work: => T) = {
      val sw = Stopwatch.createStarted()
      val res = work
      sw.stop()
      debug(s"$workDescription took $sw")
      res
    }
  }
}
