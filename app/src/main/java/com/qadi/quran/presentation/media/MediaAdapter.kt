package com.qadi.quran.presentation.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.qadi.quran.R
import com.qadi.quran.entity.Const
import com.qadi.quran.entity.Media
import com.qadi.quran.presentation.main.MainActivity
import kotlinx.android.synthetic.main.media_item.view.*

class MediaAdapter(private val mediaList: MutableList<Media> = mutableListOf()) :
    RecyclerView.Adapter<MediaAdapter.MediaHolder>() {

    override fun getItemCount(): Int = mediaList.size

    override fun onBindViewHolder(holder: MediaHolder, position: Int) {
        val mediaItem = mediaList[holder.adapterPosition];holder.itemView.title.text = mediaItem.title
        holder.itemView.title.isSelected = true
        holder.itemView.setOnClickListener {
            val ma = holder.itemView.context as MainActivity
            if (mediaItem.isList) {
                val bundle = Bundle();bundle.putString(Const.MEDIA_ID, mediaItem.id);bundle.putString(
                    "title",
                    mediaItem.title
                )
                ma.findNavController(R.id.nav_host_fragment).navigate(R.id.action_global_mediaFragment, bundle)
            } else {
                ma.playPause(mediaItem.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaHolder =
        MediaHolder(LayoutInflater.from(parent.context).inflate(R.layout.media_item, parent, false))

    fun updateMedia(mediaList: List<Media>) {
        if (this.mediaList.isNotEmpty()) this.mediaList.clear();this.mediaList.addAll(mediaList)
        notifyDataSetChanged()
    }

    class MediaHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}