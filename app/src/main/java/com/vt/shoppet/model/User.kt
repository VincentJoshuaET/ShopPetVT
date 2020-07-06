package com.vt.shoppet.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(
    var uid: String = "",
    var name: String = "",
    var username: String = "",
    var mobile: String = "",
    var location: String = "",
    var sex: String = "",
    var dateOfBirth: Timestamp = Timestamp.now(),
    var image: String? = null,
    var tokens: MutableList<String> = mutableListOf(),
    var reports: Int = 0
) : Parcelable