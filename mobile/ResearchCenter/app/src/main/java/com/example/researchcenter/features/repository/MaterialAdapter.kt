package com.example.researchcenter.features.repository

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R
import com.example.researchcenter.shared.model.Material

class MaterialAdapter(
    private val items: List<Material>,
    private val currentUserId: Long,
    private val isOwner: Boolean,
    private val onOpen: (Material) -> Unit,
    private val onDelete: (Material) -> Unit,
    private val onUpdateStatus: (Material, String) -> Unit,
    private val onToggleBookmark: ((Material) -> Unit)? = null
) : RecyclerView.Adapter<MaterialAdapter.ViewHolder>() {

    private val statusLabels = mapOf(
        "TO_READ" to "To Read",
        "IN_PROGRESS" to "In Progress",
        "COMPLETED" to "Completed"
    )

    private val statusValues = listOf("TO_READ", "IN_PROGRESS", "COMPLETED")

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_material_title)
        val tvStatus: TextView = view.findViewById(R.id.tv_material_status)
        val tvMeta: TextView = view.findViewById(R.id.tv_material_meta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_material, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val material = items[position]
        holder.tvTitle.text = material.title
        holder.tvStatus.text = statusLabels[material.status] ?: material.status
        holder.tvMeta.text = "${material.materialType} • by ${material.uploaderName}"

        holder.itemView.setOnClickListener { view ->
            try {
                val ctx = view.context as android.app.Activity
                val dlg = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)
                val sheetView = LayoutInflater.from(ctx).inflate(R.layout.bottom_sheet_material_detail, null)
                dlg.setContentView(sheetView)
                
                val tvTitle = sheetView.findViewById<TextView>(R.id.tvMatTitle)
                val tvMeta = sheetView.findViewById<TextView>(R.id.tvMatMeta)
                val tvDesc = sheetView.findViewById<TextView>(R.id.tvMatDesc)
                val tvStatusChip = sheetView.findViewById<TextView>(R.id.tvMatStatusChip)
                val actPersonalStatus = sheetView.findViewById<android.widget.AutoCompleteTextView>(R.id.actMatPersonalStatus)
                val tvAuthors = sheetView.findViewById<TextView>(R.id.tvMatAuthors)
                val tvPublisher = sheetView.findViewById<TextView>(R.id.tvMatPublisher)
                val tvYearIsbn = sheetView.findViewById<TextView>(R.id.tvMatYearIsbn)
                val llTags = sheetView.findViewById<LinearLayout>(R.id.llMatTags)
                val btnDelete = sheetView.findViewById<Button>(R.id.btnMatDelete)
                val btnOpen = sheetView.findViewById<Button>(R.id.btnMatOpenLink)
                val btnBookmark = sheetView.findViewById<Button>(R.id.btnMatBookmark)

                tvTitle.text = material.title
                tvMeta.text = "${material.materialType} · ${statusLabels[material.status] ?: material.status} by ${material.uploaderName}"
                tvDesc.text = material.description?.takeIf { it.isNotBlank() } ?: "No description provided."

                val currentPersonalStatus = material.myStatus ?: "TO_READ"
                applyStatusChip(tvStatusChip, currentPersonalStatus)

                val statusAdapter = ArrayAdapter(ctx, android.R.layout.simple_list_item_1, statusValues.map { statusLabels[it] ?: it })
                actPersonalStatus.setAdapter(statusAdapter)
                actPersonalStatus.setText(statusLabels[currentPersonalStatus] ?: "To Read", false)
                actPersonalStatus.setOnItemClickListener { _, _, position, _ ->
                    val selectedStatus = statusValues[position]
                    applyStatusChip(tvStatusChip, selectedStatus)
                    onUpdateStatus(material, selectedStatus)
                }

                if (material.materialType.equals("REFERENCE", ignoreCase = true)) {
                    material.authors?.takeIf { it.isNotBlank() }?.let {
                        tvAuthors.visibility = View.VISIBLE
                        tvAuthors.text = "Authors: $it"
                    }
                    material.publisher?.takeIf { it.isNotBlank() }?.let {
                        tvPublisher.visibility = View.VISIBLE
                        tvPublisher.text = "Publisher: $it"
                    }
                    val yearOrIsbn = buildList {
                        material.year?.takeIf { it.isNotBlank() }?.let { add("Year: $it") }
                        material.isbn?.takeIf { it.isNotBlank() }?.let { add("ISBN: $it") }
                    }.joinToString("   ")
                    if (yearOrIsbn.isNotBlank()) {
                        tvYearIsbn.visibility = View.VISIBLE
                        tvYearIsbn.text = yearOrIsbn
                    }
                }

                if (material.tags.isNotEmpty()) {
                    llTags.visibility = View.VISIBLE
                    llTags.removeAllViews()
                    material.tags.forEach { tag ->
                        val chip = TextView(ctx).apply {
                            text = tag
                            setTextColor(Color.parseColor("#424242"))
                            textSize = 11f
                            setPadding(dp(ctx, 10), dp(ctx, 4), dp(ctx, 10), dp(ctx, 4))
                            background = roundedChip(Color.parseColor("#E0E0E0"))
                            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                marginEnd = dp(ctx, 6)
                                topMargin = dp(ctx, 6)
                            }
                        }
                        llTags.addView(chip)
                    }
                }

                if (onToggleBookmark != null) {
                    btnBookmark.visibility = View.VISIBLE
                    btnBookmark.text = if (material.bookmarked) "Unbookmark" else "Bookmark"
                    btnBookmark.setOnClickListener {
                        dlg.dismiss()
                        onToggleBookmark.invoke(material)
                    }
                }

                if (material.uploaderId == currentUserId || isOwner) {
                    btnDelete.visibility = View.VISIBLE
                    btnDelete.setOnClickListener {
                        dlg.dismiss()
                        onDelete(material)
                    }
                }

                val canOpen = when {
                    material.materialType.equals("REFERENCE", ignoreCase = true) -> false
                    material.materialType.equals("PDF", ignoreCase = true) -> !material.fileUrl.isNullOrBlank()
                    else -> !material.url.isNullOrBlank() || !material.fileUrl.isNullOrBlank()
                }
                if (canOpen) {
                    btnOpen.visibility = View.VISIBLE
                    btnOpen.setOnClickListener {
                        dlg.dismiss()
                        onOpen(material)
                    }
                } else {
                    btnOpen.visibility = View.GONE
                }
                
                dlg.show()
            } catch (e: Exception) {
                onOpen(material)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (material.uploaderId == currentUserId || isOwner) {
                onDelete(material)
                true
            } else false
        }
    }

    override fun getItemCount() = items.size

    private fun applyStatusChip(view: TextView, status: String) {
        val normalized = status.uppercase()
        val color = when (normalized) {
            "IN_PROGRESS" -> Color.parseColor("#F59E0B")
            "COMPLETED" -> Color.parseColor("#16A34A")
            else -> Color.parseColor("#9E9E9E")
        }
        view.text = statusLabels[normalized] ?: normalized
        view.setTextColor(Color.WHITE)
        view.background = roundedChip(color)
    }

    private fun roundedChip(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(color)
        }
    }

    private fun dp(context: android.content.Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
