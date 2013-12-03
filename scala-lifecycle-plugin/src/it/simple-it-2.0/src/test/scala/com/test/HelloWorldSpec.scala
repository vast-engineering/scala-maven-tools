package com.test

import org.scalatest._

class HelloWorldSpec extends WordSpec {

  "The scalatest plugin" should {
    "properly detect and run a test with a Spec naming pattern" in {
      val helloWorld = new HelloWorld
      val input = "Hello World"
      assert(input === helloWorld.echo(input))
    }
  }

}
