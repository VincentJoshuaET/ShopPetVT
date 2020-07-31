package com.vt.shoppet.util

import android.graphics.drawable.Drawable
import android.widget.ArrayAdapter
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import com.vt.shoppet.R
import java.util.*

@ExperimentalStdlibApi
fun String.capitalizeWords() =
    split(" ").joinToString(" ") {
        it.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault())
    }

fun Fragment.getArrayAdapter(array: Array<String>) =
    ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, array)

fun Fragment.getDrawable(@DrawableRes id: Int): Drawable =
    resources.getDrawable(id, requireContext().theme)