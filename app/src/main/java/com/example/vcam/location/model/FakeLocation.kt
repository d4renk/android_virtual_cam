package com.example.vcam.location.model

class FakeLocation(
    val x: Double,
    val y: Double,
    val offset:Double,

    val eci: Int,
    val pci: Int,
    val tac: Int,
    val earfcn: Int,
    val bandwidth: Int
)

// Used for migrate from older version
class FakeLocationHistory(
    val x: Double,
    val y: Double,
)
