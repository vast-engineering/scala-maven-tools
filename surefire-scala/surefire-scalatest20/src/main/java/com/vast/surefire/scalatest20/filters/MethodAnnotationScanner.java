package com.vast.surefire.scalatest20.filters;

import org.apache.maven.surefire.util.ScannerFilter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 *
 * @author David Pratt (dpratt@vast.com)
 */
public class MethodAnnotationScanner implements ScannerFilter {

    private final Class<? extends Annotation> annotationClass;

    public MethodAnnotationScanner(Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
    }

    public boolean accept(Class testClass) {
        if(testClass == null) {
            return false;
        }
        boolean annotationFound = false;
        for(Method m : testClass.getDeclaredMethods()) {
            for(Annotation a : m.getDeclaredAnnotations()) {
                if(a.annotationType().equals(annotationClass)) {
                    annotationFound = true;
                    break;
                }
            }
        }
        if(!annotationFound) {
            return false;
        }

        return !Modifier.isAbstract(testClass.getModifiers());
    }
}
