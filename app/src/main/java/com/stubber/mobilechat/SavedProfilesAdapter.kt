package com.stubber.mobilechat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SavedProfilesAdapter(
    private val onProfileClick: (ProfileConfiguration) -> Unit,
    private val onDeleteClick: (ProfileConfiguration) -> Unit
) : RecyclerView.Adapter<SavedProfilesAdapter.ProfileViewHolder>() {

    private var profiles = listOf<ProfileConfiguration>()

    fun submitList(newProfiles: List<ProfileConfiguration>) {
        profiles = newProfiles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_profile, parent, false)
        return ProfileViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        holder.bind(profiles[position])
    }

    override fun getItemCount(): Int = profiles.size

    inner class ProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileNameText: TextView = itemView.findViewById(R.id.profileNameText)
        private val profileDetailsText: TextView = itemView.findViewById(R.id.profileDetailsText)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteProfileButton)

        fun bind(profile: ProfileConfiguration) {
            profileNameText.text = profile.name
            val branch = if (profile.isDraft) "Draft" else "Live"
            profileDetailsText.text = "${profile.profileCode} · $branch"

            itemView.setOnClickListener {
                onProfileClick(profile)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(profile)
            }
        }
    }
}
