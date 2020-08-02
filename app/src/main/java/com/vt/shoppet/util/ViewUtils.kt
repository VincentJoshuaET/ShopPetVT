package com.vt.shoppet.util

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import androidx.annotation.MenuRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.android.material.snackbar.Snackbar
import com.vt.shoppet.R
import com.vt.shoppet.databinding.ActivityMainBinding

fun Fragment.circularProgress(): Animatable {
    val value = TypedValue()
    val context = requireContext()
    context.theme.resolveAttribute(android.R.attr.progressBarStyleSmall, value, false)
    val array = intArrayOf(android.R.attr.indeterminateDrawable)
    val attributes = context.obtainStyledAttributes(value.data, array)
    val drawable = attributes.getDrawable(0)
    attributes.recycle()
    return drawable as Animatable
}

fun Fragment.circularProgressLarge() =
    CircularProgressDrawable(requireContext()).apply {
        setStyle(CircularProgressDrawable.LARGE)
        setColorSchemeColors(Color.WHITE)
        start()
    }

fun Fragment.showSnackbar(message: String) =
    Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()

fun Fragment.topSnackbar(message: String) =
    Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).apply {
        val params = view.layoutParams as CoordinatorLayout.LayoutParams
        params.gravity = Gravity.TOP
        view.layoutParams = params
    }

fun Fragment.showSnackbar(exception: Throwable) {
    val message = exception.localizedMessage ?: return
    Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
}

fun Fragment.showActionSnackbar(message: String, action: (View) -> Unit) =
    Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)
        .setAction(R.string.btn_retry, action).show()

fun Fragment.showActionSnackbar(exception: Throwable, action: (View) -> Unit) {
    val message = exception.localizedMessage ?: return
    Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)
        .setAction(R.string.btn_retry, action).show()
}

fun Fragment.setLayout(orientation: Int) = when (orientation) {
    Configuration.ORIENTATION_LANDSCAPE -> GridLayoutManager(context, 3)
    else -> GridLayoutManager(context, 2)
}

fun Button.popBackStackOnClick() = setOnClickListener {
    findNavController().popBackStack()
}

fun RecyclerView.setOnLayoutChangeListener() =
    addOnLayoutChangeListener { _, _, top, _, _, _, oldTop, _, _ ->
        if (top < oldTop) smoothScrollToPosition(oldTop)
    }

fun ActivityMainBinding.setupAuthView() =
    apply {
        appbar.isVisible = false
        bottomNavigationView.isVisible = false
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        toolbar.menu.clear()
    }

fun ActivityMainBinding.setupHomeNavigationView() =
    apply {
        appbar.isVisible = true
        bottomNavigationView.isVisible = true
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        toolbar.menu.clear()
    }

fun ActivityMainBinding.setupHomeNavigationView(@MenuRes menu: Int) =
    apply {
        appbar.isVisible = true
        bottomNavigationView.isVisible = true
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        toolbar.menu.clear()
        toolbar.inflateMenu(menu)
    }

fun ActivityMainBinding.setupToolbar() =
    apply {
        appbar.isVisible = true
        bottomNavigationView.isVisible = false
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        toolbar.menu.clear()
    }

fun ActivityMainBinding.setupToolbar(@MenuRes menu: Int) =
    apply {
        appbar.isVisible = true
        bottomNavigationView.isVisible = false
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        toolbar.menu.clear()
        toolbar.inflateMenu(menu)
    }

fun ActivityMainBinding.showSnackbar(message: String) =
    Snackbar.make(fragment, message, Snackbar.LENGTH_SHORT).show()

fun ActivityMainBinding.showActionSnackbar(message: String, action: (View) -> Unit) =
    Snackbar.make(fragment, message, Snackbar.LENGTH_SHORT)
        .setAction(R.string.btn_view, action)
        .show()

fun ActivityMainBinding.showActionSnackbar(exception: Throwable, action: (View) -> Unit) {
    val message = exception.localizedMessage ?: return
    Snackbar.make(fragment, message, Snackbar.LENGTH_SHORT)
        .setAction(R.string.btn_retry, action)
        .show()
}