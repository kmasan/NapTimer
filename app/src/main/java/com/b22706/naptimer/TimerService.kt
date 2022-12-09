package com.b22706.naptimer

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    var mainTimerRunning = true

    lateinit var stopServicePendingIntent: PendingIntent
    lateinit var startTimerPendingIntent: PendingIntent
    lateinit var stopTimerPendingIntent: PendingIntent
    lateinit var resetTimerPendingIntent: PendingIntent

    private val buttonBr = object :BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("service","onReceive")

        }
    }

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
                        "start" ->{
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

                mainTimer = MyTimer(this, "main")
                subTimer = MyTimer(this, "sub")
                subTimer.timerMinute = 1
                subTimer.timerSecond = 0

                mediaPlayer.setOnCompletionListener {
                    if(!mediaPlayer.isLooping){
                        mediaPlayer.stop()
                        mediaPlayer.prepare()
                    }
                }

                val filter = IntentFilter(Intent.ACTION_ASSIST)
                registerReceiver(buttonBr, filter)
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(buttonBr)
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

        startTimerPendingIntent = PendingIntent.getBroadcast(this, 1,
            Intent(this, StartTimerBroadcastReceiver::class.java).apply {
                action = Intent.ACTION_SEND
            }, PendingIntent.FLAG_IMMUTABLE)

        stopTimerPendingIntent = PendingIntent.getBroadcast(this, 1,
            Intent(this, StopTimerBroadcastReceiver::class.java).apply {
                action = Intent.ACTION_SEND
            }, PendingIntent.FLAG_IMMUTABLE)

        resetTimerPendingIntent = PendingIntent.getBroadcast(this, 2,
            Intent(this, ResetTimerBroadcastReceiver::class.java).apply {
                action = Intent.ACTION_SEND
            }, PendingIntent.FLAG_IMMUTABLE)

        // startForegroundServiceでサービス起動から5秒以内にstartForegroundして通知を表示しないとANRエラーになる
        val notification: Notification = ForegroundServiceNotification.createServiceNotification(
            applicationContext, pendingIntent, stopServicePendingIntent, "alarm"
        )

        val notificationLayout = RemoteViews(packageName, R.layout.notification_layout).apply {
            setTextViewText(R.id.muniteView,"11")
            setOnClickPendingIntent(R.id.timerButton, startTimerPendingIntent)
            setOnClickPendingIntent(R.id.stopServiceButton, stopServicePendingIntent)
        }
        val notificationMiniLayout = RemoteViews(packageName, R.layout.notification_mini_layout).apply {
            setTextViewText(R.id.muniteViewM,"11")
            setOnClickPendingIntent(R.id.timerButtonM, startTimerPendingIntent)
        }
        val customNotification = ForegroundServiceNotification.createCustomNotification(
            applicationContext, notificationMiniLayout, notificationLayout
        )

        //Serviceの起動
        startForeground(
            ForegroundServiceNotification.FOREGROUND_SERVICE_NOTIFICATION_ID,
            customNotification
        )
    }

    fun notificationUiChange(){
        Log.i("service", "notificationUiChange")
        val notificationLayout = RemoteViews(packageName, R.layout.notification_layout).apply {
            setTextViewText(R.id.muniteView,"11")
            setTextViewText(R.id.secondView,"00")
            setTextViewText(R.id.timerButton,"start")
            setOnClickPendingIntent(R.id.timerButton, startTimerPendingIntent)
            setOnClickPendingIntent(R.id.stopServiceButton, stopServicePendingIntent)
        }
        val notificationMiniLayout = RemoteViews(packageName, R.layout.notification_mini_layout).apply {
            setTextViewText(R.id.muniteViewM,"11")
            setTextViewText(R.id.secondViewM,"00")
            setTextViewText(R.id.timerButtonM,"start")
            setOnClickPendingIntent(R.id.timerButtonM, startTimerPendingIntent)
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
        Log.i("service", "onTimerTick:${time}-${tag}")
        when(tag){
            "main" -> {
                changeTimerView(time)
            }
        }
    }

    private fun changeTimerView(time: Long){
        val minute = (time/(60*1000)).toInt()
        val second = ((time/1000)%60).toInt()

        val notificationLayout = RemoteViews(packageName, R.layout.notification_layout).apply {
            setTextViewText(R.id.muniteView,String.format("%02d", minute))
            setTextViewText(R.id.secondView,String.format("%02d", second))
            setTextViewText(R.id.timerButton,"stop")
            setOnClickPendingIntent(R.id.timerButton, stopTimerPendingIntent)
            setOnClickPendingIntent(R.id.stopServiceButton, stopServicePendingIntent)
        }
        val notificationMiniLayout = RemoteViews(packageName, R.layout.notification_mini_layout).apply {
            setTextViewText(R.id.muniteViewM,String.format("%02d", minute))
            setTextViewText(R.id.secondViewM,String.format("%02d", second))
            setTextViewText(R.id.timerButtonM,"stop")
            setOnClickPendingIntent(R.id.timerButtonM, stopTimerPendingIntent)
        }
        val customNotification = ForegroundServiceNotification.createCustomNotification(
            applicationContext, notificationMiniLayout, notificationLayout
        )

        startForeground(
            ForegroundServiceNotification.FOREGROUND_SERVICE_NOTIFICATION_ID,
            customNotification
        )
    }

    override fun onTimerFinish(tag: String) {
        Log.i("service", "onTimerFinish")
        when(tag){
            "main" -> {
                mainTimerRunning = false
                mediaPlayer.start()
                mainTimer.reset()
                subTimer.start()
                app.notificationMiniLayout.setTextViewText(R.id.timerButtonM,"アラーム停止")
                //startButtonText.postValue("アラーム停止")
            }
            "sub" -> {
                mainTimerRunning = true
                mediaPlayer.stop()
                mediaPlayer.prepare()
                subTimer.reset()
                mainTimer.start()
                app.notificationMiniLayout.setTextViewText(R.id.timerButtonM,"一時停止")
                //startButtonText.postValue("一時停止")
            }
        }
    }

    fun timerStart(){
        when {
            mainTimer.timerRunning -> {
                mainTimer.stop()
                app.notificationMiniLayout.setTextViewText(R.id.timerButtonM, "再開")
                //startButtonText.postValue("再開")
            }
            subTimer.timerRunning -> {
                mediaPlayer.stop()
                mediaPlayer.prepare()
                subTimer.reset()
                mainTimer.start()
                mainTimerRunning = true
                app.notificationMiniLayout.setTextViewText(R.id.timerButtonM, "一時停止")
                //startButtonText.postValue("一時停止")
            }
            else -> {
                if(mainTimerRunning){
                    mainTimer.start()
                    app.notificationMiniLayout.setTextViewText(R.id.timerButtonM, "一時停止")
                    //startButtonText.postValue("一時停止")
                }
            }
        }
    }

    fun timerReset() {
        mainTimer.reset()
        subTimer.reset()
        mainTimerRunning = true
        changeTimerView(mainTimer.timerMinute * 60 * 1000L)
        app.notificationMiniLayout.setTextViewText(R.id.timerButtonM, "スタート")
        //startButtonText.postValue("スタート")
    }
}