package com.example.myapplication.icon_changer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.pm.ShortcutManagerCompat
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityMakeManipulationBinding

class AppDetailAdapter(
    private val iconList: List<IconDetail>,
    private val activityMakeManipulationBinding: ActivityMakeManipulationBinding,
    private val mainLogo: Bitmap?,
    private val pkgName: String,
    private val context: Context
) : RecyclerView.Adapter<AppDetailAdapter.AppViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconImageView: ImageView = view.findViewById(R.id.icons)
        val tickMark: ImageView = view.findViewById(R.id.tick_mark)

        init {
            itemView.setOnClickListener {
                val previouslySelectedPosition = selectedPosition
                val clickedPosition = adapterPosition

                selectedPosition = if (clickedPosition == selectedPosition) {
                    RecyclerView.NO_POSITION
                } else {
                    clickedPosition
                }

                if (selectedPosition != RecyclerView.NO_POSITION) {
                    val iconDetail = iconList[selectedPosition]
                    val resourceId = context.resources.getIdentifier(iconDetail.resourceId, "drawable", context.packageName)
                    if (resourceId != 0) {
                        activityMakeManipulationBinding.appLogo.setImageResource(resourceId)
                    }
                } else {
                    activityMakeManipulationBinding.appLogo.setImageBitmap(mainLogo)
                }

                notifyItemChanged(previouslySelectedPosition)
                notifyItemChanged(selectedPosition)
            }

            activityMakeManipulationBinding.save.setOnClickListener {
                applyChanges()
            }
        }
    }

    private fun applyChanges() {
        val newName = activityMakeManipulationBinding.editAppTitle.text.toString().trim()
        if (newName.isEmpty()) {
            Toast.makeText(context, "Please enter a new name", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPosition != RecyclerView.NO_POSITION) {
            val iconDetail = iconList[selectedPosition]
            val resourceId = context.resources.getIdentifier(iconDetail.resourceId, "drawable", context.packageName)
            if (resourceId != 0) {
                createShortcutForOtherApp(resourceId, newName)
            } else {
                Toast.makeText(context, "Error: Invalid icon resource", Toast.LENGTH_SHORT).show()
            }
        } else {
            createShortcutForOtherApp(null, newName)
        }
    }

    private fun createShortcutForOtherApp(iconResourceId: Int?, newName: String) {
        try {
            val pm: PackageManager = context.packageManager
            val shortcutManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.getSystemService(Context.SHORTCUT_SERVICE) as android.content.pm.ShortcutManager
            } else {
                null
            }

            val intent = pm.getLaunchIntentForPackage(pkgName)
            if (intent != null) {
                val shortcutIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    component = intent.component
                }

                val shortcutBuilder = ShortcutInfoCompat.Builder(context, pkgName)
                    .setShortLabel(newName)
                    .setLongLabel(newName)
                    .setIntent(shortcutIntent)

                iconResourceId?.let {
                    val icon = IconCompat.createWithResource(context, it)
                    shortcutBuilder.setIcon(icon)
                } ?: run {
                    mainLogo?.let {
                        val icon = IconCompat.createWithBitmap(it)
                        shortcutBuilder.setIcon(icon)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && shortcutManager != null) {
                    val shortcut = shortcutBuilder.build()
                    val success = ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)

                    if (success) {
                        Toast.makeText(context, "Shortcut created successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Shortcut creation failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    ShortcutManagerCompat.setDynamicShortcuts(context, listOf(shortcutBuilder.build()))
                    Toast.makeText(context, "Shortcut added to launcher", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Error: Launch intent for package not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: Unable to create shortcut", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.icons_item, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val iconDetail = iconList[position]
        val resourceId = context.resources.getIdentifier(iconDetail.resourceId, "drawable", context.packageName)

        if (resourceId != 0) {
            holder.iconImageView.setImageResource(resourceId)
        }

        holder.tickMark.visibility = if (position == selectedPosition) VISIBLE else GONE
    }

    override fun getItemCount(): Int = iconList.size
}