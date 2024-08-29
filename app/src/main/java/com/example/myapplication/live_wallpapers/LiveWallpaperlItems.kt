package com.example.myapplication.live_wallpapers

import android.os.Parcel
import android.os.Parcelable

data class LiveWallpaperlItems(
    val imageUrl: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(imageUrl)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<LiveWallpaperlItems> {
        override fun createFromParcel(parcel: Parcel): LiveWallpaperlItems {
            return LiveWallpaperlItems(parcel)
        }

        override fun newArray(size: Int): Array<LiveWallpaperlItems?> {
            return arrayOfNulls(size)
        }
    }
}
