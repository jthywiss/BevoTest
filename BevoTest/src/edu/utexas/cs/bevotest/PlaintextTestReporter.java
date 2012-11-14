//
// PlaintextTestReporter.java -- Java class PlaintextTestReporter
// Project BevoTest
// http://www.cs.utexas.edu/~jthywiss/bevotest.shtml
//
// $Id$
//
// Created by jthywiss on Oct 27, 2012.
//
// Copyright (c) 2012 John A. Thywissen. All rights reserved.
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

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.Permissions;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.utexas.cs.bevotest.BevoTest.TestCase;
import edu.utexas.cs.bevotest.BevoTest.TestExecutionResult;
import edu.utexas.cs.bevotest.BevoTest.TestExecutionResult.Evaluation;
import edu.utexas.cs.bevotest.BevoTest.TestExecutionResult.Status;
import edu.utexas.cs.bevotest.BevoTest.TestLog;
import edu.utexas.cs.bevotest.BevoTest.TestLogEntry;
import edu.utexas.cs.bevotest.BevoTest.TimeoutStackTrace;

/**
 * A report-producing utility for a <code>BevoTest.TestLog</code>.  Writes a
 * formatted plaintext test summary report and optionally the test log to the
 * given <code>Appendable</code>.  The report format can be varied by
 * supplying <code>ReportOption</code>s.
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
 * <code>PlaintextTestReporter</code> its <code>REQUESTED_PERMISSIONS</code> or
 * not, with no catastrophic consequences.
 * 
 * @author   John Thywissen
 * @version  $Id$
 * @see      BevoTest
 * @see      "IEEE Std 829, Software Test Documentation"
 * @see      #REQUESTED_PERMISSIONS
 * @see      java.lang.Appendable
 */
public class PlaintextTestReporter {

    private final TestLog       log;
    private static final String newLine;

    static {
        newLine = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("line.separator");
            }
        });
    }

    /**
     * Constructs a TestLogReporter.
     *
     * @param log  the <code>TestLog</code> to report upon
     */
    public PlaintextTestReporter(final TestLog log) {
        super();
        this.log = log;
    }

    /**
     * Output a detailed report of this test execution.
     *
     * Convenience method for
     * <code>report(out, ReportOption.EMPTY_SET)</code>.
     *
     * @param out  an <code>Appendable</code> on which the description
     *             should be written.
     * @throws IOException  if the <code>Appendable</code>'s
     *                      <code>append</code> operation throws an
     *                      <code>IOException</code>
     */
    public void report(final Appendable out) throws IOException {
        report(out, ReportOption.EMPTY_SET);
    }

    /**
     * Output a detailed report of this test execution.
     *
     * @param out         an <code>Appendable</code> on which the
     *                    description should be written.
     * @param reportOpts  an <code>EnumSet&lt;ReportOption&gt;</code>
     * @throws IOException  if the <code>Appendable</code>'s
     *                      <code>append</code> operation throws an
     *                      <code>IOException</code>
     */
    public void report(final Appendable out, final Set<ReportOption> reportOpts) throws IOException {
        reportHeader(out, reportOpts);
        int logEntryCount = 0;
        final String entryNumFormat = "%"+String.valueOf(log.size()).length()+"d";
        for (final TestLogEntry entry : log) {
            if (reportOpts.contains(ReportOption.ONE_LINE) || reportOpts.contains(ReportOption.ONE_LINE_SHOW_STACK)) {
                out.append(String.format(entryNumFormat, new Integer(++logEntryCount))).append(" | ");
            } else {
                out.append(newLine);
                out.append("TEST LOG ENTRY ").append(String.valueOf(++logEntryCount)).append(newLine);
            }
            reportEntry(entry, out, reportOpts);
        }
        reportFooter(out, reportOpts);
    }

    /**
     * Output a summary report of this test execution.
     *
     * @param out         an <code>Appendable</code> on which the
     *                    description should be written.
     * @param reportOpts  an <code>EnumSet&lt;ReportOption&gt;</code>
     * @throws IOException  if the <code>Appendable</code>'s
     *                      <code>append</code> operation throws an
     *                      <code>IOException</code>
     */
    public void reportSummary(final Appendable out, final Set<ReportOption> reportOpts) throws IOException {

        final Map<Class<?>, String> testedClasses = new HashMap<Class<?>, String>();
        final int[] overallEvals = new int[TestExecutionResult.Evaluation.values().length];
        /* testedClassEvals is a well-behaved Map of classes to evaluation counts */
        @SuppressWarnings("serial")
        final Map<Class<?>, int[]> testedClassEvals = new HashMap<Class<?>, int[]>() {
            @Override
            public int[] get(final Object key) {
                int[] v = super.get(key);
                if (v == null) {
                    v = new int[TestExecutionResult.Evaluation.values().length];
                }
                return v;
            }
        };

        for (final TestLogEntry entry : log) {
            if (entry instanceof TestExecutionResult<?, ?>) {
                final TestExecutionResult<?, ?> result = (TestExecutionResult<?, ?>) entry;
                final Class<?> tic = result.getTestItemClass();
                if (tic != null) {
                    if (!testedClasses.containsKey(tic)) {
                        testedClasses.put(tic, describeClass(result.getTestItemClass(), true));
                    }
                    final int[] evals = testedClassEvals.get(tic);
                    evals[result.getEvaluation().ordinal()]++;
                }
                overallEvals[result.getEvaluation().ordinal()]++;
            }
        }
        out.append("TEST SUMMARY").append(newLine);
        out.append(newLine);
        out.append("Test name: ").append(log.getTest().getTestName()).append(newLine);
        out.append(newLine);
        out.append("Results: ");
        for (int ord = TestExecutionResult.Evaluation.values().length - 1; ord >= 0; ord--) {
            final TestExecutionResult.Evaluation evalType = TestExecutionResult.Evaluation.values()[ord];
            out.append(formatEnum(evalType));
            out.append(": ");
            out.append(String.valueOf(overallEvals[evalType.ordinal()]));
            if (ord > 0) {
                out.append(", ");
            }
        }
        out.append(newLine);
        out.append(newLine);
        out.append("Classes tested:").append(newLine);
        for (final String testedClassDescr : testedClasses.values()) {
            out.append("* ").append(testedClassDescr).append(newLine);
        }
        out.append(newLine);
        out.append("Environment:       ").append(log.getEnvironmentDescription()).append(newLine);
        out.append("Test start time:   ").append(formatTime(log.getTestStartTime())).append(newLine);
        out.append("Test end time:     ").append(formatTime(log.getTestEndTime())).append(newLine);
        out.append("Test elapsed time: ").append(formatDuration(log.getTestEndTime() - log.getTestStartTime())).append(newLine);
    }

    protected void reportHeader(final Appendable out, final Set<ReportOption> reportOpts) throws IOException {
        reportSummary(out, reportOpts);
        out.append(newLine).append(newLine).append("TEST LOG").append(newLine);
    }

    protected void reportFooter(final Appendable out, final Set<ReportOption> reportOpts) throws IOException {
        out.append(newLine).append("END OF TEST LOG").append(newLine);
    }

    /**
     * Output a detailed description of the given test case.
     *
     * @param testCase    the <code>TestCase</code> to describe
     * @param out         an <code>Appendable</code> on which the
     *                    description should be written.
     * @param reportOpts  an <code>EnumSet&lt;ReportOption&gt;</code>
     * @throws IOException  if the <code>Appendable</code>'s
     *                      <code>append</code> operation throws an
     *                      <code>IOException</code>
     */
    public void reportCase(final TestCase<?, ?> testCase, final Appendable out, final Set<ReportOption> reportOpts) throws IOException {
        out.append("   Test case description:   ").append(testCase.getDescription()).append(newLine);
        out.append("   Declared test item type: ").append(describeClass(testCase.getClassUnderTest(), false)).append(newLine);
        if (testCase.getExpectedThrowClass() == null) {
            if (testCase.getExpectedReturn() == null) {
                out.append("   Expected return value:   null").append(newLine);
            } else {
                if (!reportOpts.contains(ReportOption.NO_VALUES)) {
                    out.append("   Expected return value:   ").append(testCase.getExpectedReturn().getClass().getCanonicalName()).append(": ").append(testCase.getExpectedReturn().toString()).append(newLine);
                } else {
                    out.append("   Expected return type:    ").append(testCase.getExpectedReturn().getClass().getCanonicalName()).append(newLine);
                }
            }
        } else {
            out.append("   Expected thrown class:   ").append(describeClass(testCase.getExpectedThrowClass(), false)).append(newLine);
        }
        if (testCase.getTimeOut() > 0) {
            out.append("   Time out:                ").append(String.valueOf(testCase.getTimeOut())).append(" ms").append(newLine);
        }
    }

    public void reportEntry(final TestLogEntry entry, final Appendable out, final Set<ReportOption> reportOpts) throws IOException {
        if (entry instanceof TestExecutionResult<?, ?>) {
            reportEntry((TestExecutionResult<?, ?>) entry, out, reportOpts);
        } else {
            out.append("Unknown entry type:").append(newLine);
            out.append("    ");
            out.append(entry.toString());
            out.append(newLine);
        }
    }

    /**
     * Output a detailed report of the given test execution result.
     *
     * @param entry       the <code>TestExecutionResult</code> to report upon
     * @param out         an <code>Appendable</code> on which the
     *                    description should be written.
     * @param reportOpts  an <code>EnumSet&lt;ReportOption&gt;</code>
     * @throws IOException  if the <code>Appendable</code>'s
     *                      <code>append</code> operation throws an
     *                      <code>IOException</code>
     */
    public void reportEntry(final TestExecutionResult<?, ?> entry, final Appendable out, final Set<ReportOption> reportOpts) throws IOException {
        if (reportOpts.contains(ReportOption.ONE_LINE) || reportOpts.contains(ReportOption.ONE_LINE_SHOW_STACK)) {
            final String eval = entry.isCompleteOrSkipped() ? formatEnum(entry.getEvaluation()) : "";
            out.append((eval+"         ").substring(0, 9)).append(" | ");
            final StringBuilder statusColumn = new StringBuilder(24);
            if (entry.getStatus() == Status.COMPLETE_NORMAL && entry.getEvaluation() == Evaluation.PASSED) {
                statusColumn.append("Run time: ");
                statusColumn.append(entry.getRunTime());
                statusColumn.append(" ms");
            } else if (entry.getStatus() == Status.COMPLETE_NORMAL && entry.getEvaluation() == Evaluation.FAILED) {
                statusColumn.append("Incorrect return value");
            } else if (entry.getStatus() == Status.COMPLETE_ABNORMAL && entry.getCaughtValue() != null) {
                statusColumn.append(entry.getCaughtValue().getClass().getSimpleName());
            } else if (entry.getStatus() == Status.TIMED_OUT) {
                statusColumn.append(formatEnum(entry.getStatus()));
                statusColumn.append(" > ");
                statusColumn.append(entry.getTestCase().getTimeOut());
                statusColumn.append(" ms");
            } else {
                statusColumn.append(formatEnum(entry.getStatus()));
            }
            out.append((statusColumn+"                        ").substring(0, 24)).append(" | ");
            out.append(entry.getTestCase().getDescription()).append(newLine);
            if (entry.getCaughtValue() != null && reportOpts.contains(ReportOption.ONE_LINE_SHOW_STACK)) {
                out.append("      ");
                appendStackTrace(entry.getCaughtValue(), out);
            }
        } else {
            out.append("Test case:").append(newLine);
            reportCase(entry.getTestCase(), out, reportOpts);
            out.append("Test procedure result:").append(newLine);
            final boolean showDetail = !reportOpts.contains(ReportOption.FAIL_DETAIL_ONLY) || entry.getEvaluation() == Evaluation.FAILED;
            if (entry.getTestItemClass() != null && showDetail) {
                out.append("   Actual test item type:   ").append(describeClass(entry.getTestItemClass(), false)).append(newLine);
            }
            out.append("   Test procedure status:   ").append(entry.getStatus().toString()).append(newLine);
            if (entry.isReturnedValueValid() && showDetail) {
                if (entry.getReturnedValue() == null) {
                    out.append("   Actual return value:     null").append(newLine);
                } else {
                    if (!reportOpts.contains(ReportOption.NO_VALUES)) {
                        out.append("   Actual return value:     ").append(entry.getReturnedValue().getClass().getCanonicalName()).append(": ").append(entry.getReturnedValue().toString()).append(newLine);
                    } else {
                        out.append("   Actual return type:      ").append(entry.getReturnedValue().getClass().getCanonicalName()).append(newLine);
                    }
                }
            }
            if (entry.getCaughtValue() != null && showDetail) {
                if (!(entry.getCaughtValue() instanceof TimeoutStackTrace)) {
                    out.append("   Thrown value:            ");
                } else {
                    out.append("   ");
                }
                appendStackTrace(entry.getCaughtValue(), out);
            }
            if (entry.getStatus() == Status.COMPLETE_NORMAL) {
                out.append("   Test procedure run time: ").append(String.valueOf(entry.getRunTime())).append(" ms").append(newLine);
            }
            out.append("   Evaluation:              ").append(entry.getEvaluation().toString()).append(newLine);
        }
    }

    protected static String formatEnum(final Enum<?> enumVal) {
        final String s = enumVal.toString().replace('_', ' ');
        return s.charAt(0) + s.toLowerCase().substring(1);
    }

    protected static String formatTime(final long timeMs) {
        return String.format("%1$TF %1$TT %1$TZ", Long.valueOf(timeMs));
    }

    protected static String formatDuration(final long durationMs) {
        return String.format("%d.%03d s", Long.valueOf(durationMs / 1000), Long.valueOf(durationMs % 1000));
    }

    protected static String describeClass(final Class<?> clazz, final boolean detailed) {
        if (clazz == null) {
            return null;
        }
        final StringBuilder out = new StringBuilder();
        out.append(clazz.isInterface() ? "interface " : clazz.isPrimitive() ? "" : "class ");
        if (clazz.getCanonicalName() != null) {
            out.append(clazz.getCanonicalName());
        } else {
            out.append("{anonymous class ");
            out.append(clazz.getName());
            if (detailed) {
                try {
                    final Method method = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<Method>() {
                        @Override
                        public Method run() {
                            return clazz.getEnclosingMethod();
                        }
                    });
                    out.append(" in ");
                    out.append(method.toString());
                } catch (final java.security.AccessControlException e) {
                    /* Disregard and proceed */
                } catch (final NullPointerException e) {
                    /* Probably a null code source. Disregard and proceed, in
                     * any case. */
                }
            }
            out.append('}');
        }
        if (detailed) {
            try {
                final URL location = java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<URL>() {
                    @Override
                    public URL run() {
                        return clazz.getProtectionDomain().getCodeSource().getLocation();
                    }
                });
                out.append(", loaded from ");
                out.append(location.toString());
            } catch (final java.security.AccessControlException e) {
                /* Disregard and proceed */
            } catch (final NullPointerException e) {
                /* Probably a null code source. Disregard and proceed, in any
                 * case. */
            }
        }
        return out.toString();
    }

    protected void appendStackTrace(final Throwable t, final Appendable out) throws IOException {
        final String ourName = TestCase.class.getName().split("\\$")[0];

        final StackTraceElement[] trace = t.getStackTrace();
        int endTrace = trace.length;
        for (int frameNum = trace.length - 1, foundOurself = -1; frameNum >= 0; frameNum--) {
            if (trace[frameNum].getClassName().startsWith(ourName)) {
                foundOurself = frameNum;
            } else if (foundOurself != -1) {
                endTrace = foundOurself;
                break;
            }
        }

        out.append(t.toString()).append(newLine);
        for (int frameNum = 0; frameNum < endTrace; frameNum++) {
            out.append("\tat ").append(trace[frameNum].toString()).append(newLine);
        }
        if (endTrace < trace.length) {
            out.append("\t... ").append(String.valueOf(trace.length - endTrace)).append(" more").append(newLine);
        }
        final Throwable cause = t.getCause();
        if (cause != null) {
            appendStackTraceAsCause(cause, out, trace);
        }
    }

    private void appendStackTraceAsCause(final Throwable cause, final Appendable out, final StackTraceElement[] causedTrace) throws IOException {
        final StackTraceElement[] trace = cause.getStackTrace();
        int m = trace.length - 1, n = causedTrace.length - 1;
        while (m >= 0 && n >= 0 && trace[m].equals(causedTrace[n])) {
            m--;
            n--;
        }
        final int framesInCommon = trace.length - 1 - m;

        out.append("Caused by: ").append(cause.toString()).append(newLine);
        for (int i = 0; i <= m; i++) {
            out.append("\tat ").append(trace[i].toString()).append(newLine);
        }
        if (framesInCommon != 0) {
            out.append("\t... ").append(String.valueOf(framesInCommon)).append(" more").append(newLine);
        }
        final Throwable ourCause = cause.getCause();
        if (ourCause != null) {
            appendStackTraceAsCause(ourCause, out, trace);
        }
    }

    /**
     * Option that affect the level of detail in test log reports.
     * <p>
     * <code>ReportOption</code> has convenience <code>setOf</code>
     * methods that create sets of <code>ReportOption</code>.  
     */
    public enum ReportOption {
        ONE_LINE, FAIL_DETAIL_ONLY, NO_VALUES, ONE_LINE_SHOW_STACK;

        /** An empty <code>EnumSet&lt;ReportOption&gt;</code> instance */
        public static final Set<ReportOption> EMPTY_SET = EnumSet.noneOf(ReportOption.class);

        /** Convenience method for {@link EnumSet#of(Enum)} */
        public static Set<ReportOption> setOf(final ReportOption e) {
            return EnumSet.of(e);
        }

        /** Convenience method for {@link EnumSet#of(Enum, Enum)} */
        public static Set<ReportOption> setOf(final ReportOption e1, final ReportOption e2) {
            return EnumSet.of(e1, e2);
        }

        /** Convenience method for {@link EnumSet#of(Enum, Enum, Enum)} */
        public static Set<ReportOption> setOf(final ReportOption e1, final ReportOption e2, final ReportOption e3) {
            return EnumSet.of(e1, e2, e3);
        }

        /** Convenience method for {@link EnumSet#of(Enum, Enum, Enum, Enum)} */
        public static Set<ReportOption> setOf(final ReportOption e1, final ReportOption e2, final ReportOption e3, final ReportOption e4) {
            return EnumSet.of(e1, e2, e3, e4);
        }

        /** Convenience method for {@link EnumSet#of(Enum, Enum, Enum, Enum, Enum)} */
        public static Set<ReportOption> setOf(final ReportOption e1, final ReportOption e2, final ReportOption e3, final ReportOption e4, final ReportOption e5) {
            return EnumSet.of(e1, e2, e3, e4, e5);
        }

        /** Convenience method for {@link EnumSet#of(Enum, Enum...)} */
        public static Set<ReportOption> setOf(final ReportOption first, final ReportOption... rest) {
            return EnumSet.of(first, rest);
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
        REQUESTED_PERMISSIONS.add(new RuntimePermission("accessDeclaredMembers"));
        REQUESTED_PERMISSIONS.add(new RuntimePermission("getProtectionDomain"));
        REQUESTED_PERMISSIONS.setReadOnly();
    }

}
