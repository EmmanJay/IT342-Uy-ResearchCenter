package com.example.researchcenter.features.repository.tabs

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R
import com.example.researchcenter.shared.auth.SessionManager
import com.example.researchcenter.shared.model.Material
import java.text.SimpleDateFormat
import java.util.Locale

class MaterialAdapter(
    private val materials: List<Material>,
    private val isOwner: Boolean,
    private val onToggleBookmark: (Material) -> Unit,
    private val onEdit: (Material) -> Unit,
    private val onDelete: (Material) -> Unit,
    private val onItemClick: (Material) -> Unit
) : RecyclerView.Adapter<MaterialAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_material_title)
        val tvStatus: TextView = view.findViewById(R.id.tv_material_status)
        val tvMeta: TextView = view.findViewById(R.id.tv_material_meta)
        val tvDesc: TextView = view.findViewById(R.id.tv_description)
        val llTags: LinearLayout = view.findViewById(R.id.ll_tags)
        val btnBookmark: ImageButton = view.findViewById(R.id.btn_bookmark)
        val btnMenu: ImageButton = view.findViewById(R.id.btn_menu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_material, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val material = materials[position]
        holder.tvTitle.text = material.title
        
        // Status Badge Style Mapping (Pixel-Faithful CSS mimicking)
        val statusText = material.status
        holder.tvStatus.text = when (statusText.uppercase()) {
            "TO_READ" -> "To Read"
            "IN_PROGRESS" -> "In Progress"
            "COMPLETED" -> "Completed"
            else -> statusText
        }

        val (bgColor, textColor) = when (statusText.uppercase()) {
            "TO_READ" -> Color.parseColor("#F3F4F6") to Color.parseColor("#374151") // Gray pill
            "IN_PROGRESS" -> Color.parseColor("#FEF3C7") to Color.parseColor("#B45309") // Amber pill
            "COMPLETED" -> Color.parseColor("#DCFCE7") to Color.parseColor("#15803D") // Green pill
            else -> Color.parseColor("#F3F4F6") to Color.parseColor("#374151")
        }
        
        holder.tvStatus.setTextColor(textColor)
        holder.tvStatus.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(bgColor)
        }

        holder.tvMeta.text = "${material.materialType} • By ${material.uploaderName}"

        // Description
        if (material.description.isNullOrBlank()) {
            holder.tvDesc.visibility = View.GONE
        } else {
            holder.tvDesc.visibility = View.VISIBLE
            holder.tvDesc.text = material.description
        }

        // Tags Rendering
        holder.llTags.removeAllViews()
        if (material.tags.isNotEmpty()) {
            material.tags.forEach { tag ->
                val tagView = TextView(holder.itemView.context).apply {
                    text = tag
                    textSize = 11f
                    setTextColor(Color.parseColor("#374151"))
                    setPadding(16, 6, 16, 6)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 999f
                        setColor(Color.parseColor("#F3F4F6")) // Neutral Gray 100 bg
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 12, 0)
                    }
                }
                holder.llTags.addView(tagView)
            }
        }

        // Bookmark Toggle styling
        if (material.bookmarked) {
            holder.btnBookmark.setImageResource(android.R.drawable.btn_star_big_on)
            holder.btnBookmark.setColorFilter(Color.parseColor("#16A34A")) // brand primary green
        } else {
            holder.btnBookmark.setImageResource(android.R.drawable.btn_star_big_off)
            holder.btnBookmark.setColorFilter(Color.parseColor("#9CA3AF")) // light gray
        }

        holder.btnBookmark.setOnClickListener {
            onToggleBookmark(material)
        }

        // Edit/Delete Menu
        val context = holder.itemView.context
        val currentUserId = SessionManager.getUserId(context)
        val canManage = isOwner || material.uploaderId == currentUserId

        if (canManage) {
            holder.btnMenu.visibility = View.VISIBLE
            holder.btnMenu.setImageResource(android.R.drawable.ic_menu_more)
            holder.btnMenu.setColorFilter(Color.parseColor("#6B7280"))
            holder.btnMenu.setOnClickListener { view ->
                val popup = PopupMenu(context, view)
                popup.menu.add("Edit")
                popup.menu.add("Delete")
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.title) {
                        "Edit" -> {
                            onEdit(material)
                            true
                        }
                        "Delete" -> {
                            onDelete(material)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        } else {
            holder.btnMenu.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(material)
        }
    }

    override fun getItemCount() = materials.size
}
