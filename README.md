This is a set of useful tools for Maven and Scala.

# scala-lifecycle-plugin

This is a plugin that adds a new packaging type ('scala-jar') to maven with a better default lifecycle. This lifecycle binds net.alchim31.maven:scala-maven-plugin:compile to the 'compile' phase by default while also retaining an invocation of maven-compiler-plugin. Additionally, com.vast:scala-surefire-maven-plugin:test is bound to the 'test' phase. This allows native and seamless use of ScalaTest in your build.

Here's an example of a minimal POM that will compile all scala (and Java) in src/main/(scala|java) and run any JUnit or ScalaTest suites found in src/test/(scala|java)

````xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.vast</groupId>
    <artifactId>simple-scala-project</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>scala-jar</packaging>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.6</maven.compiler.source>
        <maven.compiler.target>1.6</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.scala-lang</groupId>
            <artifactId>scala-library</artifactId>
            <version>2.10.0</version>
        </dependency>

        <dependency>
            <groupId>org.scalatest</groupId>
            <artifactId>scalatest_2.10</artifactId>
            <version>1.9.1</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
````



# surefire-scala

This is a subclass of the maven-surefire-plugin that adds ScalaTest to the default list of 'known' test providers. If ScalaTest is detected in your test classpath, this provider will be used by default. As a bonus, this provider will also run any JUnit tests it finds in your build. The version of scala and ScalaTest is detected automatically from your build, assuming that there's a dependency to 'scala-library' somewhere in your test classpath.

The plugin is built with the intention of adding more Scala test providers to it eventually - right now, there is only support for ScalaTest, but adding Specs would not be a difficult exercise.
