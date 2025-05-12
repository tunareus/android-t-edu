package com.example.myapplication.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

class LibraryViewModelFactory(
    private val application: Application,
    private val getPagedLocalItemsUseCase: GetPagedLocalItemsUseCase,
    private val getTotalLocalItemCountUseCase: GetTotalLocalItemCountUseCase,
    private val addLocalItemUseCase: AddLocalItemUseCase,
    private val deleteLocalItemUseCase: DeleteLocalItemUseCase,
    private val getLocalItemByIdUseCase: GetLocalItemByIdUseCase,
    private val findLocalBookByIsbnUseCase: FindLocalBookByIsbnUseCase,
    private val findLocalBookByNameAndAuthorUseCase: FindLocalBookByNameAndAuthorUseCase,
    private val searchGoogleBooksUseCase: SearchGoogleBooksUseCase,
    private val saveGoogleBookToLocalLibraryUseCase: SaveGoogleBookToLocalLibraryUseCase,
    private val getSortPreferenceUseCase: GetSortPreferenceUseCase,
    private val setSortPreferenceUseCase: SetSortPreferenceUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(
                application,
                getPagedLocalItemsUseCase,
                getTotalLocalItemCountUseCase,
                addLocalItemUseCase,
                deleteLocalItemUseCase,
                getLocalItemByIdUseCase,
                findLocalBookByIsbnUseCase,
                findLocalBookByNameAndAuthorUseCase,
                searchGoogleBooksUseCase,
                saveGoogleBookToLocalLibraryUseCase,
                getSortPreferenceUseCase,
                setSortPreferenceUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}