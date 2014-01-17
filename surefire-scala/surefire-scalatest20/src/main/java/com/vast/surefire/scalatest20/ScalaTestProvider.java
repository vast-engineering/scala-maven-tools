package com.vast.surefire.scalatest20;

import com.vast.surefire.scalatest20.filters.MethodAnnotationScanner;
import com.vast.surefire.scalatest20.filters.ScalaTestScanner;
import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.ScannerFilter;
import org.apache.maven.surefire.util.TestsToRun;
import org.scalatest.tools.SurefireRunner;

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
    private final Class<org.junit.Test> junitAnnotation;
    private final Class<org.scalatest.Suite> suiteClass;

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

        Class stClass;
        try {
            stClass = providerParameters.getTestClassLoader().loadClass("org.scalatest.Suite");
        } catch(ClassNotFoundException e) {
            throw new RuntimeException("Cannot find scalatest Suite class.", e);
        }
        suiteClass = stClass;
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
        return scanClasspath(new ScalaTestScanner(suiteClass));
    }

    private List<String> getJunitTests() {
        return scanClasspath(new MethodAnnotationScanner(junitAnnotation));
    }

    private List<String> scanClasspath(ScannerFilter filter) {
        TestsToRun testsToRun= providerParameters
                .getRunOrderCalculator()
                .orderTestClasses(providerParameters.getScanResult().applyFilter(filter, providerParameters.getTestClassLoader()));
        ArrayList<String> retval = new ArrayList<String>();
        for (Class aTestsToRun : testsToRun) {
            retval.add(aTestsToRun.getName());
        }
        return retval;
    }


}
