package com.example.researchcenter.features.repository.tabs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R
import com.example.researchcenter.shared.model.Material
import java.text.SimpleDateFormat
import java.util.Locale

class MaterialAdapter(
    private val materials: List<Material>,
    private val onToggleBookmark: (Material) -> Unit
) : RecyclerView.Adapter<MaterialAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_material_title)
        val tvStatus: TextView = view.findViewById(R.id.tv_material_status)
        val tvMeta: TextView = view.findViewById(R.id.tv_material_meta)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val btnBookmark: ImageButton = view.findViewById(R.id.btn_bookmark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_material, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val material = materials[position]
        holder.tvTitle.text = material.title
        holder.tvStatus.text = material.status
        holder.tvMeta.text = "${material.materialType} • By ${material.uploaderName}"

        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            holder.tvDate.text = parser.parse(material.createdAt)?.let { formatter.format(it) } ?: material.createdAt
        } catch (e: Exception) {
            holder.tvDate.text = material.createdAt
        }

        if (material.bookmarked) {
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark)
        } else {
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_border)
        }

        holder.btnBookmark.setOnClickListener {
            onToggleBookmark(material)
        }
    }

    override fun getItemCount() = materials.size
}
