package org.scalatest;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.vast.surefire.scalatest19.WrappedCheckedException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.scalatest.events.Ordinal;
import org.scalatest.events.RunAborted;
import org.scalatest.events.RunCompleted;
import org.scalatest.events.RunStarting;
import org.scalatest.junit.JUnitWrapperSuite;
import scala.Option;
import scala.collection.JavaConversions;
import scala.collection.immutable.Map$;
import scala.collection.immutable.Nil$;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A ScalaTest runner compatible with Surefire
 *
 * @author David Pratt (dpratt@vast.com)
 */
public class SurefireRunner {

    //NOTE: This class is VERY ugly, but there's not much I can do about this. This class needs to be
    //independent of any scala version and thus needs to be written in Java. Unfortunately, calling idiomatic
    //Scala code (like ScalaTest) from Java is ugly at best.

    private static final Stopper NULL_STOPPER = new Stopper() {
        @Override
        public boolean apply() {
            return false;
        }
    };

    SurefireRunner() {

    }

    public static void run(List<String> scalaTests, List<String> junitTests, ReporterFactory reporterFactory,
                           final ClassLoader loader, boolean isConcurrent, int threadCount) {

        long startTime = System.currentTimeMillis();
        Tracker tracker = new Tracker(new Ordinal(1));
        scala.collection.immutable.Map<String, Object> configMap = Map$.MODULE$.empty();

        //U G L Y
        Filter filter = Filter$.MODULE$.apply();
        //OH MY GOD SO UGLY
        //Unfortunately, ScalaTest requires a Scala List as a constructor arg - this
        //is really the only way to construct one in Java.
        scala.collection.immutable.List reporters = Nil$.MODULE$;
        reporters = reporters.$colon$colon(new SurefireReporter(reporterFactory.createReporter()));
        //reporters = reporters.$colon$colon(new StandardOutReporter(false, true, false, true));
        DispatchReporter reporter = new DispatchReporter(reporters);

        try {
            List<? extends Suite> scalaTestSuites;
            try {
                scalaTestSuites = Lists.transform(scalaTests, new Function<String, Suite>() {
                    @Override
                    public Suite apply(String clazzName) {
                        try {
                            Class<?> clazz = loader.loadClass(clazzName);
                            WrapWith wrapWith = clazz.getAnnotation(WrapWith.class);
                            if (wrapWith == null) {
                                return (Suite) clazz.newInstance();
                            } else {
                                Class<? extends Suite> suiteClazz = wrapWith.value();
                                Constructor ctor = null;
                                for (Constructor c : suiteClazz.getDeclaredConstructors()) {
                                    Class[] types = c.getParameterTypes();
                                    if (types.length == 1 && types[0] == Class.class) {
                                        ctor = c;
                                    }
                                }
                                if (ctor == null) {
                                    throw new RuntimeException("The class " + suiteClazz.getName() + " must have a public constructor with one argument of type Class to use the WrapWith annotation.");
                                }
                                return (Suite) ctor.newInstance(clazz);
                            }
                        } catch (Exception e) {
                            throw new WrappedCheckedException(e);
                        }
                    }
                });
            } catch(WrappedCheckedException e) {
                throw e.getWrapped();
            }

            List<JUnitWrapperSuite> junitSuites = Lists.transform(junitTests, new Function<String, JUnitWrapperSuite>() {
                @Override
                public JUnitWrapperSuite apply(String input) {
                    return new JUnitWrapperSuite(input, loader);
                }
            });

            ImmutableList<? extends Suite> suites = new ImmutableList.Builder<Suite>().addAll(scalaTestSuites).addAll(junitSuites).build();
            int expectedCount = 0;
            for (Suite suite : suites) {
                expectedCount += suite.expectedTestCount(filter);
            }

            reporter.apply(RunStarting.apply(tracker.nextOrdinal(), expectedCount, configMap));

            if (isConcurrent) {
                ExecutorService execSvc = Executors.newFixedThreadPool(threadCount);
                try {
                    org.scalatest.tools.ConcurrentDistributor distributor =
                            new org.scalatest.tools.ConcurrentDistributor(reporter, NULL_STOPPER, filter, configMap, execSvc);
                    for (Suite suite : suites) {
                        distributor.apply(suite, tracker.nextTracker());
                    }
                    distributor.waitUntilDone();
                } finally {
                    execSvc.shutdown();
                }
            } else {
                for (Suite suite : suites) {
                    org.scalatest.tools.SuiteRunner suiteRunner =
                            new org.scalatest.tools.SuiteRunner(suite, reporter, NULL_STOPPER, filter,
                                    configMap, Option.apply((Distributor)null), tracker);
                    suiteRunner.run();
                }
            }

            Long duration = System.currentTimeMillis() - startTime;
            reporter.apply(RunCompleted.apply(tracker.nextOrdinal(), Option.apply((Object)duration)));
        } catch(InstantiationException e) {
            reporter.apply(RunAborted.apply(tracker.nextOrdinal(), org.scalatest.Resources.apply("cannotInstantiateSuite", JavaConversions.asScalaBuffer(Lists.newArrayList((Object)e.getMessage()))), Option.apply((Throwable) e), Option.apply((Object) (System.currentTimeMillis() - startTime))));
        } catch(IllegalAccessException e) {
            reporter.apply(RunAborted.apply(tracker.nextOrdinal(), org.scalatest.Resources.apply("cannotInstantiateSuite", JavaConversions.asScalaBuffer(Lists.newArrayList((Object)e.getMessage()))), Option.apply((Throwable) e), Option.apply((Object) (System.currentTimeMillis() - startTime))));
        } catch(NoClassDefFoundError e) {
            reporter.apply(RunAborted.apply(tracker.nextOrdinal(), org.scalatest.Resources.apply("cannotLoadClass", JavaConversions.asScalaBuffer(Lists.newArrayList((Object)e.getMessage()))), Option.apply((Throwable) e), Option.apply((Object) (System.currentTimeMillis() - startTime))));
        } catch(Throwable e) {
            reporter.apply(RunAborted.apply(tracker.nextOrdinal(), org.scalatest.Resources.bigProblems(e), Option.apply(e), Option.apply((Object) (System.currentTimeMillis() - startTime))));
        } finally {
            reporter.dispatchDisposeAndWaitUntilDone();
        }
    }
}
