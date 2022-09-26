package com.rrat.distancetrackerapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.rrat.distancetrackerapp.utils.Constants.ACTION_SERVICE_START
import com.rrat.distancetrackerapp.utils.Constants.ACTION_SERVICE_STOP
import com.rrat.distancetrackerapp.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.rrat.distancetrackerapp.utils.Constants.NOTIFICATION_CHANNEL_NAME
import com.rrat.distancetrackerapp.utils.Constants.NOTIFICATION_ID
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class TrackerService : LifecycleService() {

    @Inject
    lateinit var notification: NotificationCompat.Builder

    @Inject
    lateinit var notificationManager: NotificationManager

    companion object{
        val started = MutableLiveData<Boolean>()
    }

    private fun setInitialValue(){
        started.postValue(false)
    }

    override fun onCreate() {
        setInitialValue()
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action){
                ACTION_SERVICE_START->{
                    started.postValue(true)
                    startForegroundService()
                }
                ACTION_SERVICE_STOP ->{
                    started.postValue(false)
                }
                else ->{}
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService(){
        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            notification.build()
            )
    }

    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun getLifecycle(): Lifecycle {
        return super.getLifecycle()
    }
}