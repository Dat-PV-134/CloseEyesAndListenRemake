package com.rekoj134.closeeyesandlisten.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.rekoj134.closeeyesandlisten.app.MyApplication
import com.rekoj134.closeeyesandlisten.constant.Constants
import com.rekoj134.closeeyesandlisten.receiver.MyReceiver
import com.rekoj134.closeeyesandlisten.remake.R
import com.rekoj134.closeeyesandlisten.ui.activity.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyService : Service() {
    private val myBinder = MyBinder()

    class MyBinder : Binder() {
        fun getMyService(): MyService {
            return MyService()
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return myBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            pushNotification(
                intent.getStringExtra(Constants.EXTRA_SONG_NAME),
                intent.getStringExtra(Constants.EXTRA_SONG_AUTHOR),
                intent.getStringExtra(Constants.EXTRA_IMAGE_URL),
                intent.getBooleanExtra(Constants.EXTRA_PLAY_OR_PAUSE, true)
            )
        }
        return START_NOT_STICKY
    }

    private fun pushNotification(songName: String?, songAuthor: String?, image: String?, isPlaying: Boolean) {
        Log.e("HELLLO", "COME HERE " + image)
        val notificationIntent = this.packageManager.getLaunchIntentForPackage(packageName);
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        CoroutineScope(Dispatchers.IO).launch {
            val icon = try {
                Glide.with(this@MyService)
                    .asBitmap()
                    .load(image)
                    .submit(512, 512)
                    .get()
            } catch (e: Exception) {
                BitmapFactory.decodeResource(this@MyService.resources, R.drawable.img_error)
            }
            CoroutineScope(Dispatchers.Main).launch {
                val notification = NotificationCompat.Builder(this@MyService, MyApplication.CHANNEL_ID)
                    .setContentTitle(songName)
                    .setContentText(songAuthor)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setOngoing(false)
                    .setContentIntent(pendingIntent)
                    .setLargeIcon(icon)
                    .setColor(Color.parseColor("#FFFFFF"))
                    .addAction(R.drawable.ic_previous_song, "Previous", getPendingIntent(this@MyService, Constants.ACTION_PREVIOUS))
                    .addAction(if (isPlaying) R.drawable.ic_pause_bottom else R.drawable.ic_play_bottom, "Pause", getPendingIntent(this@MyService, Constants.ACTION_PLAY_PAUSE))
                    .addAction(R.drawable.ic_next, "Next", getPendingIntent(this@MyService, Constants.ACTION_NEXT))
                    .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1, 2))
                    .build()

                startForeground(134, notification)
            }
        }
    }

    private fun getPendingIntent(context: Context, action: String): PendingIntent? {
        val intent = Intent(context, MyReceiver::class.java)
        intent.action = action
        return PendingIntent.getBroadcast(
            context.applicationContext,
            Constants.REQUEST_CODE_CONTROL,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}