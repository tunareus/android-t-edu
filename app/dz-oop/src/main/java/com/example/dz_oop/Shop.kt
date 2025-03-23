package com.example.dz_oop

interface Shop<T : LibraryItem> {
    fun sell(): T
}