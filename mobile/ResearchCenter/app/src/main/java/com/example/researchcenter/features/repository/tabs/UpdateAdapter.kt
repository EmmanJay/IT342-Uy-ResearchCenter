package com.example.researchcenter.features.repository.tabs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R
import com.example.researchcenter.shared.model.RepositoryUpdate
import com.example.researchcenter.shared.ui.UserAvatarView
import android.graphics.Color
import java.text.SimpleDateFormat
import java.util.Locale

class UpdateAdapter(
    private val updates: List<RepositoryUpdate>,
    private val isOwner: Boolean,
    private val currentUserId: Long,
    private val onEdit: (RepositoryUpdate) -> Unit,
    private val onDelete: (RepositoryUpdate) -> Unit
) : RecyclerView.Adapter<UpdateAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarView: UserAvatarView = view.findViewById(R.id.avatar_view)
        val tvAuthor: TextView = view.findViewById(R.id.tv_author)
        val tvRole: TextView = view.findViewById(R.id.tv_role)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvContent: TextView = view.findViewById(R.id.tv_content)
        val llActions: View = view.findViewById(R.id.ll_actions)
        val btnEdit: View = view.findViewById(R.id.btn_edit)
        val btnDelete: View = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_update, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val update = updates[position]
        holder.tvAuthor.text = update.authorName
        holder.tvContent.text = update.content
        holder.avatarView.setUser(update.authorName, null, update.authorProfilePicture)

        // Bind Role Badge
        val role = update.authorRole ?: "MEMBER"
        holder.tvRole.text = role
        holder.tvRole.visibility = View.VISIBLE
        if (role == "OWNER") {
            holder.tvRole.setBackgroundResource(R.drawable.badge_bg)
            holder.tvRole.setTextColor(Color.WHITE)
        } else {
            holder.tvRole.setBackgroundResource(R.drawable.chip_inactive_bg)
            holder.tvRole.setTextColor(Color.parseColor("#4B5563"))
        }

        // Show actions only for authors or repository owners
        val context = holder.itemView.context
        val userRole = com.example.researchcenter.shared.auth.SessionManager.getRole(context)
        val isAdmin = userRole == "ADMIN"
        val isAuthor = update.authorId == currentUserId

        val canDelete = isAdmin || isAuthor
        val canEdit = isAuthor

        holder.btnDelete.visibility = if (canDelete) View.VISIBLE else View.GONE
        holder.btnEdit.visibility = if (canEdit) View.VISIBLE else View.GONE
        holder.llActions.visibility = if (canDelete || canEdit) View.VISIBLE else View.GONE
        
        holder.btnEdit.setOnClickListener { onEdit(update) }
        holder.btnDelete.setOnClickListener { onDelete(update) }

        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
            holder.tvDate.text = parser.parse(update.createdAt)?.let { formatter.format(it) } ?: update.createdAt
        } catch (e: Exception) {
            holder.tvDate.text = update.createdAt
        }
    }

    override fun getItemCount() = updates.size
}
