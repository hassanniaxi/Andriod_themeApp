package com.example.myapplication.live_wallpapers

import android.os.Parcel
import android.os.Parcelable

data class LiveWallpaperItems(
    val imageUrl: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(imageUrl)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<LiveWallpaperItems> {
        override fun createFromParcel(parcel: Parcel): LiveWallpaperItems {
            return LiveWallpaperItems(parcel)
        }

        override fun newArray(size: Int): Array<LiveWallpaperItems?> {
            return arrayOfNulls(size)
        }
    }
}
