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
    private val requests: List<MaterialRequest>
) : RecyclerView.Adapter<RequestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_request_title)
        val tvStatus: TextView = view.findViewById(R.id.tv_request_status)
        val tvDesc: TextView = view.findViewById(R.id.tv_request_desc)
        val tvRequester: TextView = view.findViewById(R.id.tv_requester)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        holder.tvTitle.text = request.title
        holder.tvStatus.text = request.status
        holder.tvDesc.text = request.description ?: "No description provided."
        holder.tvRequester.text = "Requested by ${request.requesterName}"

        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            holder.tvDate.text = parser.parse(request.createdAt)?.let { formatter.format(it) } ?: request.createdAt
        } catch (e: Exception) {
            holder.tvDate.text = request.createdAt
        }
    }

    override fun getItemCount() = requests.size
}
