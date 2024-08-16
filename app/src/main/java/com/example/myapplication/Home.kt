package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide

class Home : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var testing: ImageView
    private lateinit var userNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firestore
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the TextView and ImageView
        testing = view.findViewById(R.id.testing)
        userNameTextView = view.findViewById(R.id.firebasetest) // Make sure this ID matches your layout

        // Example of accessing a document in a collection and updating the ImageView
        db.collection("test").document("data").get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val imageUrl = document.getString("music") // Assuming "image" is a URL
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(imageUrl)
                            .into(testing)
                    } else {
                        userNameTextView.text = "Image URL Not Found"
                    }
                } else {
                    userNameTextView.text = "User Document Not Found"
                }
            }
            .addOnFailureListener { exception ->
                userNameTextView.text = "Error: ${exception.message}"
            }
    }

    companion object {
        // You can add static members or factory methods here if needed
    }
}
