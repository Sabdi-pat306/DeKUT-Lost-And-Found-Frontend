
package ac.ke.dkut.lostfound

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class ItemReportAdapter(
    private var items: List<ItemReport>,
    private var categories: Map<Int, String> = emptyMap(),
    private var locations: Map<Int, String> = emptyMap()
) : RecyclerView.Adapter<ItemReportAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.itemImage)
        val typeText: TextView = view.findViewById(R.id.itemTypeText)
        val descriptionText: TextView =
            view.findViewById(R.id.itemDescriptionText)
        val categoryText: TextView =
            view.findViewById(R.id.itemCategoryText)
        val locationText: TextView =
            view.findViewById(R.id.itemLocationText)
        val dateText: TextView = view.findViewById(R.id.itemDateText)
        val statusText: TextView = view.findViewById(R.id.itemStatusText)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_card, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int
    ) {
        val item = items[position]

        holder.typeText.text = "${item.report_type.uppercase()} ITEM"
        holder.descriptionText.text = item.item_description
        holder.categoryText.text =
            "Category: ${categories[item.category_id] ?: "Unknown"}"
        holder.locationText.text =
            "Location: ${locations[item.location_id] ?: "Unknown"}"
        holder.dateText.text = "Date: ${item.date_lost_found}"
        holder.statusText.text = "Status: ${item.item_status}"

        val imagePath = item.image

        if (!imagePath.isNullOrBlank()) {
            holder.image.visibility = View.VISIBLE

            val imageUrl = when {
                imagePath.startsWith("http://") ||
                        imagePath.startsWith("https://") -> imagePath

                imagePath.startsWith("/") ->
                    "http://10.0.2.2:8000$imagePath"

                else -> "http://10.0.2.2:8000/$imagePath"
            }

            holder.image.load(imageUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }
        } else {
            holder.image.setImageDrawable(null)
            holder.image.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ItemReport>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateMetadata(
        newCategories: Map<Int, String>,
        newLocations: Map<Int, String>
    ) {
        categories = newCategories
        locations = newLocations
        notifyDataSetChanged()
    }
}
