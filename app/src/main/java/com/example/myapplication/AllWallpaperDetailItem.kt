package com.example.myapplication

import android.os.Parcel
import android.os.Parcelable

data class AllWallpaperDetailItem(
    val imageUrl: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(imageUrl)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AllWallpaperDetailItem> {
        override fun createFromParcel(parcel: Parcel): AllWallpaperDetailItem {
            return AllWallpaperDetailItem(parcel)
        }

        override fun newArray(size: Int): Array<AllWallpaperDetailItem?> {
            return arrayOfNulls(size)
        }
    }
}
