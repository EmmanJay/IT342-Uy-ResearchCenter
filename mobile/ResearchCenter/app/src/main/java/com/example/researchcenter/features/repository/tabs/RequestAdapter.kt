package com.example.researchcenter.features.repository.tabs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R
import com.example.researchcenter.shared.model.MaterialRequest
import java.text.SimpleDateFormat
import java.util.Locale

class RequestAdapter(
    private val requests: List<MaterialRequest>,
    private val onItemClick: (MaterialRequest) -> Unit
) : RecyclerView.Adapter<RequestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_request_title)
        val tvStatus: TextView = view.findViewById(R.id.tv_request_status)
        val tvDesc: TextView = view.findViewById(R.id.tv_request_desc)
        val tvRequesterDate: TextView = view.findViewById(R.id.tv_requester_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_request, parent, false)
        return ViewHolder(view)
    }

    private val statusLabels = mapOf(
        "OPEN" to "Open",
        "FULFILLED" to "Fulfilled",
        "PENDING" to "Pending",
        "CANCELLED" to "Cancelled",
        "CLOSED" to "Closed"
    )

    private fun applyStatusBadge(view: TextView, status: String) {
        val normalized = status.uppercase()
        view.text = statusLabels[normalized] ?: normalized

        val (bgColor, textColor) = when (normalized) {
            "OPEN" -> android.graphics.Color.parseColor("#DBEAFE") to android.graphics.Color.parseColor("#1D4ED8")
            "FULFILLED" -> android.graphics.Color.parseColor("#DCFCE7") to android.graphics.Color.parseColor("#166534")
            "PENDING" -> android.graphics.Color.parseColor("#FEF3C7") to android.graphics.Color.parseColor("#B45309")
            "CANCELLED", "CLOSED" -> android.graphics.Color.parseColor("#F3F4F6") to android.graphics.Color.parseColor("#374151")
            else -> android.graphics.Color.parseColor("#F3F4F6") to android.graphics.Color.parseColor("#374151")
        }

        view.setTextColor(textColor)
        view.setPadding(32, 8, 32, 8)
        view.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(bgColor)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        holder.tvTitle.text = request.title
        applyStatusBadge(holder.tvStatus, request.status)
        holder.tvDesc.text = request.description ?: "No description provided."

        var formattedDate = request.createdAt
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            formattedDate = parser.parse(request.createdAt)?.let { formatter.format(it) } ?: request.createdAt
        } catch (e: Exception) {}

        holder.tvRequesterDate.text = "Requested by ${request.requesterName} • $formattedDate"

        holder.itemView.setOnClickListener {
            onItemClick(request)
        }
    }

    override fun getItemCount() = requests.size
}
