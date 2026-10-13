package com.agroroute.demo

import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Structural boundary checks complement the device persistence tests. */
class DemoIsolationTest {
    private fun app(): File = generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
        .map { if (File(it,"src/main").isDirectory) it else File(it,"app") }
        .first { File(it,"src/debug/java/com/agroroute/demo").isDirectory }
    @Test fun demoCodeHasNoRemoteRepositoryOrAuthenticationDependencies() {
        val source = File(app(),"src/debug/java/com/agroroute/demo").walkTopDown()
            .filter { it.name in listOf("DemoDataset.kt","DemoDataSeeder.kt","DemoActivity.kt") }.joinToString("\n") { it.readText() }
        listOf("import com.google.firebase", "import com.agroroute.core.repository", "import com.agroroute.core.remote",
            "clearAllTables(", "FirebaseAuth", "FirestoreService").forEach { assertFalse(it,source.contains(it)) }
    }
    @Test fun releaseAndMainHaveNoDemoEntryOrAutomaticSeedCalls() {
        listOf("main","release").forEach { variant ->
            File(app(),"src/$variant").walkTopDown().filter { it.isFile && it.extension in listOf("kt","xml") }.forEach {
                assertFalse(it.path,it.readText().contains("com.agroroute.demo"))
                assertFalse(it.path,it.readText().contains("loadDemoData("))
            }
        }
        assertTrue(File(app(),"src/debug/AndroidManifest.xml").readText().contains(".demo.DemoActivity"))
    }
}
