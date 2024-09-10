package com.example.myapplication.live_wallpapers.catalog

import android.os.Parcel
import android.os.Parcelable

data class CatLiveWallpaperItem(
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

    companion object CREATOR : Parcelable.Creator<CatLiveWallpaperItem> {
        override fun createFromParcel(parcel: Parcel): CatLiveWallpaperItem {
            return CatLiveWallpaperItem(parcel)
        }

        override fun newArray(size: Int): Array<CatLiveWallpaperItem?> {
            return arrayOfNulls(size)
        }
    }
}
