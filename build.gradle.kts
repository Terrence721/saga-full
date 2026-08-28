import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

// Removes every untracked .log file in the repo, found via git ls-files
// rather than a hand-maintained list of directories/filenames to skip -
// respects .gitignore automatically, and stays correct for any future
// .log file without this task needing changes.
//
// --exclude-standard alone drops any .log file that already matches a
// .gitignore pattern - and this repo's .gitignore has a blanket "*.log"
// rule, so on its own that call would always return nothing at all.
// Pairing it with --ignored flips to the opposite, ignored-only set;
// neither call alone covers every .log file, so both are unioned.
//
// Unlike the equivalent script in the conduit-full/coolify-full sibling
// repos, no vendor-directory exclusion is needed here: Gradle's
// dependency cache lives in ~/.gradle, outside this repo entirely, so
// there's no node_modules/vendor-style checked-in dependency tree to
// avoid reaching into.
tasks.register("cleanLogs") {
    group = "verification"
    description = "Removes every untracked .log file in the repo."

    doLast {
        fun gitLsFiles(ignoredOnly: Boolean): List<String> {
            val args = mutableListOf("git", "ls-files", "--others", "--exclude-standard")
            if (ignoredOnly) args.add("--ignored")
            args.add("*.log")

            val process = ProcessBuilder(args)
                .directory(rootDir)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            return output.lines().filter { it.isNotBlank() }
        }

        val logFiles = (gitLsFiles(false) + gitLsFiles(true)).distinct()

        if (logFiles.isEmpty()) {
            println("No log files found.")
        } else {
            logFiles.forEach { relativePath ->
                if (File(rootDir, relativePath).delete()) {
                    println("Removed $relativePath")
                }
            }
        }
    }
}

// Every service's bootRun (the actual "start this service" command
// documented in CONTRIBUTING.md) cleans stray logs first. Matched by task
// name rather than importing the Spring Boot plugin's BootRun class here
// in the root script - that plugin is applied per-service, not at the
// root, so referencing its class directly would need the root build
// script to carry that dependency too just to name a type.
subprojects {
    tasks.matching { it.name == "bootRun" }.configureEach {
        dependsOn(rootProject.tasks.named("cleanLogs"))
    }
}

// Gradle's own test-report-aggregation plugin needs to resolve every
// subproject's full dependency graph at the root (Spring Boot's BOM,
// the Spring gRPC BOM, and whatever future modules add) just to merge
// already-written XML files - an ongoing maintenance burden that grows
// with every module. This task instead just reads the JUnit XML each
// module's `test` task already writes and merges it into one HTML file,
// with zero dependency resolution of its own.
//
// Deliberately does NOT dependOn the subprojects' `test` tasks: a real
// test failure would then block this task too (a failed dependency
// always prevents a dependent task from running, `--continue` or not),
// so a single failing module would silently prevent the report from
// being generated at all. Run `./gradlew test --continue` first (which
// lets every module's tests run independently of one another), then
// `./gradlew aggregateTestReport` to merge whatever results exist,
// pass or fail - documented in CONTRIBUTING.md.
tasks.register("aggregateTestReport") {
    group = "verification"
    description = "Merges every module's JUnit XML test results (from the last `test` run) into one HTML report."

    // Gradle's directory-input validation requires the directory to exist,
    // and a module with no tests yet (NO-SOURCE) never creates one - so only
    // declare it as an input once it actually exists. Re-evaluated fresh on
    // every invocation, so a module gains input tracking the first time its
    // test-results directory shows up.
    //
    // mustRunAfter each module's `test` task (ordering only, not a real
    // dependsOn - doesn't force `test` to run and doesn't fail if it does):
    // without this, running `test` and `aggregateTestReport` in the same
    // invocation lets Gradle schedule this task before a module's `test`
    // task has actually written its results directory, which this task then
    // reads without any declared relationship to the task that produced it -
    // exactly the unsafe "implicit dependency" Gradle's own validation
    // flags, and it reproduces as a real, occasional build failure, not
    // just a warning.
    subprojects.forEach { sub ->
        val resultsDir = sub.layout.buildDirectory.dir("test-results/test").get().asFile
        if (resultsDir.exists()) {
            inputs.dir(resultsDir)
        }
        mustRunAfter(sub.tasks.named("test"))
    }

    val outputFile = layout.buildDirectory.file("reports/tests/aggregate/index.html")
    outputs.file(outputFile)

    doLast {
        data class Case(
            val module: String,
            val className: String,
            val name: String,
            val time: String,
            val status: String,
            val message: String?,
        )

        val cases = mutableListOf<Case>()

        subprojects.forEach { sub ->
            val resultsDir = sub.layout.buildDirectory.dir("test-results/test").get().asFile
            resultsDir.listFiles { f -> f.extension == "xml" }?.forEach { xmlFile ->
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
                val testcases = doc.getElementsByTagName("testcase")
                for (i in 0 until testcases.length) {
                    val tc = testcases.item(i) as Element
                    val failure = tc.getElementsByTagName("failure")
                    val error = tc.getElementsByTagName("error")
                    val skipped = tc.getElementsByTagName("skipped")
                    val (status, message) = when {
                        failure.length > 0 -> "FAILED" to (failure.item(0) as Element).getAttribute("message")
                        error.length > 0 -> "ERROR" to (error.item(0) as Element).getAttribute("message")
                        skipped.length > 0 -> "SKIPPED" to null
                        else -> "PASSED" to null
                    }
                    cases.add(
                        Case(
                            module = sub.name,
                            className = tc.getAttribute("classname"),
                            name = tc.getAttribute("name"),
                            time = tc.getAttribute("time"),
                            status = status,
                            message = message,
                        )
                    )
                }
            }
        }

        val total = cases.size
        val passed = cases.count { it.status == "PASSED" }
        val failed = cases.count { it.status == "FAILED" || it.status == "ERROR" }
        val skipped = cases.count { it.status == "SKIPPED" }

        fun esc(s: String?) = s.orEmpty()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        val rows = cases
            .sortedWith(compareBy({ it.module }, { it.className }, { it.name }))
            .joinToString("\n") { c ->
                val color = when (c.status) {
                    "PASSED" -> "#1a7f37"
                    "SKIPPED" -> "#9a6700"
                    else -> "#cf222e"
                }
                """
                <tr>
                    <td>${esc(c.module)}</td>
                    <td>${esc(c.className)}</td>
                    <td>${esc(c.name)}</td>
                    <td style="color:$color;font-weight:600">${c.status}</td>
                    <td>${esc(c.time)}s</td>
                    <td>${esc(c.message)}</td>
                </tr>
                """.trimIndent()
            }

        val html = """
        <!doctype html>
        <html>
        <head>
        <meta charset="utf-8">
        <title>saga-full - Aggregated Test Report</title>
        <style>
            body { font-family: -apple-system, "Segoe UI", sans-serif; margin: 2rem; color: #1f2328; }
            h1 { margin-bottom: 0.25rem; }
            .summary { margin-bottom: 1.5rem; font-size: 1.05rem; }
            .summary span { margin-right: 1.5rem; }
            table { border-collapse: collapse; width: 100%; }
            th, td { border: 1px solid #d0d7de; padding: 6px 10px; text-align: left; font-size: 0.9rem; }
            th { background: #f6f8fa; }
            tr:nth-child(even) { background: #fafbfc; }
        </style>
        </head>
        <body>
        <h1>saga-full - Aggregated Test Report</h1>
        <div class="summary">
            <span><strong>Total:</strong> $total</span>
            <span style="color:#1a7f37"><strong>Passed:</strong> $passed</span>
            <span style="color:#cf222e"><strong>Failed:</strong> $failed</span>
            <span style="color:#9a6700"><strong>Skipped:</strong> $skipped</span>
        </div>
        <table>
            <thead>
                <tr><th>Module</th><th>Class</th><th>Test</th><th>Status</th><th>Time</th><th>Message</th></tr>
            </thead>
            <tbody>
            $rows
            </tbody>
        </table>
        </body>
        </html>
        """.trimIndent()

        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(html)

        println("Aggregated test report written to: $file")
    }
}
