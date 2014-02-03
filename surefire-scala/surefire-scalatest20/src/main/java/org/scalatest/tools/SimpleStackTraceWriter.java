package org.scalatest.tools;

import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.SmartStackTraceParser;
import org.apache.maven.surefire.report.StackTraceWriter;

import java.io.PrintWriter;
import java.io.StringWriter;

public class SimpleStackTraceWriter implements StackTraceWriter {

    private final Throwable t;
    protected final String testClass;
    protected final String suiteName;
    protected final String testName;

    public SimpleStackTraceWriter( String testClass, String suiteName, String testName, Throwable t ) {
        this.testClass = testClass;
        this.suiteName = suiteName;
        this.testName = testName;
        this.t = t;
    }

    @Override
    public String smartTrimmedStackTrace() {
        String message = suiteName + " -> " + testName;
        if(t.getMessage() != null) {
            message = message + " : " + t.getMessage();
        }
        if("org.scalatest.exceptions.TestFailedException".equals( t.getClass().getName() )) {
            if(t.getCause() != null && t.getCause().getMessage() != null) {
                message = message + " : " + t.getCause().getMessage();
            }
        }
        return message;
    }

    @Override
    public String writeTraceToString() {
        StringWriter w = new StringWriter();
        if ( t != null )
        {
            t.printStackTrace( new PrintWriter( w ) );
            w.flush();
        }
        return w.toString();

    }

    @Override
    public String writeTrimmedTraceToString()
    {
        if(testClass != null) {
            return SmartStackTraceParser.innerMostWithFocusOnClass(t, testClass);
        } else {
            return "";
        }
    }

    @Override
    public SafeThrowable getThrowable()
    {
        return new SafeThrowable( t );
    }

}
