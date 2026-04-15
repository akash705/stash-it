package com.stashed.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stashed.app.billing.BillingManager
import com.stashed.app.data.local.AppDatabase
import com.stashed.app.data.local.PreferencesManager
import com.stashed.app.intelligence.MiniLMEmbedder
import com.stashed.app.intelligence.WordPieceTokenizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // Load the sqlite-vec native library before Room opens the database.
        // The .so files are in jniLibs/ and compiled from sqlite-vec source.
        // If the .so is not yet compiled, this will throw UnsatisfiedLinkError —
        // the app will fall back to FTS5-only search in that case.
        try {
            System.loadLibrary("vec0")
        } catch (e: UnsatisfiedLinkError) {
            // sqlite-vec not available — app continues with FTS5 keyword search only
        }

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Create virtual tables on every open (IF NOT EXISTS is safe to repeat).
                    // This handles first launch AND cases where onCreate already ran.
                    AppDatabase.CREATE_VIRTUAL_TABLES.forEach { sql ->
                        try {
                            db.execSQL(sql)
                        } catch (e: Exception) {
                            // sqlite-vec table will fail if native lib isn't loaded — that's fine.
                            // FTS5 should always succeed.
                        }
                    }
                }
            })
            .addMigrations(AppDatabase.MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideWordPieceTokenizer(@ApplicationContext context: Context): WordPieceTokenizer {
        return WordPieceTokenizer(context)
    }

    @Provides
    @Singleton
    fun provideMiniLMEmbedder(
        @ApplicationContext context: Context,
        tokenizer: WordPieceTokenizer,
    ): MiniLMEmbedder {
        return MiniLMEmbedder(context, tokenizer)
    }

    @Provides
    @Singleton
    fun provideBillingManager(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager,
    ): BillingManager {
        return BillingManager(context, preferencesManager)
    }
}
