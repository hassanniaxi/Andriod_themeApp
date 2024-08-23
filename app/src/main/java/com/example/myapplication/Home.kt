package com.example.myapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.myapplication.ringtone.NavigationHandler
import com.example.myapplication.ringtone.Ringtone
import com.example.myapplication.ringtone.Ringtone.Companion
import kotlin.math.abs

class Home : Fragment(),  GestureDetector.OnGestureListener{

    private lateinit var gestureDetector: GestureDetector
    var x1:Float = 0.0f
    var x2:Float = 0.0f
    var y1:Float = 0.0f
    var y2:Float = 0.0f
    private lateinit var navController: NavController

    companion object{
        const val MINI_DISTANCE = 150
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        navController = findNavController()
        this.gestureDetector = GestureDetector(requireContext(), this)

        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    x1 = event.x
                    y1 = event.y
                }
                MotionEvent.ACTION_UP -> {
                    x2 = event.x
                    y2 = event.y
                    val valueX: Float = x2 - x1
                    if (abs(valueX) > MINI_DISTANCE) {
                        if (x2 > x1) {
                        } else {
                            NavigationHandler.navigateToDestination(navController, R.id.ringtone)
                        }
                    }
                }
            }
            true
        }

        return view
    }

    override fun onDown(e: MotionEvent): Boolean {
        x1 = e.x
        y1 = e.y
        return false
    }

    override fun onShowPress(p0: MotionEvent) {
    }

    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        return false
    }

    override fun onLongPress(p0: MotionEvent) {
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        return false
    }
}
