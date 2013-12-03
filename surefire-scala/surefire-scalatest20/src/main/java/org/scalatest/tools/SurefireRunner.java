package org.scalatest.tools;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.vast.surefire.scalatest20.WrappedCheckedException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.scalatest.*;
import org.scalatest.events.*;
import org.scalatest.junit.JUnitWrapperSuite;
import scala.Option;
import scala.collection.JavaConversions;
import scala.collection.immutable.*;
import scala.collection.immutable.Map;
import scala.collection.immutable.Map$;
import scala.collection.immutable.Set;
import scala.collection.immutable.Set$;
import scala.collection.mutable.*;
import scala.collection.mutable.HashSet;

import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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
            return stopRequested();
        }

        @Override
        public boolean stopRequested() {
            return false;
        }

        @Override
        public void requestStop() {
            //do nothing
        }
    };

    SurefireRunner() {

    }

    public static void run(List<String> scalaTests, List<String> junitTests, ReporterFactory reporterFactory,
                           final ClassLoader loader, boolean isConcurrent, int threadCount) {

        long startTime = System.currentTimeMillis();
        Tracker tracker = new Tracker(new Ordinal(1));
        ConfigMap configMap = new ConfigMap(Map$.MODULE$.<String, Object>empty());

        //U G L Y
        HashSet<String> toIgnore = new HashSet<String>();
        toIgnore.add("org.scalatest.Ignore");
        DynaTags tags = new DynaTags(Map$.MODULE$.<String, Set<String>>empty(), Map$.MODULE$.<String, Map<String, Set<String>>>empty());
        Filter filter = Filter$.MODULE$.apply(Option.apply((Set<String>)null), toIgnore.<String>toSet(), false, tags);

        //OH MY GOD SO UGLY
        //Unfortunately, ScalaTest requires a Scala List as a constructor arg - this
        //is really the only way to construct one in Java.
        scala.collection.immutable.List reporters = Nil$.MODULE$;
        reporters = reporters.$colon$colon(new SurefireReporter(reporterFactory.createReporter()));
        //reporters = reporters.$colon$colon(new StandardOutReporter(false, true, false, true));
        DispatchReporter reporter = new DispatchReporter(reporters, System.err, false, 60000, 60000);

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

            reporter.apply(new RunStarting(tracker.nextOrdinal(), (java.lang.Integer)expectedCount, configMap, Option.<Formatter>apply(null), Option.<Location>apply(null), Option.apply(null),
                    Thread.currentThread().getName(), new Date().getTime()));

            Args testArgs = new Args(reporter, NULL_STOPPER, filter, configMap, Option.apply((Distributor)null), tracker, Set$.MODULE$.<String>empty(),
                    false, Option.apply((DistributedTestSorter)null), Option.apply((DistributedSuiteSorter)null));
            if (isConcurrent) {
                ThreadFactory threadFactory = new ThreadFactory() {
                    final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = defaultThreadFactory.newThread(r);
                        thread.setName(thread.getName().replaceAll(".*-", "ScalaTest-"));
                        return thread;
                    }
                };
                ExecutorService execSvc = Executors.newFixedThreadPool(threadCount, threadFactory);
                try {
                    org.scalatest.tools.ConcurrentDistributor distributor =
                            new org.scalatest.tools.ConcurrentDistributor(testArgs, execSvc);
                    for (Suite suite : suites) {
                        distributor.apply(suite, tracker.nextTracker());
                    }
                    distributor.waitUntilDone();
                } finally {
                    execSvc.shutdown();
                }
            } else {
                for (Suite suite : suites) {
                    ScalaTestStatefulStatus status = new ScalaTestStatefulStatus();
                    SuiteRunner suiteRunner = new SuiteRunner(suite, testArgs, status);
                    suiteRunner.run();
                }
            }

            Long duration = System.currentTimeMillis() - startTime;
            reporter.apply(new RunCompleted(tracker.nextOrdinal(), Option.apply((Object)duration),
                    Option.apply((Summary)null), Option.apply((Formatter)null), Option.apply((Location)null),
                    Option.<Object>apply(null), Thread.currentThread().getName(), new Date().getTime()));
        } catch(InstantiationException e) {
            reporter.apply(new RunAborted(tracker.nextOrdinal(), org.scalatest.Resources.apply("cannotInstantiateSuite",
                    JavaConversions.asScalaBuffer(Lists.newArrayList((Object)e.getMessage()))),
                    Option.apply((Throwable) e), Option.apply((Object) (System.currentTimeMillis() - startTime)),
                    Option.apply((Summary)null), Option.apply((Formatter)null), Option.apply((Location)null),
                    Option.apply(null), Thread.currentThread().getName(), new Date().getTime()));
        } catch(IllegalAccessException e) {
            reporter.apply(new RunAborted(tracker.nextOrdinal(), org.scalatest.Resources.apply("cannotInstantiateSuite",
                    JavaConversions.asScalaBuffer(Lists.newArrayList((Object)e.getMessage()))),
                    Option.apply((Throwable) e), Option.apply((Object) (System.currentTimeMillis() - startTime)),
                    Option.apply((Summary)null), Option.apply((Formatter)null), Option.apply((Location)null),
                    Option.apply(null), Thread.currentThread().getName(), new Date().getTime()));
        } catch(NoClassDefFoundError e) {
            reporter.apply(new RunAborted(tracker.nextOrdinal(),
                    org.scalatest.Resources.apply("cannotLoadClass", JavaConversions.asScalaBuffer(Lists.newArrayList((Object)e.getMessage()))),
                    Option.apply((Throwable) e), Option.apply((Object) (System.currentTimeMillis() - startTime)),
                    Option.apply((Summary)null), Option.apply((Formatter)null), Option.apply((Location)null),
                    Option.apply(null), Thread.currentThread().getName(), new Date().getTime()));
        } catch(Throwable e) {
            reporter.apply(new RunAborted(tracker.nextOrdinal(), org.scalatest.Resources.bigProblems(e),
                    Option.apply(e), Option.apply((Object) (System.currentTimeMillis() - startTime)),
                    Option.apply((Summary)null), Option.apply((Formatter)null), Option.apply((Location)null),
                    Option.apply(null), Thread.currentThread().getName(), new Date().getTime()));
        } finally {
            reporter.dispatchDisposeAndWaitUntilDone();
        }
    }
}
