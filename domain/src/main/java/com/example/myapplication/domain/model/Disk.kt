package com.example.myapplication.domain.model

import kotlinx.parcelize.Parcelize

@Parcelize
class Disk(
    override val id: Int,
    override var available: Boolean,
    override val name: String,
    private val diskType: String
) : LibraryItem(id, available, name) {
    fun getDiskType(): String = diskType
}