package com.vast.scalatestprovider

import org.scalatest.matchers.ShouldMatchers
import org.scalatest._
import com.vast.surefire.scalatest.filters.ScalaTestScanner
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.junit.Test

@RunWith(classOf[JUnitRunner])
class ScalaTestScannerTest extends WordSpec with ShouldMatchers {

  "A ScalaTestScanner" should {
    "recognize ScalaTest Suites" in {
      class DummyFunSuite extends FunSuite
      class DummyWordSpec extends WordSpec
      class DummyFunSpec extends FunSpec
      class DummyFlatSpec extends FlatSpec
      class DummyFreeSpec extends FreeSpec
      class DummyFeatureSpec extends FeatureSpec

      val specClasses = Set(
        classOf[DummyFunSuite],
        classOf[DummyWordSpec],
        classOf[DummyFunSpec],
        classOf[DummyFlatSpec],
        classOf[DummyFreeSpec],
        classOf[DummyFeatureSpec]
      )
      val scanner = new ScalaTestScanner
      specClasses.foreach { clazz =>
        scanner.accept(clazz) should be (true)
      }
    }

    "reject invalid classes" in {
      class JUnitTestClass {
        @Test
        def dummyTest() {
          assert(true)
        }
      }

      val classes = Set(
        classOf[JUnitTestClass],
        classOf[Object],
        classOf[Int],
        classOf[String]
      )
      val scanner = new ScalaTestScanner
      classes.foreach { clazz =>
        scanner.accept(clazz) should be (false)
      }
    }
  }

}