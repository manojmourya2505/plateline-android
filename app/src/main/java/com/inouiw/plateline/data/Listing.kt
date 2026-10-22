package com.inouiw.plateline.data

data class Listing(val plate: String, val car: String, val price: Int) {
    // price is the middle of the AI estimate range. 0 means no estimate yet.
    val priceText: String get() = if (price <= 0) "Price pending" else "₹%,d (estimate)".format(price)
}
