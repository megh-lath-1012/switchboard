package dev.meghlath.switchboard.android

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.meghlath.switchboard.core.OverrideStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "switchboard_overrides")

/**
 * Android persistent override storage implementation backed by Jetpack DataStore Preferences.
 *
 * Persists evaluated debug overrides into `"switchboard_overrides.preferences_pb"`, ensuring mutex-protected
 * concurrent disk access, efficient Protobuf serialization, and non-blocking reactive invalidation streams.
 *
 * Exposes secondary internal dependency injection constructors to support highly performant pure JVM Approach B
 * testing factories backed by standard file targets.
 */
public class DataStoreOverrideStorage internal constructor(
    private val dataStore: DataStore<Preferences>
) : OverrideStorage {

    /**
     * Initializes the override storage layer using the standard application-scoped DataStore delegate.
     *
     * @param context The Android context utilized to fetch the preferences delegate singleton.
     */
    public constructor(context: Context) : this(
        dataStore = context.dataStore
    )

    override suspend fun read(): Map<String, String> {
        return dataStore.data.map { prefs ->
            prefs.asMap().mapKeys { it.key.name }.mapValues { it.value.toString() }
        }.first()
    }

    override suspend fun write(key: String, value: String?) {
        val prefKey = stringPreferencesKey(key)
        dataStore.edit { prefs ->
            if (value == null) {
                prefs.remove(prefKey)
            } else {
                prefs[prefKey] = value
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    override fun changes(): Flow<Map<String, String>> {
        return dataStore.data.map { prefs ->
            prefs.asMap().mapKeys { it.key.name }.mapValues { it.value.toString() }
        }
    }
}
