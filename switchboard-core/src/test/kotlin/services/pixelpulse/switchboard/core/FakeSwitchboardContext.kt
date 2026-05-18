package services.pixelpulse.switchboard.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

public class FakeOverrideStorage : OverrideStorage {
    private val storage = ConcurrentHashMap<String, String>()
    private val changesFlow = MutableSharedFlow<Map<String, String>>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override suspend fun read(): Map<String, String> = HashMap(storage)

    override suspend fun write(key: String, value: String?) {
        if (value != null) {
            storage[key] = value
        } else {
            storage.remove(key)
        }
        changesFlow.tryEmit(HashMap(storage))
    }

    override suspend fun clear() {
        storage.clear()
        changesFlow.tryEmit(HashMap(storage))
    }

    override fun changes(): Flow<Map<String, String>> = changesFlow.asSharedFlow()

    public fun setOverride(key: String, value: String?) {
        if (value != null) {
            storage[key] = value
        } else {
            storage.remove(key)
        }
        changesFlow.tryEmit(HashMap(storage))
    }
}

public class FakeSwitchboardContext(
    private val fakeStorage: FakeOverrideStorage = FakeOverrideStorage()
) : SwitchboardContext {

    override fun overrideStorage(): OverrideStorage = fakeStorage

    public fun getFakeStorage(): FakeOverrideStorage = fakeStorage
}
