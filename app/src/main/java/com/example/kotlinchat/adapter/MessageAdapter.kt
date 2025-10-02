package com.example.kotlinchat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlinchat.R
import com.example.kotlinchat.models.Attachment
import com.example.kotlinchat.models.Message
import com.example.kotlinchat.models.MessageDirection
import com.example.kotlinchat.models.UploadingAttachment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
                    .inflate(R.layout.item_message_in, parent, false)
                IncomingMessageViewHolder(view)
            }
            VIEW_TYPE_OUTGOING -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_out, parent, false)
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
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        private val attachmentsContainer: LinearLayout = itemView.findViewById(R.id.attachmentsContainer)

        fun bind(message: Message) {
            messageText.text = message.message
            timestampText.text = formatTime(message.timestamp)

            // Show attachments
            attachmentsContainer.removeAllViews()
            if (message.attachments.isNotEmpty()) {
                attachmentsContainer.visibility = View.VISIBLE
                for (attachment in message.attachments) {
                    addAttachmentView(attachment)
                }
            } else {
                attachmentsContainer.visibility = View.GONE
            }
        }

        private fun addAttachmentView(attachment: Attachment) {
            val imageView = ImageView(itemView.context).apply {
                layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                    marginEnd = 8
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            Glide.with(itemView.context)
                .load(attachment.url)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(imageView)

            attachmentsContainer.addView(imageView)
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
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        private val attachmentsContainer: LinearLayout = itemView.findViewById(R.id.attachmentsContainer)
        private val uploadingAttachmentsContainer: LinearLayout = itemView.findViewById(R.id.uploadingAttachmentsContainer)

        fun bind(message: Message) {
            messageText.text = message.message
            timestampText.text = formatTime(message.timestamp)

            // Show uploading attachments
            uploadingAttachmentsContainer.removeAllViews()
            if (message.uploadingAttachments.isNotEmpty()) {
                uploadingAttachmentsContainer.visibility = View.VISIBLE
                for (uploadingAttachment in message.uploadingAttachments) {
                    addUploadingAttachmentView(uploadingAttachment)
                }
            } else {
                uploadingAttachmentsContainer.visibility = View.GONE
            }

            // Show completed attachments
            attachmentsContainer.removeAllViews()
            if (message.attachments.isNotEmpty()) {
                attachmentsContainer.visibility = View.VISIBLE
                for (attachment in message.attachments) {
                    addAttachmentView(attachment)
                }
            } else {
                attachmentsContainer.visibility = View.GONE
            }
        }

        private fun addUploadingAttachmentView(uploadingAttachment: UploadingAttachment) {
            val textView = TextView(itemView.context).apply {
                text = "⏳ ${uploadingAttachment.filename}"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 4
                }
                setPadding(8, 4, 8, 4)
                setBackgroundResource(android.R.color.holo_orange_light)
                setTextColor(itemView.context.getColor(android.R.color.white))
                textSize = 12f
            }
            uploadingAttachmentsContainer.addView(textView)
        }

        private fun addAttachmentView(attachment: Attachment) {
            val imageView = ImageView(itemView.context).apply {
                layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                    marginEnd = 8
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            Glide.with(itemView.context)
                .load(attachment.url)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(imageView)

            attachmentsContainer.addView(imageView)
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
