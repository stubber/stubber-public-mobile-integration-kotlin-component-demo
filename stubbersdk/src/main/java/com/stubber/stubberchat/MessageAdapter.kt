package com.stubber.stubbersdk.stubberchat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.stubber.stubbersdk.R
import com.stubber.stubbersdk.stubberchat.models.Message
import com.stubber.stubbersdk.stubberchat.models.MessageDirection
import java.util.Date
import java.util.concurrent.TimeUnit

class MessageAdapter(
    private val incomingLayoutId: Int,
    private val outgoingLayoutId: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<Message>()

    companion object {
        private const val VIEW_TYPE_INCOMING = 1
        private const val VIEW_TYPE_OUTGOING = 2
    }

    fun submitList(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].direction == MessageDirection.INCOMING) {
            VIEW_TYPE_INCOMING
        } else {
            VIEW_TYPE_OUTGOING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_INCOMING -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(incomingLayoutId, parent, false)
                IncomingMessageViewHolder(view)
            }
            VIEW_TYPE_OUTGOING -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(outgoingLayoutId, parent, false)
                OutgoingMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is IncomingMessageViewHolder -> holder.bind(message)
            is OutgoingMessageViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    // Incoming message view holder
    class IncomingMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView? = itemView.findViewById(R.id.messageText)
        private val timestampText: TextView? = itemView.findViewById(R.id.timestampText)
        private val attachmentsContainer: LinearLayout? = itemView.findViewById(R.id.attachmentsContainer)

        fun bind(message: Message) {
            // Set message text
            if (message.message.isNotEmpty()) {
                messageText?.visibility = View.VISIBLE
                messageText?.text = message.message
            } else {
                messageText?.visibility = View.GONE
            }

            timestampText?.text = formatTime(message.timestamp)

            // Display attachments
            attachmentsContainer?.removeAllViews()
            if (message.attachments.isNotEmpty()) {
                attachmentsContainer?.visibility = View.VISIBLE
                message.attachments.forEach { attachment ->
                    if (attachment.contentType.startsWith("image/")) {
                        val imageView = ImageView(itemView.context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                400
                            ).apply {
                                setMargins(0, 8, 0, 8)
                            }
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        val imageUrl = "${Environment.fileServerUrl}/${attachment.fileuuid}"
                        imageView.load(imageUrl) {
                            crossfade(true)
                            placeholder(android.R.drawable.ic_menu_gallery)
                            error(android.R.drawable.ic_menu_report_image)
                        }
                        attachmentsContainer?.addView(imageView)
                    }
                }
            } else {
                attachmentsContainer?.visibility = View.GONE
            }
        }

        private fun formatTime(timestamp: Date): String {
            val now = Date()
            val diff = now.time - timestamp.time
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)

            return when {
                days > 0 -> "${days}d ago"
                hours > 0 -> "${hours}h ago"
                minutes > 0 -> "${minutes}m ago"
                else -> "Just now"
            }
        }
    }

    // Outgoing message view holder
    class OutgoingMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView? = itemView.findViewById(R.id.messageText)
        private val timestampText: TextView? = itemView.findViewById(R.id.timestampText)
        private val attachmentsContainer: LinearLayout? = itemView.findViewById(R.id.attachmentsContainer)

        fun bind(message: Message) {
            // Set message text
            if (message.message.isNotEmpty()) {
                messageText?.visibility = View.VISIBLE
                messageText?.text = message.message
            } else {
                messageText?.visibility = View.GONE
            }

            timestampText?.text = formatTime(message.timestamp)

            // Display attachments
            attachmentsContainer?.removeAllViews()
            if (message.attachments.isNotEmpty()) {
                attachmentsContainer?.visibility = View.VISIBLE
                message.attachments.forEach { attachment ->
                    if (attachment.contentType.startsWith("image/")) {
                        val imageView = ImageView(itemView.context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                400
                            ).apply {
                                setMargins(0, 8, 0, 8)
                            }
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        val imageUrl = "${Environment.fileServerUrl}/${attachment.fileuuid}"
                        imageView.load(imageUrl) {
                            crossfade(true)
                            placeholder(android.R.drawable.ic_menu_gallery)
                            error(android.R.drawable.ic_menu_report_image)
                        }
                        attachmentsContainer?.addView(imageView)
                    }
                }
            } else {
                attachmentsContainer?.visibility = View.GONE
            }
        }

        private fun formatTime(timestamp: Date): String {
            val now = Date()
            val diff = now.time - timestamp.time
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)

            return when {
                days > 0 -> "${days}d ago"
                hours > 0 -> "${hours}h ago"
                minutes > 0 -> "${minutes}m ago"
                else -> "Just now"
            }
        }
    }
}
