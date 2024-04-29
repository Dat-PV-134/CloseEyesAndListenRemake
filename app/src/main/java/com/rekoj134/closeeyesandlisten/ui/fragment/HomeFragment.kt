package com.rekoj134.closeeyesandlisten.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.rekoj134.closeeyesandlisten.constant.Constants
import com.rekoj134.closeeyesandlisten.epoxy.MyEpoxyController
import com.rekoj134.closeeyesandlisten.model.MusicItem
import com.rekoj134.closeeyesandlisten.remake.databinding.FragmentHomeBinding
import com.rekoj134.closeeyesandlisten.ui.activity.HistoryActivity
import com.rekoj134.closeeyesandlisten.ui.activity.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val listMusic = ArrayList<MusicItem>()
    private lateinit var myController: MyEpoxyController
    private var isFiltering = false
    private var isFragmentRunning = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val historyActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedSongId: String = result.data?.action!!
                (requireActivity() as MainActivity).startPlaying(getSongById(selectedSongId))
            }
        }

    private fun getSongById(id: String): MusicItem {
        var musicItem = MusicItem()
        for (item in listMusic) {
            if (item.id.toString() == id) musicItem = item
        }
        return musicItem
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isFragmentRunning = true
        setupView()
        initData("")
        handleEvent()
    }

    private fun handleEvent() {
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (requireActivity() as MainActivity).handleBackpress()
            }
        })

        binding.etSearch.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                isFiltering = binding.etSearch.text.toString() != ""
                initData(binding.etSearch.text.toString())
            }
        })

        binding.btnHistory.setOnClickListener {
            historyActivityLauncher.launch(Intent(requireContext(), HistoryActivity::class.java))
        }
    }

    private fun initData(keyword: String) {
        Log.e("ADWFWFWFWF", keyword)
        CoroutineScope(Dispatchers.IO).launch {
            val database = Firebase.database
            val myRef = database.reference.child(Constants.CHILD_MUSICS)
            myRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    listMusic.clear()
                    val tempList = dataSnapshot.getValue<List<MusicItem>>()
                    tempList?.let {
                        for (item in it) {
                            Log.e("ADWFWFWFWF", item.name + " - " + item.author)
                            if (item.name!!.contains(keyword, true) || item.author!!.contains(keyword, true)) listMusic.add(item)
                        }
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        if (isFragmentRunning) {
                            if (isFiltering) binding.rvMusics.layoutManager = GridLayoutManager(requireContext(), 2, RecyclerView.VERTICAL, false)
                            else binding.rvMusics.layoutManager = LinearLayoutManager(requireContext())
                            myController.setIsFiltering(isFiltering)
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

    private fun setupView() {
        myController = MyEpoxyController.newInstance(requireContext()) {
            (requireActivity() as MainActivity).startPlaying(it)
        }
        binding.rvMusics.setControllerAndBuildModels(myController)
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
        fun newInstance(): HomeFragment {
            val args = Bundle()
            val fragment: HomeFragment = HomeFragment()
            fragment.arguments = args
            return fragment
        }
    }
}