package com.inouiw.plateline.data

data class Profile(
    val phone: String,
    val name: String,
    val email: String,
    val pin: String,
) {
    /** "MANOJ MOURYA NEELI" -> "Manoj" */
    val firstName: String
        get() = name.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
            .lowercase().replaceFirstChar { it.uppercase() }
    val initial: String get() = name.trim().firstOrNull()?.uppercase().orEmpty()
    val displayPhone: String
        get() = if (phone.length == 13 && phone.startsWith("+91")) "+91 ${phone.substring(3, 8)} ${phone.substring(8)}" else phone
}
