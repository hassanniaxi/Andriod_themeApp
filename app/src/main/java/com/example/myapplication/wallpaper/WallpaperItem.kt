package com.example.myapplication.wallpaper

import android.os.Parcel
import android.os.Parcelable

data class WallpaperItem(
    val title: String,
    val imageUrl: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(imageUrl)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<WallpaperItem> {
        override fun createFromParcel(parcel: Parcel): WallpaperItem {
            return WallpaperItem(parcel)
        }

        override fun newArray(size: Int): Array<WallpaperItem?> {
            return arrayOfNulls(size)
        }
    }
}
