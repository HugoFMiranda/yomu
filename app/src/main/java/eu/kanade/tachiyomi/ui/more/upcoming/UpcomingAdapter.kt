package eu.kanade.tachiyomi.ui.more.upcoming

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.image.coil.loadManga
import eu.kanade.tachiyomi.databinding.UpcomingItemBinding
import java.text.DateFormat
import java.util.Date

class UpcomingAdapter(
    private val items: List<UpcomingPresenter.UpcomingItem>,
    private val onClick: (UpcomingPresenter.UpcomingItem) -> Unit,
) : RecyclerView.Adapter<UpcomingAdapter.UpcomingHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpcomingHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.upcoming_item, parent, false)
        return UpcomingHolder(view)
    }

    override fun onBindViewHolder(holder: UpcomingHolder, position: Int) {
        val item = items[position]
        holder.binding.mangaCover.loadManga(item.manga)
        holder.binding.mangaTitle.text = item.manga.title
        holder.binding.nextChapterDate.text = DateFormat.getDateInstance(DateFormat.MEDIUM)
            .format(Date(item.nextChapterDate))
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class UpcomingHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = UpcomingItemBinding.bind(view)
    }
}
