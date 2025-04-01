package com.example.myapplication

class BookShop : Shop<Book> {
    override fun sell(): Book {
        return Book(
            id = (10000..99999).random(),
            available = true,
            name = "Новая книга",
            pages = 200,
            author = "Автор книги"
        )
    }
}