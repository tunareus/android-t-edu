package com.example.myapplication

class Manager {
    fun <T : LibraryItem> buy(shop: Shop<T>): T {
        return shop.sell()
    }
}