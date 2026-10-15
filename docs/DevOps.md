---
  layout: default.md
  title: "DevOps guide"
  pageNav: 3
---

# DevOps guide

<!-- * Table of Contents -->
<page-nav-print />

<!-- -------------------------------------------------------------------------------------------------------------------- -->

## Build automation

This project uses Gradle for **build automation and dependency management**. **We recommend reading [this Gradle tutorial from se-edu/guides](https://se-education.org/guides/tutorials/gradle.html).**


The following commands perform common Gradle tasks.


* **`clean`**: Deletes the files created during the previous build tasks (e.g. files in the `build` folder).<br>
  For example: `./gradlew clean`

* **`shadowJar`**: Uses the Shadow plugin to create the fat JAR file `build/libs/coordimate.jar`.<br>
  For example: `./gradlew shadowJar`

* **`run`**: Builds and runs the application.<br>
  **`runShadow`**: Builds the application as a fat JAR, then runs it.

* **`checkstyleMain`**: Runs the code style check for the main code base.<br>
  **`checkstyleTest`**: Runs the code style check for the test code base.

* **`test`**
  * `./gradlew test`: Runs all tests.
  * `./gradlew clean test`: Cleans the project before running all tests

* **`coverage`**: Runs all tests, then writes the JaCoCo coverage report to `build/reports/jacoco/coverage/html/index.html`.<br>
  **`coverageVerification`**: Runs all tests, then fails if line or branch coverage is below the minimum (see [Code coverage](#code-coverage)).

* **`check`**: Runs all tests, both code style checks, and the coverage check. CI runs this command.<br>
  For example: `./gradlew check`

--------------------------------------------------------------------------------------------------------------------

## Continuous integration (CI)

This project uses GitHub Actions for CI. The necessary workflow configuration files are in `.github/workflows`. No further setup is required.

### Code coverage

As part of CI, Gradle generates JaCoCo coverage reports from the tests, and CI uploads the coverage data to Codecov. Codecov then provides information about test coverage, such as the coverage of the lines a PR changes.

**Minimum coverage:** `./gradlew check` fails if overall **line or branch coverage is below 90%**. CI runs `check` on every push and PR, so a change that drops coverage below this minimum fails CI and must add tests before it can be merged. The minimum is set by `minimumCoverage` in `build.gradle`.

* To check coverage before pushing, run `./gradlew check`. A failure names the counter that is too low, e.g. `Rule violated for bundle tp: branches covered ratio is 0.88, but expected minimum is 0.90`.
* To find the untested code, run `./gradlew coverage` and open `build/reports/jacoco/coverage/html/index.html`.
* Raise the minimum as coverage improves. Do not lower it to make a PR pass; add tests instead.

Because Codecov is known to run into intermittent problems (e.g., report upload fails) due to issues on the Codecov service side, the CI is configured to pass even if the Codecov upload fails. The minimum coverage check runs in Gradle and does not depend on Codecov.

To enable Codecov for forks of this project, follow the steps given in [this se-edu guide](https://se-education.org/guides/tutorials/codecov.html).

### Repository-wide checks

In addition to Gradle checks, CI runs repository-wide checks. Unlike Gradle checks, which cover files used in the build, these checks cover every repository file and enforce rules that are hard to apply on development machines, such as line-ending requirements.

These checks are implemented as POSIX shell scripts, and thus can only be run on POSIX-compliant operating systems such as macOS and Linux. To run all checks locally on these operating systems, execute the following in the repository root directory:

`./.github/run-checks.sh`

Any warnings or errors will be printed out to the console.

**If adding new checks:**

* Checks are implemented as executable `check-*` scripts within the `.github` directory. The `run-checks.sh` script will automatically pick up and run files named as such. That is, you can add more such files if you need and the CI will do the rest.

* Check scripts should print out errors in the format `SEVERITY:FILENAME:LINE: MESSAGE`
  * SEVERITY is either ERROR or WARN.
  * FILENAME is the path to the file relative to the current directory.
  * LINE is the line of the file where the error occurred and MESSAGE is the message explaining the error.

* Check scripts must exit with a non-zero exit code if any errors occur.

--------------------------------------------------------------------------------------------------------------------

## Making a release

Here are the steps to create a new release.

1. Update the version number in [`MainApp.java`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/MainApp.java).
1. Generate a fat JAR file using Gradle (i.e., `./gradlew shadowJar`).
1. Tag the repo with the version number. e.g. `v0.1`
1. [Create a new release using GitHub](https://help.github.com/articles/creating-releases/). Upload the JAR file you created.
