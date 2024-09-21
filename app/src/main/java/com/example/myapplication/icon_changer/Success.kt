package com.example.myapplication.icon_changer

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.FragmentManagement
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityMakeManipulationBinding
import com.example.myapplication.databinding.ActivitySuccessBinding

class Success : AppCompatActivity() {
    private lateinit var binding: ActivitySuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.goHome.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        binding.makeMore.setOnClickListener{
            val intent = Intent(this, FragmentManagement::class.java).apply {
                putExtra(FragmentManagement.NAV_ID, R.id.icon_changer)
            }
            startActivity(intent)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, FragmentManagement::class.java).apply {
            putExtra(FragmentManagement.NAV_ID, R.id.icon_changer)
        }
        startActivity(intent)
    }

}