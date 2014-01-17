package com.vast.surefire.scalatest20.filters;

import org.apache.maven.surefire.util.ScannerFilter;

import java.lang.reflect.Modifier;

/**
 * @author David Pratt (dpratt@vast.com)
 */
public class ScalaTestScanner implements ScannerFilter {

    private final Class<org.scalatest.Suite> suiteClass;

    public ScalaTestScanner(Class<org.scalatest.Suite> suiteClass) {
        this.suiteClass = suiteClass;
    }

    public boolean accept(Class testClass) {

        boolean isConcrete = !Modifier.isAbstract(testClass.getModifiers());
        boolean isSuite = suiteClass.isAssignableFrom(testClass);

        return testClass != null && isConcrete && isSuite;
    }

}
