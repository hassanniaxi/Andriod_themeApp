package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Wallpaper : Fragment() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fragment initialization logic here
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_wallpaper, container, false)

        // Initialize RecyclerView here
        recyclerView = view.findViewById(R.id.recycler_view)

        val gridLayoutManager = GridLayoutManager(context, 2)
        recyclerView.layoutManager = gridLayoutManager

        // Set adapter with titles and images
        val titles = arrayOf("Cars", "Pubg", "Nature","Aesthetics")
        val images = intArrayOf(R.drawable.wall_cover, R.drawable.pubg_cover, R.drawable.nature_cover,R.drawable.animated_cover)
        recyclerView.adapter = Adapter(titles, images)

        return view
    }
}
