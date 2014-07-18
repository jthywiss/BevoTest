//
// BevoTest.java -- Java class BevoTest
// Project BevoTest
// http://www.cs.utexas.edu/~jthywiss/bevotest.shtml
//
// $Id$
//
// Created by jthywiss on Oct 27, 2012.
//
// Copyright (c) 2014 John A. Thywissen. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//     http://www.apache.org/licenses/LICENSE-2.0
//
// This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied. See the License
// for the specific language governing permissions and limitations under
// the License.
//

package edu.utexas.cs.bevotest;

import java.security.Permissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.PropertyPermission;

/**
 * An outer class wrapping the BevoTest framework.
 * <p>
 * The BevoTest framework executes test procedures on test items.  Each test
 * procedure's input and expected result is specified by a
 * <code>TestCase</code>.  A <code>Test</code> is simply a container for
 * <code>TestCase</code>s.  An execution of a <code>Test</code> executes the
 * test procedures in the <code>Test</code>, and records
 * <code>TestExecutionResults</code> in a <code>TestLog</code>.
 * <p>
 * BevoTest handles test cases that expect returned values or expect
 * particular exceptions to be thrown.  It is robust to test items that throw
 * unexpected exceptions or errors, that don't timely terminate (infinite
 * looping or very slow test items), and that attempt security violations.
 * <p>
 * This framework conforms to IEEE Std 829, IEEE Standard for Software Test
 * Documentation.
 * <p>
 * Some operations of this class use actions that are considered 
 * privileged under the Java security model.  The permissions needed for
 * these actions are given by <code>REQUESTED_PERMISSIONS</code>.  If these
 * privileged actions fail with an <code>AccessControlException</code>, the
 * framework recovers gracefully and proceeds with slightly less detailed
 * output.  In other words, the current security policy can choose to grant
 * <code>BevoTest</code> its <code>REQUESTED_PERMISSIONS</code> or not, with
 * no catastrophic consequences.
 * 
 * @author   John Thywissen
 * @version  $Id$
 * @see      "IEEE Std 829, Software Test Documentation"
 * @see      #REQUESTED_PERMISSIONS
 */
public class BevoTest {

    private BevoTest() {
        /* Not instantiable. 
         * BevoTest is merely a container for (static) nested classes.
         */
        throw new UnsupportedOperationException("Class BevoTest is not instantiable");
    }

    /**
     * A <code>Test</code> is a named collection of test procedures.  A
     * <code>Test</code> may be run, given a log to record test results into.
     *
     * @author  John Thywissen
     */
    public static class Test implements Iterable<TestCase<?, ?>> {
        private String                               testName;
        private final List<TestCase<?, ?>>           testCases;
        private final transient List<TestCase<?, ?>> testCasesReadOnly;

        /**
         * Constructs a new Test, i.e., a collection of test procedures.
         *
         * @param testName  the name of the test, such as "Acme project 123
         *                  qualification test"
         */
        public Test(final String testName) {
            setTestName(testName);
            testCases = new ArrayList<TestCase<?, ?>>();
            testCasesReadOnly = Collections.unmodifiableList(testCases);
        }

        /**
         * @return  the name of the test
         */
        public String getTestName() {
            return testName;
        }

        protected void setTestName(final String testName) {
            this.testName = testName;
        }

        /**
         * @return  the number of test cases in this test
         */
        public int size() {
            return testCasesReadOnly.size();
        }

        /**
         * @return  an <code>Iterator</code> over this test's test cases
         */
        @Override
        public Iterator<TestCase<?, ?>> iterator() {
            return testCasesReadOnly.iterator();
        }

        protected void addTestCase(final TestCase<?, ?> testCase) {
            testCases.add(testCase);
        }

        /**
         * Execute this test's test procedures.
         *
         * @param log  the <code>TestLog</code> in which the test results will
         *             be recorded
         * @throws InterruptedException  if the test framework thread is
         *                               interrupted.  The <i>interrupted
         *                               status</i> of the current thread is
         *                               cleared when this exception is
         *                               thrown.
         */
        public void run(final TestLog log) throws InterruptedException {
            try {
                for (final TestCase<?, ?> testCase : testCases) {
                    testCase.run(log);
                }
            } finally {
                log.complete();
            }
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("Test [testName=\"").append(testName).append("\", testCases=").append(testCases).append("]");
            return builder.toString();
        }
    }

    /**
     * A <code>TestCase</code> is a test case and corresponding test procedure
     * for a test item. A test case is one set of inputs, expected results,
     * and execution conditions for a test item. A test procedure is a
     * sequence of actions for the execution of a test. A <code>TestCase</code>
     * incorporates the test procedure for the case. (The name
     * <code>TestCaseAndProcedure</code> was rejected as too verbose, albeit
     * correct.)
     * <p>
     * A test case is specified by constructing a concrete subclass of
     * <code>TestCase</code>, supplying the expected results and a test
     * procedure, in the form of an implementation of the
     * <code>executeTest()</code> method.  See the convenience classes
     * <code>TestReturns</code> and <code>TestThrows</code> for examples.
     *
     * @author     John Thywissen
     * @param <C>  the Class type instance of the test item
     * @param <R>  the type of the return value of the test procedure
     * @see        TestReturns
     * @see        TestThrows
     */
    public static abstract class TestCase<C, R> {
        private final Test                 test;
        private final Class<C>             classUnderTest;
        private String                     description;
        private R                          expectedReturn;
        private Class<? extends Throwable> expectedThrowClass;
        private long                       timeOutMs;

        /**
         * Constructs a new TestCase.  Note that construction adds the case to
         * its test.
         *
         * @param test            the <code>Test</code> which this case is part of
         * @param classUnderTest  the expected type of the test item
         * @param description     a name for the test case
         */
        protected TestCase(final Test test, final Class<C> classUnderTest, final String description) {
            this.test = test;
            this.classUnderTest = classUnderTest;
            this.description = description;
            test.addTestCase(this);
        }

        /**
         * @return  the <code>Test</code> which this case is part of
         */
        public Test getTest() {
            return test;
        }

        /**
         * @return  the name for the test case
         */
        public String getDescription() {
            return description;
        }

        protected void setDescription(final String description) {
            this.description = description;
        }

        /**
         * @return  the expected type of the test item
         */
        public Class<?> getClassUnderTest() {
            return classUnderTest;
        }

        /**
         * @return  the maximum allowed test execution time, in milliseconds
         */
        public long getTimeOut() {
            return timeOutMs;
        }

        /**
         * @return  the expected test return value
         */
        public R getExpectedReturn() {
            return expectedReturn;
        }

        /**
         * @return  the type of the expected thrown value, <code>null</code>
         *          if no thrown value is expected.
         */
        public Class<? extends Throwable> getExpectedThrowClass() {
            return expectedThrowClass;
        }

        protected void setExpectedReturn(final R expectedReturn) {
            this.expectedReturn = expectedReturn;
        }

        protected void setExpectedThrowClass(final Class<? extends Throwable> expectedThrowClass) {
            this.expectedThrowClass = expectedThrowClass;
        }

        protected void setTimeOut(final long timeOutMs) {
            this.timeOutMs = timeOutMs;
        }

        /**
         * Execute this test procedure, and record events in the given test
         * log.
         *
         * @param testLog  the <code>TestLog</code> in which to record the
         *                 test execution results
         * @throws InterruptedException  if the test framework thread is
         *                               interrupted.  The <i>interrupted
         *                               status</i> of the current thread is
         *                               cleared when this exception is
         *                               thrown.
         */
        public void run(final TestLog testLog) throws InterruptedException {
            final TestExecutionResult<C, R> logEntry = new TestExecutionResult<C, R>(testLog, this);

            if (Thread.interrupted()) {
                throw new InterruptedException("test run interrupted");
            }

            if (shouldSkip()) {
                logEntry.skipped();
                return;
            }

            final ThreadGroup tg = new ThreadGroup("TestExecutionThreadGroup") {
                @Override
                public void uncaughtException(final Thread t, final Throwable e) {
                    if (!(e instanceof ThreadDeath)) {
                        try {
                            caught(e, logEntry);
                        } catch (final InterruptedException e1) {
                            /* Logged, so consume and continue next test */
                        }
                    }
                }
            };
            tg.setDaemon(true);
            final Thread t = new Thread(tg, "TestExecutionMainThread") {
                @Override
                public void run() {
                    try {
                        initiate(logEntry);
                        try {
                            executeTest(logEntry);
                        } catch (final Throwable e1) {
                            if (!(e1 instanceof ThreadDeath)) {
                                caught(e1, logEntry);
                            }
                        } finally {
                            complete(logEntry);
                        }
                    } catch (final InterruptedException e2) {
                        /* Logged, so consume and continue next test */
                    }
                }
            };
            /* TODO: Capture stdout, stderr, and jul logging records, too? */
            t.start();
            try {
                /* Wait for the thread group to terminate */
                final long maxWait = System.currentTimeMillis() + getTimeOut();
                while (tg.activeCount() > 0 && System.currentTimeMillis() < maxWait) {
                    Thread.sleep(10L);
                }
            } catch (final InterruptedException e) {
                /* Interrupted while waiting for test; interrupt test and exit */
                tg.interrupt();
                throw e;
            } finally {
                ensureThreadGroupTerminated(logEntry, tg, t);
            }
        }

        @SuppressWarnings("deprecation")
        protected void ensureThreadGroupTerminated(final TestExecutionResult<C, R> logEntry, final ThreadGroup testThreadGroup, final Thread mainTestThread) throws InterruptedException {
            /* If there are active threads in the group, interrupt */
            if (testThreadGroup.activeCount() > 0) {
                StackTraceElement[] stackTrace = null;
                try {
                    stackTrace = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<StackTraceElement[]>() {
                        @Override
                        public StackTraceElement[] run() {
                            return mainTestThread.getStackTrace();
                        }
                    });
                } catch (final java.security.AccessControlException e) {
                    /* Disregard and proceed */
                }
                if (hasActiveUserThreads(testThreadGroup)) {
                    timedOut(logEntry, stackTrace);
                }
                testThreadGroup.interrupt();
            }
            /* Wait politely for a while */
            final long maxWait = System.currentTimeMillis() + 120L;
            while (testThreadGroup.activeCount() > 0 && System.currentTimeMillis() < maxWait) {
                Thread.sleep(10L);
            }
            /* If the interrupt didn't do it, try this unsafe approach */
            if (hasActiveUserThreads(testThreadGroup)) {
                System.err.println("BevoTest: WARNING: A test procedure execution has been forcibly terminated. This may leave an invalid state for subsequent tests.  Test case description: " + getDescription());
                try {
                    java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<Void>() {
                        @Override
                        public Void run() {
                            final java.lang.management.ThreadInfo[] allThreadInfos = java.lang.management.ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
                            int locksHeldCount = 0;
                            for (final java.lang.management.ThreadInfo ti : allThreadInfos) {
                                /* TODO: Look at all treads in the group, not just the main thread */
                                if (ti.getThreadId() == mainTestThread.getId()) {
                                    locksHeldCount += ti.getLockedMonitors().length;
                                    locksHeldCount += ti.getLockedSynchronizers().length;
                                }
                            }
                            if (locksHeldCount > 0) {
                                System.err.println("BevoTest: DANGER: The forcibly terminated procedure held " + locksHeldCount + " locks at time of termination.  Test case description: " + getDescription());
                            }
                            return null;
                        }
                    });
                } catch (final SecurityException e) {
                    /* Disregard and proceed */
                } catch (final NullPointerException e) {
                    /* Disregard and proceed */
                }
                /* TODO: Should the status be set to a more specific value than
                 * TIMEOUT to indicate that this test execution termination was
                 * forced?
                 */
                testThreadGroup.stop();
            }
            if (hasActiveUserThreads(testThreadGroup)) {
                /* Give the stop a little time to propagate */
                Thread.sleep(10L);
            }
            if (!testThreadGroup.isDestroyed()) {
                /* The ThreadGroup isn't destroyed -- a Thread won't
                 * stop.  Mark all Threads as daemon so the JVM won't
                 * wait for any of them at JVM exit time.
                 */
                final Thread[] threadList = new Thread[testThreadGroup.activeCount() * 2];
                /* 2 here is an arbitrary fudge factor -- all of this is best effort only */
                testThreadGroup.enumerate(threadList);
                for (final Thread threadListElement : threadList) {
                    threadListElement.setDaemon(true);
                }
            }
        }

        protected boolean hasActiveUserThreads(final ThreadGroup tg) {
            final Thread[] threadList = new Thread[tg.activeCount() * 2];
            tg.enumerate(threadList);
            for (final Thread threadListElement : threadList) {
                if (!threadListElement.isDaemon() && threadListElement.isAlive()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Should this test procedure be skipped?
         * To be overridden, if needed, by the concrete test procedure.
         * The default implementation returns <code>false</code>.
         *
         * @return  <code>true</code> if this test procedure should not be
         *          run, but recorded as skipped.
         */
        public boolean shouldSkip() {
            return false;
        }

        protected void initiate(final TestExecutionResult<C, R> logEntry) throws InterruptedException {
            if (Thread.interrupted()) {
                throw new InterruptedException("test initiation interrupted");
            }
            logEntry.settingUp();
        }

        private static InheritableThreadLocal<TestExecutionResult<?, ?>> logEntryTL = new InheritableThreadLocal<TestExecutionResult<?, ?>>();

        protected void executeTest(final TestExecutionResult<C, R> logEntry) throws Throwable {
            logEntryTL.set(logEntry);
            executeTest();
        }

        /**
         * Run the test procedure.  This abstract method must be overridden
         * with an implementation that undertakes these responsibilities:
         * <ol>
         * <li>Set up the test procedure, as needed.</li>
         * <li>Then, call <code>starting(C)</code> with the test item.</li>
         * <li>Perform the test.</li>
         * <li>Then, call <code>returned(R)</code> with value returned by the test.</li>
         * <li>Tear down the test procedure, as needed.</li>
         * </ol>
         * <p>
         * Exceptions thrown by <code>executeTest()</code> will be recorded,
         * and the test will be considered completed.
         */
        abstract protected void executeTest() throws Throwable;

        protected void starting(final C instanceUnderTest, final TestExecutionResult<C, R> logEntry) {
            if (instanceUnderTest == null) {
                throw new NullTestItemException("Attempt to execute test on null test item");
            }
            logEntry.processing(instanceUnderTest);
        }

        @SuppressWarnings("unchecked")
        protected void starting(final C instanceUnderTest) {
            starting(instanceUnderTest, (TestExecutionResult<C, R>) logEntryTL.get());
        }

        protected void startingStatic(final Class<? extends C> actualClassUnderTest, final TestExecutionResult<C, R> logEntry) {
            if (actualClassUnderTest == null) {
                throw new NullTestItemException("Attempt to execute test on null test class");
            }
            logEntry.processingStatic(actualClassUnderTest);
        }

        @SuppressWarnings("unchecked")
        protected void startingStatic(final Class<? extends C> actualClassUnderTest) {
            startingStatic(actualClassUnderTest, (TestExecutionResult<C, R>) logEntryTL.get());
        }

        protected R returned(final R value, final TestExecutionResult<C, R> logEntry) {
            logEntry.returned(value);
            return value;
        }

        @SuppressWarnings("unchecked")
        protected void returned(final R value) {
            returned(value, (TestExecutionResult<C, R>) logEntryTL.get());
        }

        protected void caught(final Throwable t, final TestExecutionResult<C, R> logEntry) throws InterruptedException {
            logEntry.caught(t);
            if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof InterruptedException) {
                throw (InterruptedException) t;
            } else {
                /* Exception handled */
            }
        }

        protected void timedOut(final TestExecutionResult<C, R> logEntry, final StackTraceElement[] stackTrace) {
            logEntry.timedOut(stackTrace);
        }

        protected void complete(final TestExecutionResult<C, R> logEntry) {
            logEntryTL.remove();
            logEntry.complete();
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("TestCase [test.name=\"").append(test.getTestName()).append("\", classUnderTest=").append(classUnderTest).append(", description=").append(description).append(", expectedReturn=").append(expectedReturn).append(", expectedThrowClass=").append(expectedThrowClass).append(", timeOutMs=").append(timeOutMs).append("]");
            return builder.toString();
        }

    }

    /**
     * TestReturns is a convenience class for constructing
     * <code>TestCase</code>s that expect a return value.
     * <p>
     * Example of use:
     * <pre>
     * new BevoTest.TestReturns<Object, Integer>(ts, Object.class, "Return an int", 1648697714, 2000L) { @Override public void executeTest() {
     *     // Set up:
     *     final String testItem = "Test test test";
     *     // Execute:
     *     starting(testItem);
     *     returned(testItem.hashCode());
     *     // Any tear down here
     *  } };
     * </pre>
     * <p>
     * The <code>public void executeTest()</code> implementation has these
     * responsibilities:
     * <ol>
     * <li>Set up the test procedure, as needed.</li>
     * <li>Then, call <code>starting(C)</code> with the test item.</li>
     * <li>Perform the test.</li>
     * <li>Then, call <code>returned(R)</code> with value returned by the test.</li>
     * <li>Tear down the test procedure, as needed.</li>
     * </ol>
     *
     * @author     John Thywissen
     * @param <C>  the Class type instance of the test item
     * @param <R>  the type of the return value of the test procedure
     * @see        TestThrows
     */
    public abstract static class TestReturns<C, R> extends TestCase<C, R> {

        public TestReturns(final Test test, final Class<C> classUnderTest, final String description, final R expectedReturn) {
            super(test, classUnderTest, description);
            setExpectedReturn(expectedReturn);
        }

        public TestReturns(final Test test, final Class<C> classUnderTest, final String description, final R expectedReturn, final long timeOutMs) {
            super(test, classUnderTest, description);
            setExpectedReturn(expectedReturn);
            setTimeOut(timeOutMs);
        }

    }

    /**
     * TestThrows is a convenience class for constructing
     * <code>TestCase</code>s that expect a thrown exception.
     * <p>
     * Example of use:
     * <pre>
     * new BevoTest.TestThrows<String, Integer>(ts, String.class, "Throw an expected NPE", NullPointerException.class) { @Override public void executeTest() {
     *     // Set up:
     *     final String testItem = "Test test test";
     *     // Execute:
     *     starting(testItem);
     *     returned(testItem.indexOf(null));
     *     // Any tear down here
     *  } };
     * </pre>
     * <p>
     * The <code>public void executeTest()</code> implementation has these
     * responsibilities:
     * <ol>
     * <li>Set up the test procedure, as needed.</li>
     * <li>Then, call <code>starting(C)</code> with the test item.</li>
     * <li>Perform the test.</li>
     * <li>Then, call <code>returned(R)</code> with value returned by the test.</li>
     * <li>Tear down the test procedure, as needed.</li>
     * </ol>
     *
     * @author     John Thywissen
     * @param <C>  the Class type instance of the test item
     * @param <R>  the type of the return value of the test procedure
     * @see        TestReturns
     */
    public abstract static class TestThrows<C, R> extends TestCase<C, R> {

        public TestThrows(final Test test, final Class<C> classUnderTest, final String description, final Class<? extends Throwable> expectedThrowClass) {
            super(test, classUnderTest, description);
            setExpectedThrowClass(expectedThrowClass);
        }

        public TestThrows(final Test test, final Class<C> classUnderTest, final String description, final Class<? extends Throwable> expectedThrowClass, final long timeOutMs) {
            super(test, classUnderTest, description);
            setExpectedThrowClass(expectedThrowClass);
            setTimeOut(timeOutMs);
        }

    }

    /**
     * A <code>TestLog</code> is a record of an execution of a
     * <code>Test</code>.  It consists of a sequence of
     * <code>TestLogEntries</code>, some test environment data, and some
     * summary test execution data (such as start time).
     *
     * @author  John Thywissen
     */
    public static class TestLog implements Iterable<TestLogEntry> {

        private final Test                         test;
        private final List<TestLogEntry>           entries;
        private final transient List<TestLogEntry> entriesReadOnly;
        private final String                       environmentDescription;
        private final long                         testStartTime;
        private long                               testEndTime;

        /**
         * Constructs a new TestLog, and mark the test as started.
         * This constructor also gathers environmental data.
         *
         * @param test  the <code>Test</code> that this log is recording an
         *              execution of.
         */
        public TestLog(final Test test) {
            this.test = test;
            entries = new ArrayList<TestLogEntry>(test.size());
            entriesReadOnly = Collections.unmodifiableList(entries);
            environmentDescription = describeEnvironment();
            testStartTime = System.currentTimeMillis();
        }

        /**
         * @return  the <code>Test</code> that this log is recording an
         *          execution of.
         */
        public Test getTest() {
            return test;
        }

        protected String describeEnvironment() {
            final StringBuilder sb = new StringBuilder(80);
            sb.append("Java version ");
            sb.append(System.getProperty("java.version"));
            sb.append(", maximum heap size ");
            sb.append(Runtime.getRuntime().maxMemory() / 1024 / 1024);
            sb.append(" MB");
            sb.append(", running on ");
            sb.append(System.getProperty("os.name"));
            sb.append(" ");
            sb.append(System.getProperty("os.version"));
            sb.append(" (");
            sb.append(System.getProperty("os.arch"));
            sb.append(')');
            try {
                final String java_class_path = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return System.getProperty("java.class.path");
                    }
                });
                sb.append("\nJava class path:   ");
                sb.append(java_class_path);
            } catch (final java.security.AccessControlException e) {
                /* Disregard and proceed */
            }
            try {
                final String user_dir = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return System.getProperty("user.dir");
                    }
                });
                sb.append("\nWorking directory: ");
                sb.append(user_dir);
            } catch (final java.security.AccessControlException e) {
                /* Disregard and proceed */
            }
            return sb.toString();
        }

        /**
         * @return  a <code>String</code> describing the environment in which
         *          the test was run.
         */
        public String getEnvironmentDescription() {
            return environmentDescription;
        }

        /**
         * @return  the time at which the test execution started
         * @see     java.lang.System#currentTimeMillis()
         */
        public long getTestStartTime() {
            return testStartTime;
        }

        /**
         * @return  the time at which the test execution ended
         * @see     java.lang.System#currentTimeMillis()
         */
        public synchronized long getTestEndTime() {
            return testEndTime;
        }

        /**
         * @return  the number of entries in this log
         */
        public synchronized int size() {
            return entries.size();
        }

        /**
         * @return  an <code>Iterator</code> over this log's entries
         */
        @Override
        public synchronized Iterator<TestLogEntry> iterator() {
            return entriesReadOnly.iterator();
        }

        protected void addEntry(final TestLogEntry logEntry) throws IllegalStateException {
            synchronized (this) {
                if (testEndTime != 0) {
                    throw new IllegalStateException("TestLog is already complete");
                }
                entries.add(logEntry);
            }
            notifyNewEntry(logEntry);
        }

        protected void complete() throws IllegalStateException {
            synchronized (this) {
                if (testEndTime != 0) {
                    throw new IllegalStateException("TestLog is already complete");
                }
                this.testEndTime = System.currentTimeMillis();
            }
            notifyComplete();
        }

        protected void notifyNewEntry(final TestLogEntry entry) {
            /* Override this for notifications */
        }

        protected void notifyEntryChanged(final TestLogEntry entry) {
            /* Override this for notifications */
        }

        protected void notifyComplete() {
            /* Override this for notifications */
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("TestLog [test=\"").append(test.getTestName()).append("\", entries=").append(entries).append(", environmentDescription=").append(environmentDescription).append(", testStartTime=").append(formatTime(testStartTime)).append(", testEndTime=").append(formatTime(testEndTime)).append("]");
            return builder.toString();
        }

        protected static String formatTime(final long timeMs) {
            return String.format("%1$TF %1$TT %1$TZ", Long.valueOf(timeMs));
        }

    }

    /**
     * A <code>TestLogEntry</code> represents an event that occurred during
     * a test execution.
     *
     * @author  John Thywissen
     * @see     TestExecutionResult
     * @see     TestLog
     */
    public interface TestLogEntry {
        /* No members yet */
    }

    /**
     * A <code>TestExecutionResult</code> represents the result of executing a
     * test case on a test item.  Each <code>TestExecutionResult</code>
     * has a status (running, complete, etc...) and evaluation (pass/fail),
     * along with details of the execution.  These details include the
     * runtime type of the test item, test results, and run time.
     *
     * @author     John Thywissen
     * @param <C>  the Class type instance of the test item
     * @param <R>  the type of the return value of the test procedure
     * @see        TestLog
     */
    public static class TestExecutionResult<C, R> implements TestLogEntry {
        /* Intra-thread consistency design:
         *
         * Writes to the fields below are either:
         * 1. Initializations,
         * 2. Synchronized, or
         * 3. Immediately followed by #2.
         *
         * Reads of the fields below are either:
         * 1. Of immutable (final) values,
         * 2. Synchronized, or
         * 3. Immediately preceded by #2.
         *
         * Maintainers: Be careful with the #3s.
         */

        private final TestLog        testLog;
        private final TestCase<C, R> testCase;
        private Status               status;
        private Class<?>             testItemClass;
        private R                    returnedValue;
        private boolean              returnedValueValid;
        private Throwable            caughtValue;
        private Evaluation           evaluation;
        private long                 runTime;

        /**
         * Constructs a new TestExecutionResult, in the ENQUEUED status, and
         * adds it to its test log.
         *
         * @param testLog   the <code>TestLog</code> that this result is an entry of
         * @param testCase  the <code>TestCase</code> that this is a result of
         */
        protected TestExecutionResult(final TestLog testLog, final TestCase<C, R> testCase) {
            this.testLog = testLog;
            this.testCase = testCase;
            this.status = Status.ENQUEUED;
            testLog.addEntry(this);
        }

        /**
         * @return  the <code>TestLog</code> that this result is an entry of
         */
        public TestLog getTestLog() {
            return testLog;
        }

        /**
         * @return  the <code>TestCase</code> that this is a result of
         */
        public TestCase<C, R> getTestCase() {
            return testCase;
        }

        /**
         * @return  <code>true</code> if the test procedure has run to
         *          completion (normal or abnormal termination)
         */
        public synchronized boolean isComplete() {
            return status == Status.COMPLETE_NORMAL || status == Status.COMPLETE_ABNORMAL || status == Status.TIMED_OUT;
        }

        /**
         * @return  <code>true</code> if the test procedure has run to
         *          completion (normal or abnormal termination), OR
         *          has been skipped
         */
        public synchronized boolean isCompleteOrSkipped() {
            return isComplete() || status == Status.SKIPPED;
        }

        /**
         * @return  the <code>Status</code> of this test procedure execution
         * @see     Status
         */
        public synchronized Status getStatus() {
            return status;
        }

        protected synchronized void setStatus(final Status status) {
            this.status = status;
            testLog.notifyEntryChanged(this);
        }

        /**
         * @return  the actual runtime type of the test item
         */
        public synchronized Class<?> getTestItemClass() {
            return testItemClass;
        }

        /**
         * @return  <code>true</code> if the test procedure returned a value
         */
        public synchronized boolean isReturnedValueValid() {
            return returnedValueValid;
        }

        /**
         * @return  the value returned by the test item
         * @throws IllegalStateException  if <code>isReturnedValueValid()</code> is <code>false</code>
         * @see     #isReturnedValueValid()
         */
        public R getReturnedValue() throws IllegalStateException {
            if (!isReturnedValueValid()) {
                throw new IllegalStateException("ReturnedValue is not valid");
            }
            return returnedValue;
        }

        /**
         * @return  the <code>Throwable</code> thrown by the test item, or
         *          <code>null</code> if nothing has been thrown
         */
        public synchronized Throwable getCaughtValue() {
            return caughtValue;
        }

        /**
         * @return  the <code>Evaluation</code> (pass/fail) of this test procedure execution
         * @throws IllegalStateException  if <code>isCompleteOrSkipped()</code> is <code>false</code>
         * @see     Evaluation
         * @see     #isCompleteOrSkipped()
         */
        public synchronized Evaluation getEvaluation() throws IllegalStateException {
            if (!isCompleteOrSkipped()) {
                throw new IllegalStateException("status is not COMPLETE or SKIPPED; status=" + status);
            }
            return evaluation;
        }

        protected synchronized void setEvaluation(final Evaluation evaluation) {
            this.evaluation = evaluation;
            testLog.notifyEntryChanged(this);
        }

        /**
         * @return  the real time, in milliseconds, that the test took to execute
         * @throws IllegalStateException  if status is not <code>COMPLETE_NORMAL</code>
         */
        public synchronized long getRunTime() throws IllegalStateException {
            if (status != Status.COMPLETE_NORMAL) {
                throw new IllegalStateException("status is not COMPLETE_NORMAL; status=" + status);
            }
            return runTime;
        }

        protected void settingUp() throws IllegalStateException {
            if (status != Status.ENQUEUED) {
                throw new IllegalStateException("settingUp called when status=" + status);
            }
            setStatus(Status.RUNNING_SETUP);
        }

        protected void processing(final C instanceUnderTest) throws IllegalStateException {
            processingStatic(instanceUnderTest.getClass());
        }

        protected void processingStatic(final Class<?> classUnderTest) throws IllegalStateException {
            if (status != Status.RUNNING_SETUP) {
                throw new IllegalStateException("processing called when status=" + status);
            }
            this.testItemClass = classUnderTest;
            runTime = System.currentTimeMillis(); // runTime = start time when
                                                  // processing
            setStatus(Status.RUNNING_PROCESSING);
        }

        protected void returned(final R value) throws IllegalStateException {
            runTime = System.currentTimeMillis() - runTime;
            this.returnedValue = value;
            this.returnedValueValid = true;
            if (status != Status.RUNNING_PROCESSING) {
                throw new IllegalStateException("returned called when status=" + status);
            }
            tearingDown();
        }

        protected void tearingDown() throws IllegalStateException {
            if (status != Status.RUNNING_PROCESSING && status != Status.RUNNING_TEARDOWN) {
                throw new IllegalStateException("tearingDown called when status=" + status);
            }
            setStatus(Status.RUNNING_TEARDOWN);
        }

        protected void caught(final Throwable t) {
            if (caughtValue == null) {
                caughtValue = t;
                setStatus(Status.COMPLETE_ABNORMAL);
            }
            // silently discard subsequent exceptions
        }

        protected void timedOut(final StackTraceElement[] stackTrace) {
            setStatus(Status.TIMED_OUT);
            if (stackTrace != null && stackTrace.length > 0) {
                caughtValue = new TimeoutStackTrace(stackTrace);
            }
        }

        protected synchronized void complete() throws IllegalStateException {
            if (status != Status.RUNNING_TEARDOWN && status != Status.COMPLETE_ABNORMAL && status != Status.TIMED_OUT) {
                throw new IllegalStateException("completing when status=" + status);
            }
            if (status == Status.TIMED_OUT) {
                setEvaluation(Evaluation.FAILED);
            } else {
                if (returnedValueValid) {
                    setStatus(Status.COMPLETE_NORMAL);
                    if (testCase.getExpectedThrowClass() == null && (testCase.getExpectedReturn() == null ? returnedValue == null : testCase.getExpectedReturn().equals(returnedValue))) {
                        setEvaluation(Evaluation.PASSED);
                    } else {
                        setEvaluation(Evaluation.FAILED);
                    }
                }
                if (caughtValue != null) {
                    setStatus(Status.COMPLETE_ABNORMAL);
                    if (testCase.getExpectedThrowClass() != null && caughtValue != null && testCase.getExpectedThrowClass().isAssignableFrom(caughtValue.getClass())) {
                        setEvaluation(Evaluation.PASSED);
                    } else {
                        setEvaluation(Evaluation.FAILED);
                    }
                }
            }
            assert getEvaluation() != null : "Evaluation was not set during test completion";
            assert getEvaluation() != Evaluation.NO_RESULT : "Evaluation was not set during test completion";
        }

        protected void skipped() {
            setStatus(Status.SKIPPED);
            setEvaluation(Evaluation.NO_RESULT);
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("TestExecutionResult [testCase=").append(testCase).append(", status=").append(status).append(", testItemClass=").append(testItemClass).append(", returnedValue=").append(returnedValue).append(", returnedValueValid=").append(returnedValueValid).append(", caughtValue=").append(caughtValue).append(", evaluation=").append(evaluation).append(", runTime=").append(runTime / 1000.0).append("]");
            return builder.toString();
        }

        /**
         * A status of a <code>TestCase</code> (test procedure) execution
         */
        public enum Status {
            ENQUEUED, RUNNING_SETUP, RUNNING_PROCESSING, RUNNING_TEARDOWN, SKIPPED, COMPLETE_NORMAL, COMPLETE_ABNORMAL, TIMED_OUT
        }

        /**
         * An evaluation (pass/fail) of the result of a <code>TestCase</code>
         * (test procedure) execution
         */
        public enum Evaluation {
            NO_RESULT, FAILED, PASSED
        }

    }

    /**
     * A <code>NullTestItemException</code> is a
     * <code>NullPointerException</code> thrown by BevoTest because the item
     * under test was null.
     *
     * @author  John Thywissen
     */
    @SuppressWarnings("serial")
    public static class NullTestItemException extends NullPointerException {
      /**
       * Constructs a <code>NullTestItemException</code> with the specified 
       * detail message. 
       *
       * @param   s   the detail message.
       */
      public NullTestItemException(String s) {
        super(s);
      }
    }

    /**
     * A <code>TimeoutStackTrace</code> holds the stack trace found in a
     * test execution thread that was terminated because it exceeded the test
     * case's run time limit. 
     *
     * @author  John Thywissen
     */
    @SuppressWarnings("serial")
    public static class TimeoutStackTrace extends Throwable {

        protected TimeoutStackTrace(final StackTraceElement[] stackTrace) {
            super("Stack trace at time out");
            setStackTrace(stackTrace);
        }

        @Override
        public String toString() {
            return "Stack trace at time out:";
        }

    }

    /**
     * These permissions let the test framework output more detail, if the
     * security policy grants them to this class.
     * If the security policy does not grant these permissions, the framework
     * falls-back with slightly less detailed output.
     */
    public final static Permissions REQUESTED_PERMISSIONS;

    static {
        REQUESTED_PERMISSIONS = new Permissions();
        REQUESTED_PERMISSIONS.add(new PropertyPermission("java.class.path", "read"));
        REQUESTED_PERMISSIONS.add(new PropertyPermission("user.dir", "read"));
        REQUESTED_PERMISSIONS.add(new RuntimePermission("getStackTrace"));
        REQUESTED_PERMISSIONS.add(new java.lang.management.ManagementPermission("monitor"));
        REQUESTED_PERMISSIONS.setReadOnly();
    }

}
