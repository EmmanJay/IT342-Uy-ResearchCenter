package com.example.researchcenter.features.repository

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R
import com.example.researchcenter.shared.model.MaterialRequest

class RequestAdapter(
    private val items: List<MaterialRequest>,
    private val currentUserId: Long,
    private val isOwner: Boolean,
    private val onFulfill: (MaterialRequest) -> Unit,
    private val onClose: (MaterialRequest) -> Unit
) : RecyclerView.Adapter<RequestAdapter.ViewHolder>() {

    private fun displayStatus(status: String): String = when (status) {
        "OPEN" -> "Open"
        "FULFILLED" -> "Fulfilled"
        "CLOSED" -> "Closed"
        else -> status
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_request_title)
        val tvStatus: TextView = view.findViewById(R.id.tv_request_status)
        val tvRequester: TextView = view.findViewById(R.id.tv_requester_date)
        val btnFulfill: View = view.findViewById(R.id.btn_fulfill)
        val btnClose: View = view.findViewById(R.id.btn_menu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val req = items[position]
        holder.tvTitle.text = req.title
        holder.tvStatus.text = displayStatus(req.status)
        holder.tvRequester.text = "Requested by ${req.requesterName}"

        val isClosed = req.status == "CLOSED"
        holder.btnClose.visibility = if (!isClosed) View.VISIBLE else View.GONE
        
        val canFulfill = !isClosed && req.status == "OPEN" &&
            (isOwner || req.requesterId != currentUserId)
        holder.btnFulfill.visibility = if (canFulfill) View.VISIBLE else View.GONE
        
        holder.btnClose.setOnClickListener { onClose(req) }
        holder.btnFulfill.setOnClickListener { onFulfill(req) }

        holder.itemView.setOnClickListener { view ->
            val ctx = view.context as android.app.Activity
            val dlg = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)
            val sheetView = LayoutInflater.from(ctx).inflate(R.layout.bottom_sheet_request_detail, null)
            dlg.setContentView(sheetView)

            val tvTitle = sheetView.findViewById<TextView>(R.id.tv_title)
            val tvMeta = sheetView.findViewById<TextView>(R.id.tv_subtitle)
            val tvDesc = sheetView.findViewById<TextView>(R.id.tv_description)
            val tvStatusChip = sheetView.findViewById<TextView>(R.id.tv_status_badge)
            val tvClosureNote = sheetView.findViewById<TextView>(R.id.tv_closure_note)
            val btnCloseReq = sheetView.findViewById<Button>(R.id.btn_close_request)
            val btnFulfillReq = sheetView.findViewById<Button>(R.id.btn_fulfill_request)

            tvTitle.text = req.title
            tvMeta.text = "Requested by ${req.requesterName}"
            tvDesc.text = req.description ?: "No description provided."

            val statusColor = when (req.status) {
                "OPEN" -> android.graphics.Color.parseColor("#388E3C") // success_green
                "FULFILLED" -> android.graphics.Color.parseColor("#4DB6AC") // teal_badge
                else -> android.graphics.Color.parseColor("#9E9E9E") // status_to_read / CLOSED
            }
            tvStatusChip.text = displayStatus(req.status).uppercase()
            tvStatusChip.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 999f
                setColor(statusColor)
            }

            if (!req.closureNote.isNullOrBlank()) {
                tvClosureNote.visibility = View.VISIBLE
                tvClosureNote.text = "Closure Note: ${req.closureNote}"
            }

            btnCloseReq.visibility = if (!isClosed) View.VISIBLE else View.GONE
            btnFulfillReq.visibility = if (canFulfill) View.VISIBLE else View.GONE

            btnCloseReq.setOnClickListener {
                dlg.dismiss()
                onClose(req)
            }
            btnFulfillReq.setOnClickListener {
                dlg.dismiss()
                onFulfill(req)
            }

            dlg.show()
        }
    }

    override fun getItemCount() = items.size
}
