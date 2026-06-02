package com.example.researchcenter.features.repository.tabs

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R
import com.example.researchcenter.shared.model.RepositoryMember
import com.example.researchcenter.shared.ui.UserAvatarView

class MemberAdapter(
    private val members: List<RepositoryMember>
) : RecyclerView.Adapter<MemberAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarView: UserAvatarView = view.findViewById(R.id.avatar_view)
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvEmail: TextView = view.findViewById(R.id.tv_email)
        val tvRole: TextView = view.findViewById(R.id.tv_role)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val member = members[position]
        holder.tvName.text = member.name
        holder.tvEmail.text = member.email
        
        holder.tvRole.text = when (member.role.uppercase()) {
            "OWNER" -> "Owner"
            "MEMBER" -> "Member"
            else -> member.role
        }

        val bgColor: Int
        val textColor: Int
        val borderColor: Int
        if (member.role.uppercase() == "OWNER") {
            bgColor = Color.parseColor("#DCFCE7")
            textColor = Color.parseColor("#166534")
            borderColor = Color.parseColor("#86EFAC")
        } else {
            bgColor = Color.parseColor("#DBEAFE")
            textColor = Color.parseColor("#1D4ED8")
            borderColor = Color.parseColor("#93C5FD")
        }

        holder.tvRole.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(bgColor)
            setStroke(2, borderColor)
        }
        holder.tvRole.setTextColor(textColor)

        holder.avatarView.setUser(member.name, member.email, member.profilePicture)
    }

    override fun getItemCount() = members.size
}
