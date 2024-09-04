package com.example.myapplication.wallpaper.category

import android.os.Parcel
import android.os.Parcelable

data class CatWallpaperItem(
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

    companion object CREATOR : Parcelable.Creator<CatWallpaperItem> {
        override fun createFromParcel(parcel: Parcel): CatWallpaperItem {
            return CatWallpaperItem(parcel)
        }

        override fun newArray(size: Int): Array<CatWallpaperItem?> {
            return arrayOfNulls(size)
        }
    }
}
