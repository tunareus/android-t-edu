package com.example.myapplication

import android.app.Application
import com.example.myapplication.data.local.db.AppDatabase
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.data.repository.GoogleBooksRepositoryImpl
import com.example.myapplication.data.repository.LocalLibraryRepositoryImpl
import com.example.myapplication.data.settings.SettingsRepositoryImpl
import com.example.myapplication.domain.repository.GoogleBooksRepository
import com.example.myapplication.domain.repository.LocalLibraryRepository
import com.example.myapplication.domain.repository.SettingsRepository
import com.example.myapplication.domain.usecase.googlebooks.SaveGoogleBookToLocalLibraryUseCase
import com.example.myapplication.domain.usecase.googlebooks.SearchGoogleBooksUseCase
import com.example.myapplication.domain.usecase.library.AddLocalItemUseCase
import com.example.myapplication.domain.usecase.library.DeleteLocalItemUseCase
import com.example.myapplication.domain.usecase.library.FindLocalBookByIsbnUseCase
import com.example.myapplication.domain.usecase.library.FindLocalBookByNameAndAuthorUseCase
import com.example.myapplication.domain.usecase.library.GetLocalItemByIdUseCase
import com.example.myapplication.domain.usecase.library.GetPagedLocalItemsUseCase
import com.example.myapplication.domain.usecase.library.GetTotalLocalItemCountUseCase
import com.example.myapplication.domain.usecase.settings.GetSortPreferenceUseCase
import com.example.myapplication.domain.usecase.settings.SetSortPreferenceUseCase

class MyApplication : Application() {

    private val appDatabase: AppDatabase by lazy { AppDatabase.getDatabase(applicationContext) }
    private val libraryItemDao by lazy { appDatabase.libraryItemDao() }
    private val googleBooksApiService by lazy { RetrofitClient.googleBooksApi }

    internal val localLibraryRepository: LocalLibraryRepository by lazy { LocalLibraryRepositoryImpl(libraryItemDao) }
    internal val googleBooksRepository: GoogleBooksRepository by lazy { GoogleBooksRepositoryImpl(googleBooksApiService) }
    internal val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(applicationContext) }

    val getPagedLocalItemsUseCase by lazy { GetPagedLocalItemsUseCase(localLibraryRepository) }
    val getTotalLocalItemCountUseCase by lazy { GetTotalLocalItemCountUseCase(localLibraryRepository) }
    val addLocalItemUseCase by lazy { AddLocalItemUseCase(localLibraryRepository) }
    val deleteLocalItemUseCase by lazy { DeleteLocalItemUseCase(localLibraryRepository) }
    val getLocalItemByIdUseCase by lazy { GetLocalItemByIdUseCase(localLibraryRepository) }
    val findLocalBookByIsbnUseCase by lazy { FindLocalBookByIsbnUseCase(localLibraryRepository) }
    val findLocalBookByNameAndAuthorUseCase by lazy { FindLocalBookByNameAndAuthorUseCase(localLibraryRepository) }

    val searchGoogleBooksUseCase by lazy { SearchGoogleBooksUseCase(googleBooksRepository) }
    val saveGoogleBookToLocalLibraryUseCase by lazy { SaveGoogleBookToLocalLibraryUseCase(localLibraryRepository) }

    val getSortPreferenceUseCase by lazy { GetSortPreferenceUseCase(settingsRepository) }
    val setSortPreferenceUseCase by lazy { SetSortPreferenceUseCase(settingsRepository) }
}