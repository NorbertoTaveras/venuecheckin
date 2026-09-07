package com.norbertotaveras.venuecheckin.beacon

class RssiSmoother(
    private val windowSize: Int = 5
) {
    private val values = ArrayDeque<Int>()

    fun add(rssi: Int): Int {
        values.addLast(rssi)

        if (values.size > windowSize) {
            values.removeFirst()
        }

        return values.average().toInt()
    }

    fun clear() {
        values.clear()
    }
}