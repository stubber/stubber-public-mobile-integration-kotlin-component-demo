package com.stubber.stubbersdk.stubberchat

import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import io.noties.markwon.Markwon
import com.stubber.stubbersdk.R
import com.stubber.stubbersdk.stubberchat.models.Message
import com.stubber.stubbersdk.stubberchat.models.MessageDirection
import java.util.Date
import java.util.concurrent.TimeUnit

class MessageAdapter(
    private val incomingLayoutId: Int,
    private val outgoingLayoutId: Int,
    private val primaryColor: Int,
    private val fileServerUrl: String
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
                OutgoingMessageViewHolder(view, primaryColor)
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
    inner class IncomingMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView? = itemView.findViewById(R.id.messageText)
        private val timestampText: TextView? = itemView.findViewById(R.id.timestampText)
        private val attachmentsContainer: LinearLayout? = itemView.findViewById(R.id.attachmentsContainer)

        fun bind(message: Message) {
            // Set message text
            if (message.message.isNotEmpty()) {
                messageText?.visibility = View.VISIBLE

                // Render markdown if the message type is markdown
                if (message.type == "markdown") {
                    val markwon = Markwon.create(itemView.context)
                    markwon.setMarkdown(messageText!!, message.message)
                } else {
                    messageText?.text = message.message
                }
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
                        val imageUrl = "${fileServerUrl}/${attachment.fileuuid}"
                        imageView.load(imageUrl) {
                            crossfade(true)
                            placeholder(android.R.drawable.ic_menu_gallery)
                            error(android.R.drawable.ic_menu_report_image)
                        }
                        attachmentsContainer?.addView(imageView)
                    } else if (attachment.contentType.startsWith("audio/")) {
                        val audioUrl = "${fileServerUrl}/${attachment.fileuuid}"
                        val audioPlayerView = LayoutInflater.from(itemView.context)
                            .inflate(R.layout.item_audio_player, attachmentsContainer, false)
                        setupAudioPlayer(audioPlayerView, audioUrl)
                        attachmentsContainer?.addView(audioPlayerView)
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

        private fun setupAudioPlayer(audioPlayerView: View, audioUrl: String) {
            val playPauseButton: ImageButton = audioPlayerView.findViewById(R.id.playPauseButton)
            val audioSeekBar: SeekBar = audioPlayerView.findViewById(R.id.audioSeekBar)
            val audioDuration: TextView = audioPlayerView.findViewById(R.id.audioDuration)

            var mediaPlayer: MediaPlayer? = null
            val handler = Handler(Looper.getMainLooper())
            var isPlaying = false

            val updateRunnable = object : Runnable {
                override fun run() {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            val currentPosition = player.currentPosition
                            val duration = player.duration
                            audioSeekBar.progress = (currentPosition * 100 / duration)
                            audioDuration.text = "${formatAudioTime(currentPosition)} / ${formatAudioTime(duration)}"
                            handler.postDelayed(this, 100)
                        }
                    }
                }
            }

            playPauseButton.setOnClickListener {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    playPauseButton.setImageResource(android.R.drawable.ic_media_play)
                    handler.removeCallbacks(updateRunnable)
                    isPlaying = false
                } else {
                    if (mediaPlayer == null) {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(audioUrl)
                            prepareAsync()
                            setOnPreparedListener { player ->
                                player.start()
                                audioDuration.text = "0:00 / ${formatAudioTime(player.duration)}"
                                playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
                                handler.post(updateRunnable)
                                isPlaying = true
                            }
                            setOnCompletionListener {
                                playPauseButton.setImageResource(android.R.drawable.ic_media_play)
                                audioSeekBar.progress = 0
                                handler.removeCallbacks(updateRunnable)
                                isPlaying = false
                            }
                        }
                    } else {
                        mediaPlayer?.start()
                        playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
                        handler.post(updateRunnable)
                        isPlaying = true
                    }
                }
            }

            audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        mediaPlayer?.let { player ->
                            val newPosition = (progress * player.duration / 100)
                            player.seekTo(newPosition)
                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        private fun formatAudioTime(millis: Int): String {
            val seconds = millis / 1000
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return String.format("%d:%02d", minutes, remainingSeconds)
        }
    }

    // Outgoing message view holder
    inner class OutgoingMessageViewHolder(itemView: View, primaryColor: Int) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView? = itemView.findViewById(R.id.messageText)
        private val timestampText: TextView? = itemView.findViewById(R.id.timestampText)
        private val attachmentsContainer: LinearLayout? = itemView.findViewById(R.id.attachmentsContainer)
        private val messageBubble: LinearLayout?

        init {
            // Find the message bubble container (the LinearLayout that contains all the message content)
            messageBubble = messageText?.parent as? LinearLayout

            // Create a GradientDrawable with rounded corners and the primary color
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(primaryColor)
                // Set corner radii (top-left, top-right, bottom-right, bottom-left)
                cornerRadii = floatArrayOf(
                    48f, 48f,  // top-left
                    48f, 48f,  // top-right
                    12f, 12f,  // bottom-right
                    48f, 48f   // bottom-left
                )
            }

            // Apply the drawable to the message bubble
            messageBubble?.background = drawable
        }

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
                        val imageUrl = "${fileServerUrl}/${attachment.fileuuid}"
                        imageView.load(imageUrl) {
                            crossfade(true)
                            placeholder(android.R.drawable.ic_menu_gallery)
                            error(android.R.drawable.ic_menu_report_image)
                        }
                        attachmentsContainer?.addView(imageView)
                    } else if (attachment.contentType.startsWith("audio/")) {
                        val audioUrl = "${fileServerUrl}/${attachment.fileuuid}"
                        val audioPlayerView = LayoutInflater.from(itemView.context)
                            .inflate(R.layout.item_audio_player, attachmentsContainer, false)
                        setupAudioPlayer(audioPlayerView, audioUrl)
                        attachmentsContainer?.addView(audioPlayerView)
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

        private fun setupAudioPlayer(audioPlayerView: View, audioUrl: String) {
            val playPauseButton: ImageButton = audioPlayerView.findViewById(R.id.playPauseButton)
            val audioSeekBar: SeekBar = audioPlayerView.findViewById(R.id.audioSeekBar)
            val audioDuration: TextView = audioPlayerView.findViewById(R.id.audioDuration)

            var mediaPlayer: MediaPlayer? = null
            val handler = Handler(Looper.getMainLooper())
            var isPlaying = false

            val updateRunnable = object : Runnable {
                override fun run() {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            val currentPosition = player.currentPosition
                            val duration = player.duration
                            audioSeekBar.progress = (currentPosition * 100 / duration)
                            audioDuration.text = "${formatAudioTime(currentPosition)} / ${formatAudioTime(duration)}"
                            handler.postDelayed(this, 100)
                        }
                    }
                }
            }

            playPauseButton.setOnClickListener {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    playPauseButton.setImageResource(android.R.drawable.ic_media_play)
                    handler.removeCallbacks(updateRunnable)
                    isPlaying = false
                } else {
                    if (mediaPlayer == null) {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(audioUrl)
                            prepareAsync()
                            setOnPreparedListener { player ->
                                player.start()
                                audioDuration.text = "0:00 / ${formatAudioTime(player.duration)}"
                                playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
                                handler.post(updateRunnable)
                                isPlaying = true
                            }
                            setOnCompletionListener {
                                playPauseButton.setImageResource(android.R.drawable.ic_media_play)
                                audioSeekBar.progress = 0
                                handler.removeCallbacks(updateRunnable)
                                isPlaying = false
                            }
                        }
                    } else {
                        mediaPlayer?.start()
                        playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
                        handler.post(updateRunnable)
                        isPlaying = true
                    }
                }
            }

            audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        mediaPlayer?.let { player ->
                            val newPosition = (progress * player.duration / 100)
                            player.seekTo(newPosition)
                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        private fun formatAudioTime(millis: Int): String {
            val seconds = millis / 1000
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return String.format("%d:%02d", minutes, remainingSeconds)
        }
    }
}
