package com.rekoj134.closeeyesandlisten.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.MotionLayout.TransitionListener
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.rekoj134.closeeyesandlisten.base.BaseActivity
import com.rekoj134.closeeyesandlisten.constant.Constants
import com.rekoj134.closeeyesandlisten.extension.isUserInteractionEnabled
import com.rekoj134.closeeyesandlisten.model.MusicItem
import com.rekoj134.closeeyesandlisten.remake.R
import com.rekoj134.closeeyesandlisten.remake.databinding.ActivityMainBinding
import com.rekoj134.closeeyesandlisten.service.MyService
import com.rekoj134.closeeyesandlisten.ui.fragment.FavoriteFragment
import com.rekoj134.closeeyesandlisten.ui.fragment.HomeFragment
import com.rekoj134.closeeyesandlisten.ui.fragment.ProfileFragment
import com.rekoj134.closeeyesandlisten.util.FormatterUtil
import com.rekoj134.closeeyesandlisten.util.MediaPlayerCallback
import com.rekoj134.closeeyesandlisten.util.MediaPlayerUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private var myService: MyService? = null
    private var isServiceConnected = false
    private val uiUpdateHandler by lazy { Handler(Looper.getMainLooper()) }
    private val curPlayingList by lazy { ArrayList<MusicItem>() }
    private val listBeforeShuffle by lazy { ArrayList<MusicItem>() }
    private val favoriteList by lazy { ArrayList<MusicItem>() }
    private var curSong = MusicItem()
    private var curPosInPlayList = 0
    private var curRepeat = Constants.REPEAT_NONE
    private lateinit var auth: FirebaseAuth
    private var isPlayingFavorite = false
    private var curFragment = Constants.FRAGMENT_HOME
    private var isInStatePlaying = false

    private val musicReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_PREVIOUS -> {
                    playPreviousSong()
                }

                Constants.ACTION_NEXT -> {
                    playNextSong()
                }

                Constants.ACTION_PLAY_PAUSE -> {
                    MediaPlayerUtil.playOrPause()
                    playOrPause()
                }
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val myBinder = service as MyService.MyBinder?
            myService = myBinder?.getMyService()
            isServiceConnected = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            myService = null
            isServiceConnected = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        setupView()
        handleEvent()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun initData() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.ACTION_PREVIOUS)
        intentFilter.addAction(Constants.ACTION_PLAY_PAUSE)
        intentFilter.addAction(Constants.ACTION_NEXT)
        registerReceiver(musicReceiver, intentFilter)

        auth = Firebase.auth
        val currentUser = auth.currentUser
        if (currentUser != null) {
            getFavoriteList(currentUser.uid)
        }
    }

    private fun setupView() {
        replaceFragment(HomeFragment.newInstance())
        binding.rootView.transitionToState(R.id.endGone, 0)
        binding.tvSongNameBottom.isSelected = true
        binding.tvSongAuthor.isSelected = true
    }

    fun handleBackpress() {
        if (isInStatePlaying) {
            binding.rootView.transitionToState(R.id.end, 500)
        } else if (curFragment == Constants.FRAGMENT_HOME) finish()
        else {
            replaceFragment(HomeFragment.newInstance())
            updateBottomNav(Constants.FRAGMENT_HOME)
            curFragment = Constants.FRAGMENT_HOME
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleEvent() {
        binding.rootView.addTransitionListener(object : TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {

            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {

            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                isInStatePlaying = currentId == R.id.start
                binding.fragmentContainer.isUserInteractionEnabled(!isInStatePlaying)
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {

            }
        })

        binding.sbPlayer.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvCurrentTime.text = FormatterUtil.secondsToFormattedTime(progress)
                binding.tvTotalTime.text =
                    FormatterUtil.secondsToFormattedTime(seekBar.max - progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                MediaPlayerUtil.seekTo(seekBar.progress)
            }
        })

        binding.btnClosePlayer.setOnClickListener {
            binding.rootView.transitionToState(R.id.end, 500)
        }

        binding.imagePlay.setOnClickListener {
            MediaPlayerUtil.playOrPause()
            playOrPause()
        }

        binding.imgNext.setOnClickListener {
            playNextSong()
        }

        binding.imgBack.setOnClickListener {
            playPreviousSong()
        }

        binding.imgPlayBottom.setOnClickListener {
            MediaPlayerUtil.playOrPause()
            playOrPause()
        }

        binding.imgNextBottom.setOnClickListener {
            playNextSong()
        }

        binding.imgBackBottom.setOnClickListener {
            playPreviousSong()
        }

        binding.imgShuffing.setOnClickListener {
            shuffle()
        }

        binding.imgRepeat.setOnClickListener {
            handleRepeat()
        }

        binding.iconFav.setOnClickListener {
            auth = Firebase.auth
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(
                    this@MainActivity,
                    "Bạn cần đăng nhập để sử dụng tính năng này",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                addToFavorite()
            }
        }

        binding.btnHome.setOnClickListener {
            replaceFragment(HomeFragment.newInstance())
            updateBottomNav(Constants.FRAGMENT_HOME)
            curFragment = Constants.FRAGMENT_HOME
        }

        binding.btnFav.setOnClickListener {
            replaceFragment(FavoriteFragment.newInstance())
            updateBottomNav(Constants.FRAGMENT_FAVORITE)
            curFragment = Constants.FRAGMENT_FAVORITE
        }

        binding.btnProfile.setOnClickListener {
            replaceFragment(ProfileFragment.newInstance())
            updateBottomNav(Constants.FRAGMENT_PROFILE)
            curFragment = Constants.FRAGMENT_PROFILE
        }

        binding.tvCurrentTime
    }

    private fun addToFavorite() {
        val database = Firebase.database.reference
        val isInList = favoriteList.remove(curSong)
        if (isInList) {
            binding.iconFav.setImageResource(R.drawable.ic_fav_white)
        } else {
            Toast.makeText(this@MainActivity, "Đã thêm vào danh sách yêu thích", Toast.LENGTH_SHORT)
                .show()
            binding.iconFav.setImageResource(R.drawable.ic_favorite_active)
            favoriteList.add(curSong)
        }
        database.child(Constants.CHILD_USERS).child(auth.currentUser?.uid!!)
            .child(Constants.CHILD_FAVORITE).setValue(favoriteList)
    }

    private fun updateBottomNav(curFragment: Int) {
        binding.btnHome.setImageResource(if (curFragment == Constants.FRAGMENT_HOME) R.drawable.ic_home_active else R.drawable.ic_home)
        binding.btnFav.setImageResource(if (curFragment == Constants.FRAGMENT_FAVORITE) R.drawable.ic_favorite_home_active else R.drawable.ic_favorite)
        binding.btnProfile.setImageResource(if (curFragment == Constants.FRAGMENT_PROFILE) R.drawable.ic_profile_active else R.drawable.ic_profile)
    }

    private fun handleRepeat() {
        when (curRepeat) {
            Constants.REPEAT_NONE -> {
                curRepeat = Constants.REPEAT_LIST
                binding.imgRepeat.setImageResource(R.drawable.ic_repeat_list)
            }

            Constants.REPEAT_LIST -> {
                curRepeat = Constants.REPEAT_SONG
                binding.imgRepeat.setImageResource(R.drawable.ic_repeat_one)
            }

            Constants.REPEAT_SONG -> {
                curRepeat = Constants.REPEAT_NONE
                binding.imgRepeat.setImageResource(R.drawable.ic_repeat)
            }
        }
    }

    private fun shuffle() {
        if (listBeforeShuffle.isEmpty()) {
            listBeforeShuffle.addAll(curPlayingList)
            curPlayingList.remove(curSong)
            curPlayingList.shuffle()
            curPlayingList.add(curSong)
            curPosInPlayList = curPlayingList.size - 1
            binding.imgShuffing.setColorFilter(Color.parseColor("#3DDC84"))
        } else {
            curPlayingList.clear()
            var count = 0
            for (item in listBeforeShuffle) {
                curPlayingList.add(item)
                if (item.id == curSong.id) curPosInPlayList = count
                count++
            }
            listBeforeShuffle.clear()
            binding.imgShuffing.setColorFilter(Color.parseColor("#FFFFFF"))
        }
    }

    private fun playPreviousSong() {
        if (curPlayingList.isEmpty()) {
            startPlaying(curSong)
            return
        }
        val newPos = if (curPosInPlayList > 0) curPosInPlayList - 1 else curPlayingList.size - 1
        curPosInPlayList = newPos
        Log.e("ANCUTKO", "Pos $curPosInPlayList")
        val newSong = curPlayingList[newPos]
        initMusicData(newSong)
        updateHistory(newSong)
        startMusicService(newSong)
        MediaPlayerUtil.playNewOrResume(
            newSong.id!!,
            newSong.src.toString(),
            false,
            object : MediaPlayerCallback {
                override fun onPrepared() {
                    updateViewPlaying()
                    startMusicService(newSong)
                }

                override fun onFailed() {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.can_t_play_this_song),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onDone() {
                    onCompleteCurSong()
                }
            })
    }

    private fun playNextSong(isRepeatCurSong: Boolean = false) {
        if (curPlayingList.isEmpty()) {
            startPlaying(curSong)
            return
        }
        val newPos = if (curPosInPlayList < curPlayingList.size - 1) curPosInPlayList + 1 else 0
        curPosInPlayList = newPos
        Log.e("ANCUTKO", "Pos $curPosInPlayList")
        val newSong = curPlayingList[newPos]
        initMusicData(newSong)
        updateHistory(newSong)
        MediaPlayerUtil.playNewOrResume(
            newSong.id!!,
            newSong.src.toString(),
            isRepeatCurSong,
            object : MediaPlayerCallback {
                override fun onPrepared() {
                    updateViewPlaying()
                    startMusicService(newSong)
                }

                override fun onFailed() {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.can_t_play_this_song),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onDone() {
                    onCompleteCurSong()
                }
            })
    }

    private fun onCompleteCurSong() {
        when (curRepeat) {
            Constants.REPEAT_NONE -> {
                if (curPosInPlayList == curPlayingList.size - 1) {
                    binding.sbPlayer.progress = 0
                    binding.imagePlay.setImageResource(R.drawable.ic_play)
                    binding.imgPlayBottom.setImageResource(R.drawable.ic_play_bottom)
                } else playNextSong()
            }

            Constants.REPEAT_LIST -> playNextSong()

            Constants.REPEAT_SONG -> {
                curPosInPlayList--
                playNextSong(true)
            }
        }
    }

    private fun visibleMusicPlayer() {
        binding.rootView.transitionToState(R.id.start, 500)
    }

    private fun initMusicData(musicSelected: MusicItem) {
        curSong = musicSelected
        val isInList = favoriteList.contains(curSong)
        if (!isInList) binding.iconFav.setImageResource(R.drawable.ic_fav_white)
        else binding.iconFav.setImageResource(R.drawable.ic_favorite_active)
        Glide.with(this@MainActivity).load(musicSelected.image).error(R.drawable.img_error)
            .into(binding.imgSongPlayer)
        binding.tvSongName.text = musicSelected.name
        binding.tvSongAuthor.text = musicSelected.author
        binding.sbPlayer.progress = 0
        binding.sbPlayer.secondaryProgress = 0
        updateViewControlBottom()
    }

    private fun updateViewControlBottom() {
        binding.tvSongNameBottom.text = curSong.name
        binding.tvSongAuthorBottom.text = curSong.author
    }

    private fun updateViewPlaying() {
        playOrPause()
        binding.sbPlayer.max = MediaPlayerUtil.getDurationInSecond()
        updatePlayingTime()
        updateSeekbar()
        val uiUpdateRunnable = object : Runnable {
            override fun run() {
                if (MediaPlayerUtil.isPlaying()) {
                    updatePlayingTime()
                    updateSeekbar()
                }
                uiUpdateHandler.postDelayed(this, 1000)
            }
        }
        uiUpdateHandler.postDelayed(uiUpdateRunnable, 1000)
    }

    private fun updatePlayingTime() {
        binding.tvCurrentTime.text = MediaPlayerUtil.getCurrentTime()
        binding.tvTotalTime.text = MediaPlayerUtil.getRemainingTime()
    }

    private fun updateSeekbar() {
        binding.sbPlayer.progress = MediaPlayerUtil.getCurrentTimeInSecond()
        binding.sbPlayer.secondaryProgress = MediaPlayerUtil.getMediaBufferPercent()
    }

    private fun playOrPause() {
        if (MediaPlayerUtil.isPlaying()) binding.imagePlay.setImageResource(R.drawable.ic_pause)
        else binding.imagePlay.setImageResource(R.drawable.ic_play)
        if (MediaPlayerUtil.isPlaying()) binding.imgPlayBottom.setImageResource(R.drawable.ic_pause_bottom)
        else binding.imgPlayBottom.setImageResource(R.drawable.ic_play_bottom)
        startMusicService(curSong)
    }

    private fun turnOnMusicPlayer(musicSelected: MusicItem) {
        visibleMusicPlayer()
        initMusicData(musicSelected)
        MediaPlayerUtil.playNewOrResume(
            musicSelected.id!!,
            musicSelected.src.toString(),
            false,
            object : MediaPlayerCallback {
                override fun onPrepared() {
                    updateViewPlaying()
                    startMusicService(musicSelected)
                }

                override fun onFailed() {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.can_t_play_this_song),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onDone() {
                    onCompleteCurSong()
                }
            })
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    fun startPlaying(musicSelected: MusicItem, isFromFavorite: Boolean = false) {
        turnOnMusicPlayer(musicSelected)
        updateHistory(musicSelected)
        if (isFromFavorite) {
            isPlayingFavorite = true
            curPlayingList.clear()
            curPlayingList.addAll(favoriteList)
        } else {
            isPlayingFavorite = false
            CoroutineScope(Dispatchers.IO).launch {
                val database = Firebase.database
                val myRef = database.reference.child(Constants.CHILD_MUSICS)
                val query = myRef.orderByChild("playList").equalTo(musicSelected.playList)
                query.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        Log.e("ANCUTKO", dataSnapshot.toString())
                        curPlayingList.clear()
                        var count = 0
                        for (data in dataSnapshot.children) {
                            data.getValue<MusicItem>()?.let {
                                if (it.id == curSong.id) {
                                    curPosInPlayList = count
                                    Log.e("ANCUTKO", curPosInPlayList.toString())
                                }
                                curPlayingList.add(it)
                                count++
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
            }
        }
    }

    private fun getFavoriteList(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = Firebase.database
            val myRef =
                database.reference.child(Constants.CHILD_USERS).child(id)
                    .child(Constants.CHILD_FAVORITE)
            myRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    favoriteList.clear()
                    val tempList = dataSnapshot.getValue<List<MusicItem>>()
                    tempList?.let {
                        for (item in it) {
                            favoriteList.add(item)
                        }
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        val isInList = favoriteList.contains(curSong)
                        if (!isInList) binding.iconFav.setImageResource(R.drawable.ic_fav_white)
                        else binding.iconFav.setImageResource(R.drawable.ic_favorite_active)

                        if (isPlayingFavorite) {
                            curPlayingList.clear()
                            curPlayingList.addAll(favoriteList)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
        }
    }

    private fun startMusicService(musicSelected: MusicItem) {
        val intent = Intent(this@MainActivity, MyService::class.java)
        intent.putExtra(Constants.EXTRA_SONG_NAME, musicSelected.name)
        intent.putExtra(Constants.EXTRA_SONG_AUTHOR, musicSelected.author)
        intent.putExtra(Constants.EXTRA_IMAGE_URL, musicSelected.image)
        intent.putExtra(Constants.EXTRA_PLAY_OR_PAUSE, MediaPlayerUtil.isPlaying())
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun updateHistory(musicSelected: MusicItem) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // do nothing
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                val database = Firebase.database
                val myRef = database.reference.child(Constants.CHILD_USERS).child(currentUser.uid)
                        .child(Constants.CHILD_HISTORY)
                myRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val historyList = ArrayList<MusicItem>()
                        val tempList = dataSnapshot.getValue<List<MusicItem>>()
                        tempList?.let {
                            historyList.addAll(it)
                        }
                        historyList.remove(musicSelected)
                        historyList.add(musicSelected)
                        database.reference.child(Constants.CHILD_USERS).child(auth.currentUser?.uid!!).child(Constants.CHILD_HISTORY).setValue(historyList)
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
            }
        }
    }

    private fun stopBoundService() {
        if (isServiceConnected) {
            unbindService(serviceConnection)
            isServiceConnected = false
        }
    }

    private fun stopForegroundService() {
        val intent = Intent(this@MainActivity, MyService::class.java)
        stopService(intent)
    }

    override fun onDestroy() {
        MediaPlayerUtil.releaseAll()
        unregisterReceiver(musicReceiver)
        stopBoundService()
        stopForegroundService()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
    }
}