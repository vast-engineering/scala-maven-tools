package com.vast.surefire.scalatest19.filters;

import org.apache.maven.surefire.util.ScannerFilter;
import org.scalatest.Suite;

import java.lang.reflect.Modifier;

/**
 * @author David Pratt (dpratt@vast.com)
 */
public class ScalaTestScanner implements ScannerFilter {

    public boolean accept(Class testClass) {
        return testClass != null
                && !Modifier.isAbstract(testClass.getModifiers())
                && Suite.class.isAssignableFrom(testClass);
    }

}
