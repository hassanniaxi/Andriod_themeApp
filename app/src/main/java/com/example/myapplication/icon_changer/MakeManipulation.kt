package com.example.myapplication.icon_changer

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapplication.databinding.ActivityMakeManipulationBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MakeManipulation : AppCompatActivity() {

    private lateinit var binding: ActivityMakeManipulationBinding
    private val iconList = mutableListOf<IconDetail>()
    private lateinit var viewModel: IconsViewModel
    private var pkgName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMakeManipulationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(IconsViewModel::class.java)

        val appPos = intent.getIntExtra(APP_POS, -1)
        val appLogo = intent.getParcelableExtra<Bitmap>(APP_LOGO)
        val list = intent.getParcelableArrayListExtra<AppInfo>(APP_INFO_LIST) ?: arrayListOf()

        if (appPos in list.indices) {
            val appInfo = list[appPos]
            val appTitle = appInfo.appName

            binding.headerTitle.text = appTitle
            binding.editAppTitle.setText(appTitle)
            binding.appLogo.setImageBitmap(appLogo)
            pkgName = appInfo.packageName
            binding.backToApps.setOnClickListener { finish() }
        } else {
            finish()
        }

        binding.iconRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MakeManipulation, 5)
            adapter = AppDetailAdapter(
                iconList,
                binding,
                appLogo,
                pkgName,
                this@MakeManipulation
            )
        }

        if (iconList.isEmpty()) {
            loadIcons()
        }

        viewModel.icons.observe(this) { icons ->
            iconList.clear()
            iconList.addAll(icons)
            (binding.iconRecyclerView.adapter as? AppDetailAdapter)?.notifyDataSetChanged()
        }
    }


    private fun loadIcons() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val iconDetails = listOf(
                    IconDetail("icon_1"),
                    IconDetail("icon_2"),
                    IconDetail("icon_3"),
                    IconDetail("icon_4"),
                    IconDetail("icon_5"),
                    IconDetail("icon_6"),
                    IconDetail("icon_7"),
                    IconDetail("icon_8"),
                    IconDetail("icon_9"),
                    IconDetail("icon_10"),
                    IconDetail("icon_11"),
                )

                val icons = iconDetails.mapNotNull { icon ->
                    val resourceId = resources.getIdentifier(icon.resourceId, "drawable", packageName)
                    if (resourceId != 0) IconDetail(resourceId.toString()) else null
                }

                withContext(Dispatchers.Main) {
                    viewModel.setIcons(icons)
                }
            } catch (e: Exception) {
                Log.e("MakeManuplate", "Error loading icons", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MakeManipulation, "Error loading icons", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        const val APP_POS = "pos"
        const val APP_LOGO = "logo"
        const val APP_INFO_LIST = "extra_list"
    }
}