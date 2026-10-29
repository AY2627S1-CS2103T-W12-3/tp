---
  layout: default.md
  title: "Testing guide"
  pageNav: 3
---

# Testing guide

<!-- * Table of Contents -->
<page-nav-print />

<!-- -------------------------------------------------------------------------------------------------------------------- -->

## Running tests

You can run tests in two ways.

* **Method 1: Using IntelliJ JUnit test runner**
  * To run all tests, right-click on the `src/test/java` folder and choose `Run 'All Tests'`
  * To run a subset of tests, you can right-click on a test package,
    test class, or a test and choose `Run 'ABC'`
* **Method 2: Using Gradle**
  * Open a console and run the command `gradlew clean test` (Mac/Linux: `./gradlew clean test`)
  * To run the same checks as CI, run `gradlew check` (Mac/Linux: `./gradlew check`). Besides the tests, this runs the code style checks and fails if line or branch coverage is below the minimum set in `build.gradle` (see [Code coverage](DevOps.md#code-coverage)).

<box type="info" seamless>

**Link**: Read [this Gradle Tutorial from the se-edu/guides](https://se-education.org/guides/tutorials/gradle.html) to learn more about using Gradle.
</box>

### JavaFX UI tests

UI tests require a graphical display and are skipped in a headless environment. On Linux, run
`xvfb-run -a ./gradlew check coverage` to provide a virtual display and execute the CI checks.

When a test measures a shown dialog's content or viewport, wait for a JavaFX layout pulse before reading its bounds.
`Platform.runLater` alone does not guarantee that the window's layout has completed. `UiManagerTest` registers a
[post-layout pulse listener](https://openjfx.io/javadoc/17/javafx.graphics/javafx/scene/Scene.html#addPostLayoutPulseListener(java.lang.Runnable))
and requests a pulse for both short warnings that must fit without scrolling and long messages that must remain
scrollable. The listener is removed after the inspection, and the dialog is closed through `Platform.runLater`
so closing happens outside layout processing. Each test bounds its wait with `FutureTask.get` and a timeout.

--------------------------------------------------------------------------------------------------------------------

## Types of tests

This project has three types of tests:

1. *Unit tests* target the lowest-level methods and classes.<br>
   For example: `coordimate.commons.util.StringUtilTest`
1. *Integration tests* check how multiple code units work together; the individual units are assumed to work.<br>
   For example: `coordimate.storage.StorageManagerTest`
1. *Hybrid tests* combine unit and integration testing. These tests check both the individual units and how they work together.<br>
   For example: `coordimate.logic.LogicManagerTest`
