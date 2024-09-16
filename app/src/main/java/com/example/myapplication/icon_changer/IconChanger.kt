package com.example.myapplication.icon_changer

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentIconChangerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IconChanger : Fragment() {

    private lateinit var binding: FragmentIconChangerBinding
    private lateinit var appAdapter: AppAdapter
    private var appList: List<AppInfo> = listOf()
    private var filteredAppList: List<AppInfo> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIconChangerBinding.inflate(layoutInflater)
        val view = binding.root

        binding.spinner.visibility = View.VISIBLE

        binding.appSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterApps(newText)
                return true
            }
        })

        loadApps()
        return view
    }

    private fun loadApps() {
        CoroutineScope(Dispatchers.IO).launch {
            val packageManager = requireContext().packageManager
            val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            val visibleApps = apps.filter {
                packageManager.getLaunchIntentForPackage(it.packageName) != null && it.enabled
            }

            appList = visibleApps.map {
                AppInfo(
                    appName = packageManager.getApplicationLabel(it).toString(),
                    appIcon = packageManager.getApplicationIcon(it),
                    packageName = it.packageName
                )
            }

            filteredAppList = appList

            withContext(Dispatchers.Main) {
                appAdapter = AppAdapter(filteredAppList, requireContext())
                binding.recyclerView.apply {
                    layoutManager = GridLayoutManager(requireContext(), 4)
                    adapter = appAdapter
                }

                binding.spinner.visibility = View.GONE
            }
        }
    }

    private fun filterApps(query: String?) {
        filteredAppList = if (query.isNullOrEmpty()) {
            appList
        } else {
            appList.filter {
                it.appName.contains(query, ignoreCase = true)
            }
        }
        appAdapter.updateList(filteredAppList)
    }
}