package com.example.myapplication

import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlin.math.abs

class Home : Fragment() {

    private lateinit var gestureDetector: GestureDetector
    private var isGestureEnabled: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        gestureDetector = GestureDetector(
            requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onFling(
                    e1: MotionEvent?, e2: MotionEvent, velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    val SWIPE_MIN_DISTANCE = 120
                    val SWIPE_MAX_OFF_PATH = 250
                    val SWIPE_THRESHOLD_VELOCITY = 200
                    try {
                        if (e1 == null) return false
                        if (abs(e1.y - e2.y) > SWIPE_MAX_OFF_PATH) return false
                        if (isGestureEnabled && e1.x - e2.x > SWIPE_MIN_DISTANCE
                            && abs(velocityX) > SWIPE_THRESHOLD_VELOCITY
                        ) {
                            findNavController().navigate(R.id.ringtone) // Swipe Left
                        }
                    } catch (e: Exception) {
                        // Handle exception
                    }
                    return super.onFling(e1, e2, velocityX, velocityY)
                }
            })

        view.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }

        return view
    }

    fun setGestureEnabled(enabled: Boolean) {
        isGestureEnabled = enabled
    }
}
