package com.b22706.naptimer

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.RemoteViews
import androidx.lifecycle.MutableLiveData
import java.io.File

class TimerService: Service(), MyTimer.TimerListener {
    lateinit var app: Application
    private lateinit var windowManager: WindowManager
    lateinit var mainTimer: MyTimer
    lateinit var subTimer: MyTimer

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
        windowManager = applicationContext
            .getSystemService(WINDOW_SERVICE) as WindowManager

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
                            Log.i("service", "tag:start")
                            timerStart()
                        }
                        "reset"->{
                            Log.i("service", "tag:reset")
                            timerReset()
                        }
                        "change"->{
                            Log.i("service", "tag:change")
                           //changeTimer()
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

    private fun notificationUiChange(minute: Int, second: Int, timer: String){
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

    fun changeTimer(times: List<Int>){
        mainTimer.change(times[0],times[1])
        subTimer.change(times[2],times[3])

        mainTimerMinute = times[0]
        mainTimerSecond = times[1]

        notificationUiChange(mainTimerMinute, mainTimerSecond, "start")
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
            }
            "sub" -> {
                timerRunning = true
                //mediaPlayer.stop()
                //mediaPlayer.prepare()
                subTimer.reset()
                mainTimer.start()
                app.notificationMiniLayout.setTextViewText(R.id.timerButtonM,"一時停止")
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
            }
            mainTimer.getTimerRunning() -> {
                Log.i("service", "timerStart-mainTimer")
                mainTimer.stop()
                timerRunning = false
                notificationUiChange(mainTimerMinute, mainTimerSecond, "start")
                app.notificationMiniLayout.setTextViewText(R.id.timerButtonM, "start")
            }
        }
    }

    fun timerReset() {
        mainTimer.reset()
        subTimer.reset()
        timerRunning = false

        notificationUiChange(mainTimerMinute, mainTimerSecond, "start")
    }

    fun changeTimer(){
        val intent = Intent(this, NumPickerDialogActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP //新規起動の記述
        application.startActivity(intent)

        val typeLayer = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            typeLayer, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
//        // dpを取得
//        val dpScale = resources.displayMetrics.density.toInt()
//        // 右上に配置
//        params.gravity = Gravity.TOP or Gravity.END
//        params.x = 20 * dpScale // 20dp
//        params.y = 80 * dpScale // 80dp

//        // ViewにTouchListenerを設定する
//        newView.setOnTouchListener { _, event ->
//            Log.d("debug", "onTouch")
//            if (event.action == MotionEvent.ACTION_DOWN) {
//                newView.performClick()
//
//                // Serviceを停止
//                stopSelf()
//            }
//            false
//        }
//
//        // Viewを画面上に追加
//        windowManager.addView(, params)

//        windowManager.removeView()
    }
}