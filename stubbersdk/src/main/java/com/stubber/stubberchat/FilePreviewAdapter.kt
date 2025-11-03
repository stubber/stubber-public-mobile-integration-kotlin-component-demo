package com.stubber.stubbersdk.stubberchat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.stubber.stubbersdk.R
import com.stubber.stubbersdk.stubberchat.models.FilePreviewItem

class FilePreviewAdapter(
    private val onRemoveClick: (Int) -> Unit,
    private val fileServerUrl: String
) : RecyclerView.Adapter<FilePreviewAdapter.PreviewViewHolder>() {

    private val items = mutableListOf<FilePreviewItem>()

    fun submitList(newItems: List<FilePreviewItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file_preview, parent, false)
        return PreviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class PreviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.filePreviewImage)
        private val removeButton: ImageButton = itemView.findViewById(R.id.removeFileButton)

        fun bind(item: FilePreviewItem, position: Int) {
            // Load image from URI or attachment
            if (item.attachment != null) {
                // File has been uploaded, show from server
                val imageUrl = "${fileServerUrl}/${item.attachment.fileuuid}"
                imageView.load(imageUrl) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_report_image)
                }
            } else {
                // File not yet uploaded, show from local URI
                imageView.load(item.uri) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_report_image)
                }
            }

            removeButton.setOnClickListener {
                onRemoveClick(position)
            }
        }
    }
}
