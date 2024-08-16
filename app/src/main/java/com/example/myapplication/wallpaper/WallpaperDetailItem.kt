package com.example.myapplication.wallpaper

import android.os.Parcel
import android.os.Parcelable

data class WallpaperDetailItem(
    val imageUrl: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(imageUrl)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<WallpaperDetailItem> {
        override fun createFromParcel(parcel: Parcel): WallpaperDetailItem {
            return WallpaperDetailItem(parcel)
        }

        override fun newArray(size: Int): Array<WallpaperDetailItem?> {
            return arrayOfNulls(size)
        }
    }
}
