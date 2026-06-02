package com.example.researchcenter.features.profile

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R
import com.example.researchcenter.shared.model.ActivityLog
import com.example.researchcenter.shared.ui.UserAvatarView
import java.text.SimpleDateFormat
import java.util.Locale

class ActivityLogAdapter(private val logs: List<ActivityLog>) : RecyclerView.Adapter<ActivityLogAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarView: UserAvatarView = view.findViewById(R.id.avatar_view)
        val tvActorName: TextView = view.findViewById(R.id.tv_actor_name)
        val tvAction: TextView = view.findViewById(R.id.tv_action)
        val tvDetails: TextView = view.findViewById(R.id.tv_details)
        val tvTime: TextView = view.findViewById(R.id.tv_time)
        val tvBadge: TextView = view.findViewById(R.id.tv_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activity_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs[position]

        // Actor name
        val context = holder.itemView.context
        val currentUserId = com.example.researchcenter.shared.auth.SessionManager.getUserId(context)
        val actorName = if (log.userId == currentUserId) "You" else (log.actorName ?: "Unknown User")
        holder.tvActorName.text = actorName
        holder.avatarView.setUser(if (actorName == "You") (log.actorName ?: "You") else actorName, null, log.actorProfilePicture)

        // Action description
        val actionText = buildActionText(log)
        holder.tvAction.text = actionText

        // Details / description
        val detailText = log.description ?: log.details
        if (detailText.isNotBlank()) {
            holder.tvDetails.visibility = View.VISIBLE
            holder.tvDetails.text = detailText
        } else {
            holder.tvDetails.visibility = View.GONE
        }

        // Repository badge
        val repoName = log.repositoryName
        if (!repoName.isNullOrBlank()) {
            holder.tvBadge.visibility = View.VISIBLE
            holder.tvBadge.text = repoName
        } else {
            holder.tvBadge.visibility = View.GONE
        }

        // Timestamp
        val timeStr = log.createdAt ?: log.timestamp
        if (timeStr.isNotBlank()) {
            try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val formatter = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
                holder.tvTime.text = parser.parse(timeStr.take(19))?.let { formatter.format(it) } ?: timeStr
            } catch (e: Exception) {
                holder.tvTime.text = timeStr.take(16)
            }
        } else {
            holder.tvTime.text = ""
        }
    }

    private fun buildActionText(log: ActivityLog): String {
        val action = log.action
        val targetName = log.targetName
        return when {
            !targetName.isNullOrBlank() -> "$action: $targetName"
            else -> action
        }
    }

    override fun getItemCount() = logs.size
}
