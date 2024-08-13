package com.example.myapplication

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class RingtoneItem(
    val title: String,
    val resourceId: Int,
    val duration: Int,
    val author: String,
    val icon: Int // Add this line to include the icon resource ID
) : Parcelable {

    constructor(parcel: Parcel) : this(
        title = parcel.readString() ?: "",
        resourceId = parcel.readInt(),
        duration = parcel.readInt(),
        author = parcel.readString() ?: "",
        icon = parcel.readInt() // Read the icon resource ID from the parcel
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeInt(resourceId)
        parcel.writeInt(duration)
        parcel.writeString(author)
        parcel.writeInt(icon) // Write the icon resource ID to the parcel
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

class RingtoneRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val ringtoneCollection = firestore.collection("ringtones")

    suspend fun fetchRingtones(): List<RingtoneItem> {
        return try {
            val snapshot = ringtoneCollection.get().await()
            snapshot.documents.mapNotNull { document ->
                val title = document.getString("title") ?: return@mapNotNull null
                val resourceId = document.getLong("resourceId")?.toInt() ?: return@mapNotNull null
                val duration = document.getLong("duration")?.toInt() ?: return@mapNotNull null
                val author = document.getString("author") ?: return@mapNotNull null
                val icon = document.getLong("icon")?.toInt() ?: return@mapNotNull null

                RingtoneItem(title, resourceId, duration, author, icon)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

