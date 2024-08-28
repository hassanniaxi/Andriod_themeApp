package com.example.myapplication.ringtone

import android.os.Parcel
import android.os.Parcelable
import java.util.Date

class RingtoneItem(
    val title: String,
    val resourceId: String,
    val author: String,
    val category: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(resourceId)
        parcel.writeString(author)
        parcel.writeString(category)
    }

    override fun describeContents(): Int {
        return 0
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
