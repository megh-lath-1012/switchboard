package dev.meghlath.switchboard.android

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

public class DataStoreOverrideStorageTest {

    @JvmField
    @TempDir
    public var tempDir: File = File("")

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var storage: DataStoreOverrideStorage

    @BeforeEach
    public fun setup() {
        dataStoreScope = CoroutineScope(Dispatchers.IO + Job())
        testDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { File(tempDir, "test.preferences_pb") }
        )
        storage = DataStoreOverrideStorage(testDataStore)
    }

    @AfterEach
    public fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    public fun testWriteThenReadRoundTrip(): Unit = runBlocking {
        storage.write("boolKey", "true")
        storage.write("intKey", "99")
        storage.write("strKey", "override_string")

        val overrides = storage.read()
        overrides["boolKey"] shouldBe "true"
        overrides["intKey"] shouldBe "99"
        overrides["strKey"] shouldBe "override_string"

        // Null write removes the entry entirely
        storage.write("intKey", null)
        storage.read()["intKey"] shouldBe null
    }

    @Test
    public fun testClearRemovesAllEntries(): Unit = runBlocking {
        storage.write("flagA", "valA")
        storage.write("flagB", "valB")
        storage.read().size shouldBe 2

        storage.clear()
        storage.read().size shouldBe 0
    }

    @Test
    public fun testChangesFlowEmitsOnEveryWriteNoDebouncing(): Unit = runBlocking {
        storage.changes().test {
            // Initial map emitted upon start of collection
            awaitItem()

            storage.write("key1", "val1")
            awaitItem()["key1"] shouldBe "val1"

            // Two consecutive writes executed back-to-back within 10ms
            storage.write("key2", "val2")
            awaitItem()["key2"] shouldBe "val2"

            storage.write("key3", "val3")
            awaitItem()["key3"] shouldBe "val3"

            cancelAndIgnoreRemainingEvents()
        }
    }
}
