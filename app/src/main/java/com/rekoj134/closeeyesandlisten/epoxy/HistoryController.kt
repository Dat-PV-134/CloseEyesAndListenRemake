package com.rekoj134.closeeyesandlisten.epoxy

import android.content.Context
import android.util.Log
import android.view.Gravity
import androidx.recyclerview.widget.SnapHelper
import com.airbnb.epoxy.Carousel
import com.airbnb.epoxy.EpoxyController
import com.bumptech.glide.Glide
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import com.rekoj134.closeeyesandlisten.model.MusicItem
import com.rekoj134.closeeyesandlisten.remake.R
import com.rekoj134.closeeyesandlisten.remake.databinding.ItemFavoriteBinding
import com.rekoj134.closeeyesandlisten.remake.databinding.ItemLoadingFullBinding
import com.rekoj134.closeeyesandlisten.remake.databinding.ItemNoFileBinding

class HistoryController private constructor(): EpoxyController() {
    private lateinit var listMusic: ArrayList<MusicItem>
    private lateinit var context: Context
    private var isDoneMusic = false
    private lateinit var onClickItem:(MusicItem) -> Unit
    private lateinit var onClickFav:(MusicItem) -> Unit

    fun setOnClickFav(onClickItem: (MusicItem) -> Unit) {
        this.onClickFav = onClickItem
    }

    fun setListMusic(listMusic: ArrayList<MusicItem>) {
        this.listMusic.clear()
        this.listMusic.addAll(listMusic)
        isDoneMusic = true
    }

    override fun buildModels() {
        Carousel.setDefaultGlobalSnapHelperFactory(object : Carousel.SnapHelperFactory() {
            override fun buildSnapHelper(context: Context?): SnapHelper {
                return GravitySnapHelper(Gravity.CENTER)
            }
        })

        Log.e("dawfawfawafaw", "COME HERE")
        if (!isDoneMusic) {
            Log.e("dawfawfawafaw", "COME HERE1")
            LoadingItemModel().id("loading").addTo(this)
        } else {
            Log.e("dawfawfawafaw", "COME HERE2")
            Log.e("dawfawfawafaw", listMusic.toString())
            for (item in listMusic) {
                FavoriteItemModel(context, item, onClickItem, onClickFav).id(item.id.toString()).addTo(this)
            }
        }

        if (listMusic.isEmpty()) NoFileItemModel().id("no_file").addTo(this)
    }

    data class FavoriteItemModel(
        val context: Context,
        val musicItem: MusicItem,
        val onClickItem:(MusicItem) -> Unit,
        val onClickFav:(MusicItem) -> Unit
    ) : ViewBindingKotlinModel<ItemFavoriteBinding>(R.layout.item_favorite) {
        override fun ItemFavoriteBinding.bind() {
            Log.e("dawfawfawafaw", musicItem.isFavorite.toString())
            tvFavSongName.text = musicItem.name
            tvFavSongAuthor.text = musicItem.author
            tvFavSongName.isSelected = true
            tvFavSongAuthor.isSelected = true
            iconFavInFragment.setImageResource(if (!musicItem.isFavorite) R.drawable.ic_fav_white else R.drawable.ic_favorite_active)
            Glide.with(context).load(musicItem.image).error(R.drawable.img_error).into(imgSongFav)
            root.setOnClickListener {
                onClickItem(musicItem)
            }
            iconFavInFragment.setOnClickListener {
                onClickFav(musicItem)
            }
        }
    }

    class LoadingItemModel(
    ) : ViewBindingKotlinModel<ItemLoadingFullBinding>(R.layout.item_loading_full) {
        override fun ItemLoadingFullBinding.bind() {}
    }

    class NoFileItemModel(
    ) : ViewBindingKotlinModel<ItemNoFileBinding>(R.layout.item_no_file) {
        override fun ItemNoFileBinding.bind() {
            tvNoFile.text = "Không có lịch sử"
        }
    }

    companion object {
        fun newInstance(context: Context, onClickItem:(MusicItem) -> Unit, onClickFav:(MusicItem) -> Unit): HistoryController {
            val myEpoxyController = HistoryController()
            myEpoxyController.context = context
            myEpoxyController.listMusic = ArrayList()
            myEpoxyController.onClickItem = onClickItem
            myEpoxyController.onClickFav = onClickFav
            return myEpoxyController
        }
    }
}