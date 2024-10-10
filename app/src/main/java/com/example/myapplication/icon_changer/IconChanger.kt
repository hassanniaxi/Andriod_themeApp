package com.example.myapplication.icon_changer

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapplication.NavigationHandler
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentIconChangerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class IconChanger : Fragment(), GestureDetector.OnGestureListener {

    private lateinit var binding: FragmentIconChangerBinding
    private lateinit var appAdapter: AppAdapter
    private var appList: List<AppInfo> = listOf()
    private var filteredAppList: List<AppInfo> = listOf()

    private lateinit var navController: NavController
    private lateinit var gestureDetector: GestureDetector
    private var x1: Float = 0.0f
    private var x2: Float = 0.0f
    private var y1: Float = 0.0f
    private var y2: Float = 0.0f

    companion object {
        const val MINI_DISTANCE = 50
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIconChangerBinding.inflate(layoutInflater)
        val view = binding.root

        binding.appSearchView.clearFocus()
        gestureDetector = GestureDetector(requireContext(), this)
        navController = findNavController()

        binding.spinner.visibility = View.VISIBLE

        binding.appSearchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                binding.appSearchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)?.visibility = View.GONE
                filterApps(newText)
                return true
            }
        })

        binding.recyclerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                handleSwipeGesture(event)
            }
            false
        }

        toTransfer()

        binding.appSearchView.setOnCloseListener {
            binding.appSearchView.clearFocus()
            true
        }

        loadApps()
        return view
    }

    private fun toTransfer() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (navController.currentDestination?.id == R.id.icon_changer) {
                        requireActivity().finish()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            }
        )
    }

    private fun loadApps() {
        CoroutineScope(Dispatchers.IO).launch {
            val packageManager = requireContext().packageManager

            val queryIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = packageManager.queryIntentActivities(queryIntent, PackageManager.MATCH_ALL)

            appList = resolveInfos.map { resolveInfo ->
                AppInfo(
                    appName = resolveInfo.loadLabel(packageManager).toString(),
                    appIcon = resolveInfo.loadIcon(packageManager),
                    packageName = resolveInfo.activityInfo.packageName
                )
            }

            filteredAppList = appList

            withContext(Dispatchers.Main) {
                appAdapter = AppAdapter(filteredAppList, requireContext(), navController)
                binding.recyclerView.apply {
                    layoutManager = GridLayoutManager(requireContext(), 4)
                    adapter = appAdapter
                }

                binding.spinner.visibility = View.GONE
                updateNoAppsFoundMessage()
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
        updateNoAppsFoundMessage()
    }

    private fun updateNoAppsFoundMessage() {
        if (filteredAppList.isEmpty()) {
            binding.noAppsFoundText.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.noAppsFoundText.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun handleSwipeGesture(event: MotionEvent) {
        if (x1 == 0f && y1 == 0f) return

        x2 = event.x
        y2 = event.y
        val deltaX = x2 - x1
        val deltaY = y2 - y1
        if (abs(deltaX) > MINI_DISTANCE && abs(deltaY) < MINI_DISTANCE) {
            if (deltaX > 0) {
                navController.let { NavigationHandler.navigateToDestination(it, R.id.wallpapers) }
            } else {
                navController.let { NavigationHandler.navigateToDestination(it, R.id.ringtones) }
            }
        }

        x1 = 0f
        y1 = 0f
    }

    override fun onDown(e: MotionEvent): Boolean {
        x1 = e.x
        y1 = e.y
        return false
    }

    override fun onShowPress(p0: MotionEvent) {}

    override fun onSingleTapUp(p0: MotionEvent): Boolean = false

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean = false

    override fun onLongPress(p0: MotionEvent) {}

    override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean = false
}
