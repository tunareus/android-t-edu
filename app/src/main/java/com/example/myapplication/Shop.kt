package com.example.myapplication

interface Shop<out T : LibraryItem> {
    fun sell(): T
}