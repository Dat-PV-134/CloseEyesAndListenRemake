package com.rekoj134.closeeyesandlisten.epoxy

import android.content.Context
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

class FavoriteController private constructor(): EpoxyController() {
    private lateinit var listMusic: ArrayList<MusicItem>
    private lateinit var context: Context
    private lateinit var onClickItem:(MusicItem) -> Unit
    private lateinit var onClickFav:(MusicItem) -> Unit
    private var isDoneFavorite = false

    fun setListMusic(listMusic: ArrayList<MusicItem>) {
        this.listMusic.clear()
        this.listMusic.addAll(listMusic)
        isDoneFavorite = true
    }

    override fun buildModels() {
        Carousel.setDefaultGlobalSnapHelperFactory(object : Carousel.SnapHelperFactory() {
            override fun buildSnapHelper(context: Context?): SnapHelper {
                return GravitySnapHelper(Gravity.CENTER)
            }
        })

        if (!isDoneFavorite && listMusic.isEmpty()) {
            LoadingItemModel().id("loading").addTo(this)
        } else {
            for (item in listMusic) {
                FavoriteItemModel(context, item, onClickItem, onClickFav).id(item.id).addTo(this)
            }
        }

        if (listMusic.isEmpty()) NoFileItemModel().id("no_file").addTo(this)

        isDoneFavorite = false
    }

    data class FavoriteItemModel(
        val context: Context,
        val musicItem: MusicItem,
        val onClickItem:(MusicItem) -> Unit,
        val onClickFav:(MusicItem) -> Unit
    ) : ViewBindingKotlinModel<ItemFavoriteBinding>(R.layout.item_favorite) {
        override fun ItemFavoriteBinding.bind() {
            tvFavSongName.text = musicItem.name
            tvFavSongAuthor.text = musicItem.author
            tvFavSongName.isSelected = true
            tvFavSongAuthor.isSelected = true
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
            tvNoFile.text = "Không có bài hát nào yêu thích"
        }
    }

    companion object {
        fun newInstance(context: Context, onClickItem:(MusicItem) -> Unit, onClickFav:(MusicItem) -> Unit): FavoriteController {
            val myEpoxyController = FavoriteController()
            myEpoxyController.context = context
            myEpoxyController.listMusic = ArrayList()
            myEpoxyController.onClickItem = onClickItem
            myEpoxyController.onClickFav = onClickFav
            return myEpoxyController
        }
    }
}