package com.example.researchcenter.features.repository.tabs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R
import com.example.researchcenter.shared.model.RepositoryUpdate
import com.example.researchcenter.shared.ui.UserAvatarView
import java.text.SimpleDateFormat
import java.util.Locale

class UpdateAdapter(
    private val updates: List<RepositoryUpdate>
) : RecyclerView.Adapter<UpdateAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarView: UserAvatarView = view.findViewById(R.id.avatar_view)
        val tvAuthor: TextView = view.findViewById(R.id.tv_author)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvContent: TextView = view.findViewById(R.id.tv_content)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_update, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val update = updates[position]
        holder.tvAuthor.text = update.authorName
        holder.tvContent.text = update.content
        holder.avatarView.setUser(update.authorName, "")

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
