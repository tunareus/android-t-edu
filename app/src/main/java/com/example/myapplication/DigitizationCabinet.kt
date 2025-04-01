package com.example.myapplication

class DigitizationCabinet<in T : Digitizable> {
    fun digitize(item: T): Disk {
        return Disk(
            id = (10000..99999).random(),
            available = true,
            name = "Цифровая копия: ${item.getDigitizableName()}",
            diskType = "CD"
        )
    }
}