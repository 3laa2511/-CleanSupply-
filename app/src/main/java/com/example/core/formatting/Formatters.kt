package com.example.core.formatting

import androidx.annotation.DrawableRes
import com.example.R
import com.google.firebase.Timestamp
import java.text.DateFormat
import java.util.Locale

object Formatters {
    fun number(amount: Double): String =
        if (amount % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", amount)
        } else {
            String.format(Locale.US, "%.2f", amount)
        }

    fun money(amount: Double, currency: String): String = "${number(amount)} $currency"

    fun timestamp(ts: Timestamp?, isArabic: Boolean): String {
        if (ts == null) return ""
        val locale = if (isArabic) Locale("ar", "EG") else Locale.US
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
            .format(ts.toDate())
    }

    @DrawableRes
    fun productDrawable(key: String): Int = when (key) {
        "paper" -> R.drawable.img_cat_paper
        "tools" -> R.drawable.img_cat_tools
        else -> R.drawable.img_cat_detergents
    }
}
