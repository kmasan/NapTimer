package com.b22706.naptimer

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.lifecycle.MutableLiveData
import java.io.File

class TimerService: Service(), MyTimer.TimerListener {
    lateinit var app: Application
    lateinit var mainTimer: MyTimer
    lateinit var subTimer: MyTimer

    val startButtonText = MutableLiveData("スタート")
    var audioFile: File? = null
    var mediaPlayer = MediaPlayer()
    var timerRunning = false

    lateinit var stopServicePendingIntent: PendingIntent
    lateinit var timerPendingIntent: PendingIntent
    lateinit var resetTimerPendingIntent: PendingIntent
    lateinit var changeTimePendingIntent: PendingIntent
    var mainTimerMinute = 15
    var mainTimerSecond = 0

    private val binder = ServiceBinder()
    inner class ServiceBinder : Binder(){
        fun getService(): TimerService = this@TimerService
    }

    override fun onCreate() {
        Log.i("service", "onCreate")
        super.onCreate()
        app = application as Application

        // アプリ起動時にチャンネル作成するほうがいい
        ForegroundServiceNotification.createNotificationChannel(applicationContext)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("service", "onStartCommand")
        Log.i("service", "action:${intent.action}")
        Log.i("service", "type:${intent.type}")
        when(intent.action){
            Intent.ACTION_SEND -> {
                if(intent.type == "text/plain"){
                    when(intent.getStringExtra("tag")){
                        "timer" ->{
                            timerStart()
                            Log.i("service", "tag:start")
                        }
                        "reset"->{
                            timerReset()
                            Log.i("service", "tag:reset")
                        }
                        "change"->{

                        }
                    }
                }
            }
            else -> {
                startServiceSetting(intent)
                startTimerSetting()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Log.i("service", "onBind")
        //画面Activityとの通信接続処理
        return binder
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.i("service", "onRebind")
        //再接続時の処理
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i("service", "onUnbind")
        //通信終了時の処理
        return super.onUnbind(intent)
    }

    // フォラグランドサービスの開始
    @SuppressLint("RemoteViewLayout")
    private fun startServiceSetting(intent: Intent) {
        Log.i("service", "startServiceSetting")
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            intent.getIntExtra("REQUEST_CODE", 0),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        //サービス通知から停止可能なボタン
        stopServicePendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, StopServiceBroadcastReceiver::class.java).apply {
                action = Intent.ACTION_SEND
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        timerPendingIntent = PendingIntent.getBroadcast(this, 1,
            Intent(this, TimerBroadcastReceiver::class.java).apply {
                action = Intent.ACTION_SEND
            }, PendingIntent.FLAG_IMMUTABLE)

        resetTimerPendingIntent = PendingIntent.getBroadcast(this, 2,
            Intent(this, ResetTimerBroadcastReceiver::class.java).apply {
                action = Intent.ACTION_SEND
            }, PendingIntent.FLAG_IMMUTABLE)

        changeTimePendingIntent = PendingIntent.getBroadcast(this, 3,
            Intent(this, ChangeTimeBroadcastReceiver::class.java).apply {
                action = Intent.ACTION_SEND
            }, PendingIntent.FLAG_IMMUTABLE)

        // startForegroundServiceでサービス起動から5秒以内にstartForegroundして通知を表示しないとANRエラーになる
        val notification: Notification = ForegroundServiceNotification.createServiceNotification(
            applicationContext, pendingIntent, stopServicePendingIntent, "alarm"
        )

        notificationUiChange(15,0, "start")
    }

    private fun startTimerSetting(){
        mainTimer = MyTimer(this, "main")
        subTimer = MyTimer(this, "sub")
        subTimer.change(1,0)

        mediaPlayer.setOnCompletionListener {
            if(!mediaPlayer.isLooping){
                mediaPlayer.stop()
                mediaPlayer.prepare()
            }
        }
    }

    fun notificationUiChange(minute: Int, second: Int, timer: String){
        //Log.i("service", "notificationUiChange")

        val notificationLayout = RemoteViews(packageName, R.layout.notification_layout).apply {
            setTextViewText(R.id.muniteView,String.format("%02d", minute))
            setTextViewText(R.id.secondView,String.format("%02d", second))
            setTextViewText(R.id.timerButton,timer)
            setOnClickPendingIntent(R.id.timerButton, timerPendingIntent)
            setOnClickPendingIntent(R.id.resetButton, resetTimerPendingIntent)
            setOnClickPendingIntent(R.id.timeChangeButton, changeTimePendingIntent)
            setOnClickPendingIntent(R.id.stopServiceButton, stopServicePendingIntent)
        }
        val notificationMiniLayout = RemoteViews(packageName, R.layout.notification_mini_layout).apply {
            setTextViewText(R.id.muniteViewM,String.format("%02d", minute))
            setTextViewText(R.id.secondViewM,String.format("%02d", second))
            setTextViewText(R.id.timerButtonM,timer)
            setOnClickPendingIntent(R.id.timerButtonM, timerPendingIntent)
            setOnClickPendingIntent(R.id.resetButtonM, resetTimerPendingIntent)
        }
        val customNotification = ForegroundServiceNotification.createCustomNotification(
            applicationContext, notificationMiniLayout, notificationLayout
        )

        startForeground(
            ForegroundServiceNotification.FOREGROUND_SERVICE_NOTIFICATION_ID,
            customNotification
        )
    }

    override fun onTimerTick(time: Long, tag: String) {
        //Log.i("service", "onTimerTick:${time}-${tag}")
        when(tag){
            "main" -> {
                changeTimerView(time)
            }
        }
    }

    private fun changeTimerView(time: Long){
        mainTimerMinute = (time/(60*1000)).toInt()
        mainTimerSecond = ((time/1000)%60).toInt()

        notificationUiChange(mainTimerMinute, mainTimerSecond, "stop")
    }

    override fun onTimerFinish(tag: String) {
        Log.i("service", "onTimerFinish")
        when(tag){
            "main" -> {
                timerRunning = false
                //mediaPlayer.start()
                mainTimer.reset()
                subTimer.start()
                app.notificationMiniLayout.setTextViewText(R.id.timerButtonM,"アラーム停止")
                //startButtonText.postValue("アラーム停止")
            }
            "sub" -> {
                timerRunning = true
                //mediaPlayer.stop()
                //mediaPlayer.prepare()
                subTimer.reset()
                mainTimer.start()
                app.notificationMiniLayout.setTextViewText(R.id.timerButtonM,"一時停止")
                //startButtonText.postValue("一時停止")
            }
        }
    }

    fun timerStart(){
        when {
            !timerRunning -> {
                Log.i("service", "timerStart-timerRunning")
                mainTimer.start()
                app.notificationMiniLayout.setTextViewText(R.id.timerButtonM, "stop")
                timerRunning = true
                //startButtonText.postValue("一時停止")
            }
            subTimer.getTimerRunning() -> {
                Log.i("service", "timerStart-subTimer")
                //mediaPlayer.stop()
                //mediaPlayer.prepare()
                subTimer.reset()
                mainTimer.reset()
                mainTimer.start()
                timerRunning = true
                notificationUiChange(mainTimerMinute, mainTimerSecond, "restart")
                app.notificationMiniLayout.setTextViewText(R.id.timerButtonM, "restart")
                //startButtonText.postValue("一時停止")
            }
            mainTimer.getTimerRunning() -> {
                Log.i("service", "timerStart-mainTimer")
                mainTimer.stop()
                timerRunning = false
                notificationUiChange(mainTimerMinute, mainTimerSecond, "start")
                app.notificationMiniLayout.setTextViewText(R.id.timerButtonM, "start")
                //startButtonText.postValue("再開")
            }
        }
    }

    fun timerReset() {
        mainTimer.reset()
        subTimer.reset()
        timerRunning = true
        changeTimerView(mainTimer.timerMinute * 60 * 1000L)
        app.notificationMiniLayout.setTextViewText(R.id.timerButtonM, "スタート")
        //startButtonText.postValue("スタート")
    }
}