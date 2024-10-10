package com.example.myapplication.icon_changer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.pm.ShortcutManagerCompat
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityMakeManipulationBinding
import com.example.myapplication.ringtone.RingtoneDetailActivity

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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(context, "Shortcut creation is not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pm: PackageManager = context.packageManager
            val intent = pm.getLaunchIntentForPackage(pkgName)

            if (intent == null) {
                Toast.makeText(context, "Error: Launch intent for package not found", Toast.LENGTH_SHORT).show()
                return
            }

            val targetAppContext = context.createPackageContext(pkgName, Context.CONTEXT_IGNORE_SECURITY)
            val targetAppResources = targetAppContext.resources

            val targetAppIcon = try {
                val appInfo = pm.getApplicationInfo(pkgName, 0)
                pm.getApplicationIcon(appInfo)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }

            val shortcutIntent = Intent(Intent.ACTION_CREATE_SHORTCUT).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = intent.component
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }

            val shortcutBuilder = ShortcutInfoCompat.Builder(context, "${pkgName}_${System.currentTimeMillis()}")
                .setShortLabel(newName)
                .setLongLabel(newName)
                .setIntent(shortcutIntent)

            val icon = when {
                iconResourceId != null -> IconCompat.createWithResource(context, iconResourceId)
                targetAppIcon != null -> IconCompat.createWithBitmap((targetAppIcon as BitmapDrawable).bitmap)
                else -> null
            }
            icon?.let { shortcutBuilder.setIcon(it) }

            val shortcut = shortcutBuilder.build()

            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            if (shortcutManager?.isRequestPinShortcutSupported == true) {
                ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)

                val existingShortcuts = ShortcutManagerCompat.getDynamicShortcuts(context)
                val updatedShortcuts = existingShortcuts.toMutableList()
                updatedShortcuts.add(shortcut)
                ShortcutManagerCompat.setDynamicShortcuts(context, updatedShortcuts)
            } else {
                Toast.makeText(context, "Pinning shortcuts is not supported on this device", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: Please make changes", Toast.LENGTH_SHORT).show()
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