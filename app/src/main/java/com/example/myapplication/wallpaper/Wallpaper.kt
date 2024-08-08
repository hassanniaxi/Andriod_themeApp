package com.example.myapplication.wallpaper

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class Wallpaper : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: Adapter
    private val wallpaperList = mutableListOf<Pair<String, Pair<Int, Class<out AppCompatActivity>>>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wallpaper, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Initialize adapter with the list of categories and activities
        adapter = Adapter(requireContext(), wallpaperList)
        recyclerView.adapter = adapter

        // Assign data manually
        assignWallpaperData()

        return view
    }

    private fun assignWallpaperData() {
        wallpaperList.apply {
            clear()
            add(Pair("Animated", Pair(R.drawable.animated_cover, AnimatedActivity::class.java)))
            add(Pair("Pubg", Pair(R.drawable.pubg_cover, PubgActivity::class.java)))
            add(Pair("Cars", Pair(R.drawable.wall_cover, CarsActivity::class.java)))
            add(Pair("Nature", Pair(R.drawable.nature_cover, NatureActivity::class.java)))
        }

        // Notify the adapter that the data has changed
        adapter.notifyDataSetChanged()
    }
}
