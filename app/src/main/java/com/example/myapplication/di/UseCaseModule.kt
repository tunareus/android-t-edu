package com.example.myapplication.di

import com.example.myapplication.domain.repository.GoogleBooksRepository
import com.example.myapplication.domain.repository.LocalLibraryRepository
import com.example.myapplication.domain.repository.SettingsRepository
import com.example.myapplication.domain.usecase.googlebooks.SaveGoogleBookToLocalLibraryUseCase
import com.example.myapplication.domain.usecase.googlebooks.SearchGoogleBooksUseCase
import com.example.myapplication.domain.usecase.library.*
import com.example.myapplication.domain.usecase.settings.GetSortPreferenceUseCase
import com.example.myapplication.domain.usecase.settings.SetSortPreferenceUseCase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DomainModule {

    @Provides
    @Singleton
    fun provideGetPagedLocalItemsUseCase(repository: LocalLibraryRepository): GetPagedLocalItemsUseCase {
        return GetPagedLocalItemsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetTotalLocalItemCountUseCase(repository: LocalLibraryRepository): GetTotalLocalItemCountUseCase {
        return GetTotalLocalItemCountUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideAddLocalItemUseCase(repository: LocalLibraryRepository): AddLocalItemUseCase {
        return AddLocalItemUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteLocalItemUseCase(repository: LocalLibraryRepository): DeleteLocalItemUseCase {
        return DeleteLocalItemUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetLocalItemByIdUseCase(repository: LocalLibraryRepository): GetLocalItemByIdUseCase {
        return GetLocalItemByIdUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideFindLocalBookByIsbnUseCase(repository: LocalLibraryRepository): FindLocalBookByIsbnUseCase {
        return FindLocalBookByIsbnUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideFindLocalBookByNameAndAuthorUseCase(repository: LocalLibraryRepository): FindLocalBookByNameAndAuthorUseCase {
        return FindLocalBookByNameAndAuthorUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSearchGoogleBooksUseCase(repository: GoogleBooksRepository): SearchGoogleBooksUseCase {
        return SearchGoogleBooksUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSaveGoogleBookToLocalLibraryUseCase(repository: LocalLibraryRepository): SaveGoogleBookToLocalLibraryUseCase {
        return SaveGoogleBookToLocalLibraryUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetSortPreferenceUseCase(repository: SettingsRepository): GetSortPreferenceUseCase {
        return GetSortPreferenceUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSetSortPreferenceUseCase(repository: SettingsRepository): SetSortPreferenceUseCase {
        return SetSortPreferenceUseCase(repository)
    }
}