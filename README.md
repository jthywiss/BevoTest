# BevoTest #

The BevoTest framework is a unit testing framework. It executes test procedures on test items. Each test procedure's input and expected result is specified by a test case. An execution of a test records test execution results in a test log.

BevoTest handles test cases that expect returned values or expect particular exceptions to be thrown. It is robust to test items that throw unexpected exceptions or errors, that don't timely terminate (infinite looping or very slow test items), and that attempt security violations.

The BevoTest framework conforms to _IEEE Std 829, IEEE Standard for Software Test Documentation_.

## Availability ##

BevoTest is licensed under the Apache License, Version 2.0.

A source code JAR and a class file JAR are available.  See the [latest release](https://github.com/jthywiss/BevoTest/releases/latest) for the files to download.

## Documentation ##

The framework is documented on this page and in [BevoTest JavaDoc](https://www.cs.utexas.edu/~jthywiss/bevotest-doc/index.html).

## Usage ##

There are four steps to using BevoTest:

1. **Create a `Test`**: This is just a named container for test cases.
2. **Specify test cases**: Create some instances of `TestReturns` or `TestThrows`. Merely creating one of these with the correct arguments adds the test case to the test.
3. **Run test**: Create a `TestLog` and hand it to the `Test.run` method.
4. **Report results**: Use a test reporter on the test log. There is a `PlaintextTestReporter` supplied with BevoTest, or supply your own.

Here is a file with examples of BevoTest being used: [ExampleTest.java](https://www.cs.utexas.edu/~jthywiss/ExampleTest.java)

## Test Cases ##

To specify a test case expecting a particular return value:

```Java
new BevoTest.TestReturns<String, Integer>(ts, String.class, "Length of String", 14, 2000L) {
    @Override public void executeTest() {
        // Set up:
        final String testItem = "Test test test";
        // Execute:
        starting(testItem);
        returned(testItem.length());
        // Any tear down here
    }
};
```

The arguments to the `TestReturns` constructor are two type arguments: the type being tested and the expected value type; and five value arguments: the `Test`, the `Class` value of the type being tested, the test case description, expected return value, and timeout in milliseconds. In the body of the test procedure, there are three phases: 1) set up of the test case, 2) execution of the item being tested, and 3) tear down (clean up). The calls to `starting` and `returned` divide the phases of the test procedure.

If the test case, instead of expecting a returned value, expects a particular type of exception to be thrown, construct a `TestThrows` instead of a `TestReturns`.

If the method being tested is a static method (or a constructor invocation, or a static initializer, or the variable initializer of a static variable), where there is no test item instance to pass to `starting`, call `startingStatic` instead, passing the `Class` under test.

If the test case may need to be skipped under certain conditions, override `shouldSkip()`.

There is more detail in the [BevoTest JavaDoc](https://www.cs.utexas.edu/~jthywiss/bevotest-doc/index.html).

## Results ##

Test results are recorded in a test log. The log keeps some environmental information, a test start and stop time, and a collection of test log entries. Specifically, the environmental description includes: JVM version, heap size, OS, class path, and working directory.

Test results entries in the log keep a reference to the test case, the type of the item tested, the actual returned value or any exception thrown, the time the test took to run, a test status, and a test evaluation (pass/fail). Specifically, the statuses are: Enqueued, Running setup, Running processing, Running teardown, Skipped, Complete normal, Complete abnormal, and Timed out.

## Utilities ##

To convert a test log into a plain text test report, use the `PlaintextTestReporter` class. This class has a few options to change the output format, and you can write your own reporter if you like.

The `TestLog` class provides hooks for a status UI: subclass and override the three `notify...` methods. The log and entries are thread safe.

The framework also supplies a security policy utility class that simplifies adding permissions to the policy, without the hassle of a security policy configuration file.

There is more detail in the [BevoTest JavaDoc](https://www.cs.utexas.edu/~jthywiss/bevotest-doc/index.html).
