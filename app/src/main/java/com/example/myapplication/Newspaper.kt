package com.example.myapplication

import kotlinx.parcelize.Parcelize

@Parcelize
class Newspaper(
    override val id: Int,
    override var available: Boolean,
    override val name: String,
    val issueNumber: Int,
    val month: Month
) : LibraryItem(id, available, name) { //

}