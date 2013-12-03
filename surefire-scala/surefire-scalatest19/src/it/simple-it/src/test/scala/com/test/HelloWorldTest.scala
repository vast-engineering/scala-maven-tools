package com.test

import org.scalatest.{BeforeAndAfter, FunSuite}

class HelloWorldTest extends FunSuite {

  test("basic test") {
    val helloWorld = new HelloWorld
    val input = "Hello World"
    assert(input === helloWorld.echo(input))
  }

}