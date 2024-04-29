package com.rekoj134.closeeyesandlisten.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.rekoj134.closeeyesandlisten.constant.Constants
import com.rekoj134.closeeyesandlisten.epoxy.HistoryController
import com.rekoj134.closeeyesandlisten.model.MusicItem
import com.rekoj134.closeeyesandlisten.remake.databinding.ActivityHistoryBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private val listMusic = ArrayList<MusicItem>()
    private val listFavorite = ArrayList<MusicItem>()
    private lateinit var myController: HistoryController
    private var isFirstTime = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
    }

    private fun setupView() {
        val auth = Firebase.auth
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.tvNeedLogin.visibility = View.VISIBLE
        } else {
            myController = HistoryController.newInstance(this@HistoryActivity, {
                val resultIntent = Intent()
                resultIntent.action = it.id.toString()
                setResult(RESULT_OK, resultIntent)
                finish()
            }, {
                updateFavorite(it)
            })
            binding.rvFav.setControllerAndBuildModels(myController)
            initData()
        }
    }

    private fun updateFavorite(musicItem: MusicItem) {
        val auth = Firebase.auth
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val database = Firebase.database.reference
            if (musicItem.isFavorite) {
                musicItem.isFavorite = false
                listFavorite.remove(musicItem)
                database.child(Constants.CHILD_USERS).child(auth.currentUser?.uid!!).child(Constants.CHILD_FAVORITE).setValue(listFavorite)
            } else {
                listFavorite.add(musicItem)
                database.child(Constants.CHILD_USERS).child(auth.currentUser?.uid!!).child(Constants.CHILD_FAVORITE).setValue(listFavorite)
            }
            for (item in listMusic) {
                item.isFavorite = false
            }
            Log.e("dawfawfwawfafwa", listMusic.toString())
            for (item in listMusic) {
                if (listFavorite.contains(item)) item.isFavorite = true
            }
            myController.setOnClickFav {
                updateFavorite(it)
            }
            myController.setListMusic(listMusic)
            myController.requestModelBuild()
        }
    }

    private fun initData() {
        getHistoryList()
    }

    private fun getHistoryList() {
        CoroutineScope(Dispatchers.IO).launch {
            val auth = Firebase.auth
            val currentUser = auth.currentUser
            if (currentUser == null) {
                binding.tvNeedLogin.visibility = View.VISIBLE
            } else {
                val database = Firebase.database
                val myRef =
                    database.reference.child(Constants.CHILD_USERS).child(currentUser.uid)
                        .child(Constants.CHILD_HISTORY)
                myRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        listMusic.clear()
                        val tempList = dataSnapshot.getValue<List<MusicItem>>()
                        tempList?.let {
                            for (item in it) {
                                listMusic.add(item)
                            }
                        }
                        Log.e("dawfafwffwf", dataSnapshot.toString())
                        if (isFirstTime) {
                            listMusic.reverse()
                            isFirstTime = false
                        }
                        getFavoriteList()
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
            }
        }
    }

    private fun getFavoriteList() {
        CoroutineScope(Dispatchers.IO).launch {
            val auth = Firebase.auth
            val currentUser = auth.currentUser
            if (currentUser == null) {
                binding.tvNeedLogin.visibility = View.VISIBLE
            } else {
                val database = Firebase.database
                val myRef =
                    database.reference.child(Constants.CHILD_USERS).child(currentUser.uid)
                        .child(Constants.CHILD_FAVORITE)
                myRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        listFavorite.clear()
                        val tempList = dataSnapshot.getValue<List<MusicItem>>()
                        Log.e("dfawfawffafw", listMusic.toString())
                        Log.e("dfawfawffafw", dataSnapshot.toString())
                        tempList?.let {
                            for (item in it) {
                                item.isFavorite = false
                                listFavorite.add(item)
                            }
                            for (item in listMusic) {
                                if (listFavorite.contains(item)) item.isFavorite = true
                            }
                        }
                        CoroutineScope(Dispatchers.Main).launch {
                            myController.setListMusic(listMusic)
                            myController.requestModelBuild()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
            }
        }
    }
}