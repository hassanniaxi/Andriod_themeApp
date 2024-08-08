package com.example.myapplication

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Ringtone : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RingtoneAdapter
    private val ringtoneList = mutableListOf<RingtoneItem>()
    private lateinit var notFoundTextView: TextView
    private lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ringtone, container, false)

        recyclerView = view.findViewById(R.id.ringtone_recycler_view)
        notFoundTextView = view.findViewById(R.id.not_found_text_view)

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = RingtoneAdapter(ringtoneList, requireContext(), requireMainActivity())
        recyclerView.adapter = adapter

        loadRingtones()

        // Check if ringtoneList is empty to show the message
        updateNotFoundMessage()

        // Access the SearchView from the MainActivity
        requireMainActivity().let { mainActivity ->
            searchView = mainActivity.getSearchView()
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    adapter.filter.filter(newText)
                    updateNotFoundMessage()
                    return true
                }
            })
        }

        return view
    }

    private fun loadRingtones() {
        // Add your ringtones here with durations
        ringtoneList.add(RingtoneItem("Mafia Ringtone", R.raw.mafia_ringtone, getRingtoneDuration(R.raw.mafia_ringtone)))
        ringtoneList.add(RingtoneItem("Iphone 14 pro", R.raw.ringtone_iphone_14_pro, getRingtoneDuration(R.raw.ringtone_iphone_14_pro)))

        adapter.notifyDataSetChanged()
    }

    private fun getRingtoneDuration(resourceId: Int): Int {
        // You can use MediaPlayer to get the duration of each ringtone
        val mediaPlayer = MediaPlayer.create(requireContext(), resourceId)
        val duration = mediaPlayer.duration
        mediaPlayer.release()
        return duration
    }

    private fun updateNotFoundMessage() {
        notFoundTextView.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    private fun requireMainActivity(): MainActivity {
        return requireActivity() as MainActivity
    }
}
