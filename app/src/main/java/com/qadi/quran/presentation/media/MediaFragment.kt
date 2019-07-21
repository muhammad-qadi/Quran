package com.qadi.quran.presentation.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.qadi.quran.R
import com.qadi.quran.entity.Media
import com.qadi.quran.presentation.ext.hide
import com.qadi.quran.presentation.main.MainActivity
import kotlinx.android.synthetic.main.fragment_media.*

class MediaFragment : Fragment() {

    private val adapter by lazy { MediaAdapter(mutableListOf()) }
    private val parentMediaId: String by lazy { arguments?.getString("media-id")!! }
    private val vm by lazy { (activity as MainActivity).vm }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_media, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRV()
        loadMediaList()
    }

    private fun initRV() {
        context?.let {
            mediaRecyclerView.layoutManager = GridLayoutManager(it, 1);mediaRecyclerView.adapter = adapter
        }
    }

    private fun loadMediaList() {
        vm.mediaChildrenForParentId(parentMediaId).observe(this, Observer { updateAdapter(it) })
    }

    private fun updateAdapter(childrenMedia: List<Media>) {
        context?.let {
            adapter.updateMedia(childrenMedia)
            pb.hide()
        }
    }
}
