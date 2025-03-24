package com.example.dz_oop

class DiskShop : Shop<Disk> {
    override fun sell(): Disk {
        return Disk(
            id = (10000..99999).random(),
            available = true,
            name = "Новый диск",
            diskType = "CD"
        )
    }
}