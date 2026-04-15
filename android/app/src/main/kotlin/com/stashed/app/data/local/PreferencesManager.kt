package com.stashed.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stashed_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context,
) {
    companion object {
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val KEY_MODEL_DOWNLOADED = booleanPreferencesKey("model_downloaded")
        private val KEY_PREMIUM_ACTIVE = booleanPreferencesKey("premium_active")
    }

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_ONBOARDING_COMPLETE] ?: false }

    suspend fun setOnboardingComplete() {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETE] = true }
    }

    val isModelDownloaded: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_MODEL_DOWNLOADED] ?: false }

    suspend fun setModelDownloaded() {
        context.dataStore.edit { it[KEY_MODEL_DOWNLOADED] = true }
    }

    val isPremiumActive: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_PREMIUM_ACTIVE] ?: false }

    suspend fun setPremiumActive(active: Boolean) {
        context.dataStore.edit { it[KEY_PREMIUM_ACTIVE] = active }
    }
}
