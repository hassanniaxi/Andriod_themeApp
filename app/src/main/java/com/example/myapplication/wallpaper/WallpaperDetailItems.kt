package com.example.myapplication.wallpaper

import android.os.Parcel
import android.os.Parcelable

data class WallpaperDetailItems(
    val imageUrl: String= ""
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(imageUrl)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<WallpaperDetailItems> {
        override fun createFromParcel(parcel: Parcel): WallpaperDetailItems {
            return WallpaperDetailItems(parcel)
        }

        override fun newArray(size: Int): Array<WallpaperDetailItems?> {
            return arrayOfNulls(size)
        }
    }
}
