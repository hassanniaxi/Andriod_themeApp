package com.example.myapplication.ringtone

import android.os.Parcel
import android.os.Parcelable

data class RingtoneItem(
    val title: String,
    val resourceId: String, // Use music URL instead of resource ID
    val duration: Int,
    val author: String,
    val icon: String // Use image URL instead of icon resource ID
) : Parcelable {

    constructor(parcel: Parcel) : this(
        title = parcel.readString() ?: "",
        resourceId  = parcel.readString() ?: "",
        duration = parcel.readInt(),
        author = parcel.readString() ?: "",
        icon = parcel.readString() ?: ""
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(resourceId)
        parcel.writeInt(duration)
        parcel.writeString(author)
        parcel.writeString(icon)
    }

    companion object CREATOR : Parcelable.Creator<RingtoneItem> {
        override fun createFromParcel(parcel: Parcel): RingtoneItem {
            return RingtoneItem(parcel)
        }

        override fun newArray(size: Int): Array<RingtoneItem?> {
            return arrayOfNulls(size)
        }
    }
}