package com.vast.scalatestprovider

import org.scalatest.{Matchers, WordSpec}
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.vast.surefire.scalatest20.filters.MethodAnnotationScanner

@RunWith(classOf[JUnitRunner])
class MethodAnnotationScannerTest extends WordSpec with Matchers {

  "A MethodAnnotationScanner set up for JUnit4 annotations" should {
    "recognize a class with annotated methods" in {
      class SampleJUnit {
        @Test
        def simpleTest() {
          assertTrue("Hi there.", true)
        }
      }
      val scanner = new MethodAnnotationScanner(classOf[Test])
      scanner.accept(classOf[SampleJUnit]) should be (true)
    }
    "reject null classes" in {
      val scanner = new MethodAnnotationScanner(classOf[Test])
      scanner.accept(null) should be (false)
    }
    "reject abstract classes" in {
      abstract class AbstractJUnit {
        @Test
        def simpleTest() {
          assertTrue("Hi there.", true)
        }
      }
      val scanner = new MethodAnnotationScanner(classOf[Test])
      scanner.accept(classOf[AbstractJUnit]) should be (false)
    }
  }
}