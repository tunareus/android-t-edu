package com.example.myapplication.di

import android.app.Application
import com.example.myapplication.data.local.db.AppDatabase
import com.example.myapplication.data.local.db.LibraryItemDao
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.data.remote.api.GoogleBooksApiService
import com.example.myapplication.data.repository.GoogleBooksRepositoryImpl
import com.example.myapplication.data.repository.LocalLibraryRepositoryImpl
import com.example.myapplication.data.settings.SettingsRepositoryImpl
import com.example.myapplication.domain.repository.GoogleBooksRepository
import com.example.myapplication.domain.repository.LocalLibraryRepository
import com.example.myapplication.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(application: Application): AppDatabase {
        return AppDatabase.getDatabase(application)
    }

    @Provides
    @Singleton
    fun provideLibraryItemDao(appDatabase: AppDatabase): LibraryItemDao {
        return appDatabase.libraryItemDao()
    }

    @Provides
    @Singleton
    fun provideGoogleBooksApiService(): GoogleBooksApiService {
        return RetrofitClient.googleBooksApi
    }

    @Provides
    @Singleton
    fun provideLocalLibraryRepository(libraryItemDao: LibraryItemDao): LocalLibraryRepository {
        return LocalLibraryRepositoryImpl(libraryItemDao)
    }

    @Provides
    @Singleton
    fun provideGoogleBooksRepository(apiService: GoogleBooksApiService): GoogleBooksRepository {
        return GoogleBooksRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(application: Application): SettingsRepository {
        return SettingsRepositoryImpl(application)
    }
}