package com.example.researchcenter.features.repository

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R
import com.example.researchcenter.shared.model.Repository
import java.text.SimpleDateFormat
import java.util.Locale

class RepositoryAdapter(
    private val items: List<Repository>,
    private val currentUserId: Long,
    private val onOpen: (Repository) -> Unit,
    private val onEdit: ((Repository) -> Unit)? = null,
    private val onDelete: ((Repository) -> Unit)? = null,
    private val onToggleBookmark: ((Repository) -> Unit)? = null
) : RecyclerView.Adapter<RepositoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_repo_name)
        val tvDesc: TextView = view.findViewById(R.id.tv_repo_desc)
        val tvRole: TextView = view.findViewById(R.id.tv_role_badge)
        val tvDateCreated: TextView = view.findViewById(R.id.tv_date_created)
        val tvMembers: TextView = view.findViewById(R.id.tv_member_count)
        val tvMaterials: TextView = view.findViewById(R.id.tv_material_count)
        val btnBookmark: ImageButton = view.findViewById(R.id.btn_bookmark)
        val btnMenu: ImageButton = view.findViewById(R.id.btn_menu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_repository, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val repo = items[position]
        val isOwner = repo.ownerId == currentUserId

        holder.tvName.text = repo.name
        holder.tvDesc.text = repo.description ?: "No description"
        holder.tvMembers.text = repo.memberCount.toString()
        holder.tvMaterials.text = repo.materialCount.toString()

        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = parser.parse(repo.createdAt.take(19))
            holder.tvDateCreated.text = "Created ${if (date != null) formatter.format(date) else repo.createdAt.take(10)}"
        } catch (e: Exception) {
            holder.tvDateCreated.text = "Created ${repo.createdAt.take(10)}"
        }

        if (repo.role != null) {
            holder.tvRole.visibility = View.VISIBLE
            holder.tvRole.text = when (repo.role.uppercase()) {
                "OWNER" -> "Owner"
                "MEMBER" -> "Member"
                else -> repo.role
            }
            val (bgColor, textColor, borderColor) = if (repo.role == "OWNER") {
                Triple(Color.parseColor("#DCFCE7"), Color.parseColor("#166534"), Color.parseColor("#86EFAC"))
            } else {
                Triple(Color.parseColor("#DBEAFE"), Color.parseColor("#1D4ED8"), Color.parseColor("#93C5FD"))
            }
            holder.tvRole.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 999f
                setColor(bgColor)
                setStroke(2, borderColor)
            }
            holder.tvRole.setTextColor(textColor)
        } else {
            holder.tvRole.visibility = View.GONE
        }

        holder.btnBookmark.setImageResource(
            if (repo.bookmarked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
        )
        holder.btnBookmark.setColorFilter(
            if (repo.bookmarked) Color.parseColor("#F59E0B") else Color.parseColor("#9CA3AF")
        )
        holder.btnBookmark.setOnClickListener { onToggleBookmark?.invoke(repo) }

        holder.itemView.setOnClickListener { onOpen(repo) }

        holder.btnMenu.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            if (isOwner) {
                popup.menu.add(0, 1, 0, "Edit")
                popup.menu.add(0, 2, 1, "Delete")
            } else {
                popup.menu.add(0, 3, 0, if (repo.bookmarked) "Remove bookmark" else "Bookmark")
            }
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> { onEdit?.invoke(repo); true }
                    2 -> { onDelete?.invoke(repo); true }
                    3 -> { onToggleBookmark?.invoke(repo); true }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun getItemCount() = items.size
}
