package com.example.dz_oop

class Manager {
    fun <T : LibraryItem> buy(shop: Shop<T>): T {
        return shop.sell()
    }
}