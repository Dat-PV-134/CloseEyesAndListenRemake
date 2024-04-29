package com.rekoj134.closeeyesandlisten.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.rekoj134.closeeyesandlisten.constant.Constants
import com.rekoj134.closeeyesandlisten.epoxy.FavoriteController
import com.rekoj134.closeeyesandlisten.model.MusicItem
import com.rekoj134.closeeyesandlisten.remake.databinding.FragmentFavoriteBinding
import com.rekoj134.closeeyesandlisten.ui.activity.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoriteFragment : Fragment() {
    private var _binding: FragmentFavoriteBinding? = null
    private val binding get() = _binding!!
    private val listMusic = ArrayList<MusicItem>()
    private lateinit var myController: FavoriteController
    private var isFragmentRunning = false
    private var isFirstTime = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isFragmentRunning = true
        setupView()
        initData()
        handleEvent()
    }

    private fun initData() {
        getFavoriteList()
    }

    private fun getFavoriteList() {
        CoroutineScope(Dispatchers.IO).launch {
            val auth = Firebase.auth
            val currentUser = auth.currentUser
            if (currentUser == null) {
                binding.tvNeedLogin.visibility = View.VISIBLE
                myController.setListMusic(listMusic)
            } else {
                val database = Firebase.database
                val myRef =
                    database.reference.child(Constants.CHILD_USERS).child(currentUser.uid)
                        .child(Constants.CHILD_FAVORITE)
                myRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        listMusic.clear()
                        Log.e("Dawffaffwaf", dataSnapshot.toString())
                        val tempList = dataSnapshot.getValue<List<MusicItem>>()
                        tempList?.let {
                            for (item in it) {
                                listMusic.add(item)
                            }
                        }
                        if (isFirstTime) {
                            listMusic.reverse()
                            isFirstTime = false
                        }
                        CoroutineScope(Dispatchers.Main).launch {
                            if (isFragmentRunning) {
                                myController.setListMusic(listMusic)
                                myController.requestModelBuild()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
            }
        }
    }

    private fun setupView() {
        val auth = Firebase.auth
        val currentUser = auth.currentUser
        if (currentUser != null) {
            myController = FavoriteController.newInstance(requireContext(), {
                (requireActivity() as MainActivity).startPlaying(it, true)
            }, {
                unFavorite(it)
            })
            binding.rvFav.setControllerAndBuildModels(myController)
        }
    }

    private fun unFavorite(unFavItem: MusicItem) {
        val auth = Firebase.auth
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val database = Firebase.database.reference
            listMusic.remove(unFavItem)
            database.child(Constants.CHILD_USERS).child(auth.currentUser?.uid!!).child(Constants.CHILD_FAVORITE).setValue(listMusic)
        }
    }

    private fun handleEvent() {
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (requireActivity() as MainActivity).handleBackpress()
            }
        })
    }

    override fun onResume() {
        isFragmentRunning = true
        super.onResume()
    }

    override fun onPause() {
        isFragmentRunning = false
        super.onPause()
    }

    override fun onDestroyView() {
        isFragmentRunning = false
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): FavoriteFragment {
            val args = Bundle()
            val fragment: FavoriteFragment = FavoriteFragment()
            fragment.arguments = args
            return fragment
        }
    }
}