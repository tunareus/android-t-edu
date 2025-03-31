package com.example.myapplication

import androidx.lifecycle.ViewModel

class LibraryViewModel : ViewModel() {
    val libraryItems = initializeLibrary().toMutableList()
}