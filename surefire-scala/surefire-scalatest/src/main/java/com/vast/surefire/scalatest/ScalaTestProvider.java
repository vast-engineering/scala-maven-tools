package com.vast.surefire.scalatest;

import com.vast.surefire.scalatest.filters.MethodAnnotationScanner;
import com.vast.surefire.scalatest.filters.ScalaTestScanner;
import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.ScannerFilter;
import org.apache.maven.surefire.util.TestsToRun;
import org.scalatest.SurefireRunner;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author David Pratt (dpratt@vast.com)
 */
public class ScalaTestProvider extends AbstractProvider {

    private final ProviderParameters providerParameters;
    private final ParametersParser parametersParser;
    private final Class<? extends Annotation> junitAnnotation;

    public ScalaTestProvider(ProviderParameters providerParameters) {
        this.providerParameters = providerParameters;
        this.parametersParser = new ParametersParser(providerParameters.getProviderProperties());

        Class annoClass;
        try {
            annoClass = providerParameters.getTestClassLoader().loadClass("org.junit.Test");
        } catch(ClassNotFoundException e) {
            annoClass = null;
        }
        junitAnnotation = annoClass;
    }

    @Override
    public Iterator getSuites() {
        throw new UnsupportedOperationException("Non-default forking is not supported with this provider.");
    }

    @Override
    public RunResult invoke(Object forkTestSet) throws TestSetFailedException, ReporterException, InvocationTargetException {
        SurefireRunner.run(getScalaTests(), getJunitTests(), providerParameters.getReporterFactory(),
                providerParameters.getTestClassLoader(), parametersParser.shouldExecuteParallel(), parametersParser.getThreadCount());
        return providerParameters.getReporterFactory().close();
    }

    private List<String> getScalaTests() {
        return scanClasspath(new ScalaTestScanner());
    }

    private List<String> getJunitTests() {
        return scanClasspath(new MethodAnnotationScanner(junitAnnotation));
    }

    private List<String> scanClasspath(ScannerFilter filter) {
        TestsToRun testsToRun= providerParameters
                .getRunOrderCalculator()
                .orderTestClasses(providerParameters.getScanResult().applyFilter(filter, providerParameters.getTestClassLoader()));
        ArrayList<String> retval = new ArrayList<String>();
        Iterator it = testsToRun.iterator();
        while(it.hasNext()) {
            retval.add(((Class)it.next()).getName());
        }
        return retval;
    }


}
