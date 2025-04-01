package com.example.myapplication


class NewspaperKiosk : Shop<Newspaper> {
    override fun sell(): Newspaper {
        return Newspaper(
            id = (10000..99999).random(),
            available = true,
            name = "Новая газета",
            issueNumber = (100..999).random(),
            month = Month.entries.random()
        )
    }
}