package com.example.researchcenter.features.material

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.researchcenter.R

data class BookSuggestion(
    val title: String,
    val firstAuthor: String,
    val authors: String?,
    val publisher: String?,
    val year: String?,
    val isbn: String?,
    val description: String?
)

class BookSuggestionAdapter(
    private val items: List<BookSuggestion>,
    private val onPick: (BookSuggestion) -> Unit
) : RecyclerView.Adapter<BookSuggestionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvBookTitle)
        val tvAuthor: TextView = view.findViewById(R.id.tvBookAuthor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvAuthor.text = item.firstAuthor
        holder.itemView.setOnClickListener { onPick(item) }
    }

    override fun getItemCount() = items.size
}