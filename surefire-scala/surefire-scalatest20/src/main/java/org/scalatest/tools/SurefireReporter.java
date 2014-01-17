package org.scalatest.tools;

import org.apache.maven.surefire.report.*;
import org.scalatest.Reporter;
import org.scalatest.events.*;
import org.scalatest.junit.JUnitWrapperSuite;
import scala.Option;
import scala.collection.JavaConversions;

import java.util.List;

/**
 * @author David Pratt (dpratt@vast.com)
 */
public class SurefireReporter implements Reporter {

    private final RunListener listener;

    public SurefireReporter(RunListener listener) {
        this.listener = listener;
    }

    /**
     * Invoked to report an event that subclasses may wish to report in some way to the user.
     *
     * @param event the event being reported
     */
    @Override
    public void apply(Event event) {
        if(event instanceof TestStarting) {
            TestStarting e = (TestStarting)event;
            listener.testStarting(createEntry(getOrElse(e.suiteClassName(), e.suiteName()), e.testName(), e.ordinal(), null, null, null));
        } else if(event instanceof TestSucceeded) {
            TestSucceeded e = (TestSucceeded)event;
            Integer duration = getDuration(e.duration());
            listener.testSucceeded(createEntry(getOrElse(e.suiteClassName(), e.suiteName()), e.testName(), e.ordinal(), duration, null, null));
        } else if(event instanceof TestFailed) {
            TestFailed e = (TestFailed)event;
            Integer duration = getDuration(e.duration());
            listener.testFailed(createEntry(getOrElse(e.suiteClassName(), e.suiteName()), e.testName(), e.ordinal(), duration, e.message(), orNull(e.throwable())));
        } else if(event instanceof TestCanceled) {
            TestCanceled e = (TestCanceled)event;
            String name = getOrElse(e.suiteClassName(), e.suiteName());
            listener.testError(createEntry(name, name, e.ordinal(), getDuration(e.duration()), e.message(), orNull(e.throwable())));
        } else if(event instanceof TestIgnored) {
            TestIgnored e = (TestIgnored)event;
            listener.testSkipped(testSkipped(getOrElse(e.suiteClassName(), e.suiteName()), e.testName(), e.ordinal()));
        } else if(event instanceof TestPending) {
            TestPending e = (TestPending)event;
            listener.testSkipped(testSkipped(getOrElse(e.suiteClassName(), e.suiteName()), e.testName(), e.ordinal()));
        } else if(event instanceof SuiteStarting) {
            SuiteStarting e = (SuiteStarting)event;
            listener.testSetStarting(testSetEntry(getOrElse(e.suiteClassName(), e.suiteName()), e.ordinal(), null));
        } else if(event instanceof SuiteCompleted) {
            SuiteCompleted e = (SuiteCompleted)event;
            listener.testSetCompleted(testSetEntry(getOrElse(e.suiteClassName(), e.suiteName()), e.ordinal(), getDuration(e.duration())));
        } else if(event instanceof SuiteAborted) {
            SuiteAborted e = (SuiteAborted)event;
            String name = getOrElse(e.suiteClassName(), e.suiteName());
            listener.testError(createEntry(name, name, e.ordinal(), getDuration(e.duration()), e.message(), orNull(e.throwable())));
        } else {
            //just let it drop
            //TODO: Log it somehow?
        }
    }

    private ReportEntry createEntry(String source, String testName, Ordinal ordinal, Integer elapsed, String message, Throwable throwable) {
        String sourceName = mangleJUnitSourceName(source, ordinal);

        if(throwable != null) {
            return CategorizedReportEntry.reportEntry(sourceName, testName, null, new PojoStackTraceWriter(source, testName, throwable), elapsed, message);
        }
        return CategorizedReportEntry.reportEntry(sourceName, testName, null, null, elapsed, message);
    }

    private ReportEntry testSkipped(String source, String name, Ordinal ordinal) {
        String sourceName = mangleJUnitSourceName(source, ordinal);

        return CategorizedReportEntry.ignored(sourceName, name, null);
    }

    private ReportEntry testSetEntry(String source, Ordinal ordinal, Integer duration) {
        String sourceName = mangleJUnitSourceName(source, ordinal);
        return new SimpleReportEntry( sourceName, sourceName, duration );
    }

    private <T> T getOrElse(Option<T> option, T defaultVal) {
        if(option.isEmpty()) {
            return defaultVal;
        } else {
            return option.get();
        }
    }

    private <T> T orNull(Option<T> option) {
        if(option.isEmpty()) {
            return null;
        } else {
            return option.get();
        }
    }

    private Integer getDuration(Option<Object> option) {
        if(option.isEmpty()) {
            return null;
        } else {
            return ((Long)option.get()).intValue();
        }
    }

    private static final String JUNIT_WRAPPER_NAME = JUnitWrapperSuite.class.getName();

    //JUniteWrapper tests all come in with the source name as 'JUnitWrapperSuite'.
    //This will mess up surefire reports, so this just tacks on a unique ID to each one.
    private String mangleJUnitSourceName(String source, Ordinal ordinal) {
        if (JUNIT_WRAPPER_NAME.equals(source)) {
            List<Object> values = JavaConversions.asJavaList(ordinal.toList());
            if(values.size() > 1) {
                return source + values.get(1);
            } else {
                return source;
            }
        } else {
            return source;
        }
    }

}
