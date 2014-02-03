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
            String name = mangleJUnitSourceName(getOrElse(e.suiteClassName(), e.suiteName()), e.ordinal());
            ReportEntry report = new SimpleReportEntry( name, e.testName() );
            listener.testStarting(report);
        } else if(event instanceof TestSucceeded) {
            TestSucceeded e = (TestSucceeded)event;
            String name = mangleJUnitSourceName(getOrElse(e.suiteClassName(), e.suiteName()), e.ordinal());
            ReportEntry report = new SimpleReportEntry( name, e.testName() );
            listener.testSucceeded( report );
        } else if(event instanceof TestFailed) {
            TestFailed e = (TestFailed)event;
            String name = mangleJUnitSourceName(getOrElse(e.suiteClassName(), e.suiteName()), e.ordinal());
            //ReportEntry report = new SimpleReportEntry( name, e.testName() );

            ReportEntry report = SimpleReportEntry.withException(name, e.testName(),
                    new SimpleStackTraceWriter(orNull(e.suiteClassName()),
                            e.suiteName(),
                            e.testName(),
                            orNull(e.throwable() ) ) );
            listener.testFailed( report );
//
//
//
//            Integer duration = getDuration(e.duration());
////            listener.testFailed(createEntry(getOrElse(e.suiteClassName(), e.suiteName()), e.testName(), e.ordinal(), duration, e.message(), orNull(e.throwable())));
//            String suiteName = e.suiteName();
//            String testName = e.testName();
//            Throwable throwable = orNull(e.throwable());
//            ReportEntry entry = createEntry(suiteName, testName, e.ordinal(), duration, e.message(), throwable);
//            listener.testFailed(entry);
        } else if(event instanceof TestCanceled) {
            TestCanceled e = (TestCanceled)event;
            String name = mangleJUnitSourceName(getOrElse(e.suiteClassName(), e.suiteName()), e.ordinal());
            ReportEntry report = SimpleReportEntry.withException(name, e.testName(),
                    new SimpleStackTraceWriter(orNull(e.suiteClassName()),
                            e.suiteName(),
                            e.testName(),
                            orNull(e.throwable() ) ) );
            listener.testError(report);
        } else if(event instanceof TestIgnored) {
            TestIgnored e = (TestIgnored)event;
            String name = mangleJUnitSourceName(getOrElse(e.suiteClassName(), e.suiteName()), e.ordinal());
            ReportEntry report = new SimpleReportEntry( name, e.testName() );
            listener.testSkipped( report );
        } else if(event instanceof TestPending) {
            TestPending e = (TestPending)event;
            String name = mangleJUnitSourceName(getOrElse(e.suiteClassName(), e.suiteName()), e.ordinal());
            ReportEntry report = new SimpleReportEntry( name, e.testName() );
            listener.testSkipped( report );
        } else if(event instanceof SuiteStarting) {
            SuiteStarting e = (SuiteStarting)event;
            String name = mangleJUnitSourceName(getOrElse(e.suiteClassName(), e.suiteName()), e.ordinal());
            ReportEntry report = new SimpleReportEntry( name, name );
            listener.testSetStarting(report);
        } else if(event instanceof SuiteCompleted) {
            SuiteCompleted e = (SuiteCompleted)event;
            String name = mangleJUnitSourceName(getOrElse(e.suiteClassName(), e.suiteName()), e.ordinal());
            ReportEntry report = new SimpleReportEntry( name, name );
            listener.testSetCompleted(report);
        } else if(event instanceof SuiteAborted) {
            SuiteAborted e = (SuiteAborted)event;
            String name = mangleJUnitSourceName(getOrElse(e.suiteClassName(), e.suiteName()), e.ordinal());
            ReportEntry report = SimpleReportEntry.withException(name, e.suiteName(),
                    new SimpleStackTraceWriter(orNull(e.suiteClassName()),
                            e.suiteName(),
                            e.suiteName(),
                            orNull(e.throwable() ) ) );
            listener.testError(report);
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
