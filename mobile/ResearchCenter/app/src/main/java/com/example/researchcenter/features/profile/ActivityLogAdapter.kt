package com.example.researchcenter.features.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R
import com.example.researchcenter.shared.model.ActivityLog

class ActivityLogAdapter(private val logs: List<ActivityLog>) : RecyclerView.Adapter<ActivityLogAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAction: TextView = view.findViewById(android.R.id.text1)
        val tvDetails: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs[position]
        holder.tvAction.text = log.action
        holder.tvDetails.text = log.details
    }

    override fun getItemCount() = logs.size
}
