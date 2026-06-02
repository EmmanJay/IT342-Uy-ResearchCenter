package com.example.researchcenter.features.bookmarks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R
import com.example.researchcenter.shared.model.Material

class GlobalBookmarkAdapter(
    private val items: List<Material>,
    private val onItemClick: (Material) -> Unit
) : RecyclerView.Adapter<GlobalBookmarkAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvType: TextView = view.findViewById(R.id.tv_type)
        val tvDesc: TextView = view.findViewById(R.id.tv_description)
        val tvRepo: TextView = view.findViewById(R.id.tv_repo_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark_global, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvType.text = item.materialType
        holder.tvDesc.text = item.description ?: "No description"
        holder.tvRepo.text = "Repository #${item.repositoryId}"
        holder.itemView.setOnClickListener { onItemClick(item) }

        // Apply type badge colors
        val ctx = holder.itemView.context
        when (item.materialType.uppercase()) {
            "PDF" -> {
                holder.tvType.setBackgroundColor(ctx.getColor(R.color.type_pdf_bg))
                holder.tvType.setTextColor(ctx.getColor(R.color.type_pdf_text))
            }
            "LINK" -> {
                holder.tvType.setBackgroundColor(ctx.getColor(R.color.type_link_bg))
                holder.tvType.setTextColor(ctx.getColor(R.color.type_link_text))
            }
            "REFERENCE" -> {
                holder.tvType.setBackgroundColor(ctx.getColor(R.color.type_reference_bg))
                holder.tvType.setTextColor(ctx.getColor(R.color.type_reference_text))
            }
        }
    }

    override fun getItemCount() = items.size
}
