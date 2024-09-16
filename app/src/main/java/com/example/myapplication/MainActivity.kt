package com.example.myapplication

import android.content.ContentValues.TAG
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMainBinding
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.ringtones.setOnClickListener {
            startFragmentManagementActivity(R.id.ringtones)
        }
        binding.iconChanger.setOnClickListener {
            startFragmentManagementActivity(R.id.icon_changer)
        }
        binding.allWallpapers.setOnClickListener {
            startFragmentManagementActivity(R.id.wallpapers)
        }
    }

    private fun startFragmentManagementActivity(navId: Int) {
        val intent = Intent(this, FragmentManagement::class.java).apply {
            putExtra(FragmentManagement.NAV_ID, navId)
        }
        startActivity(intent)
    }

}