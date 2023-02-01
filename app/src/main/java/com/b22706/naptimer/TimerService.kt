package com.b22706.naptimer

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import android.widget.RemoteViews
import java.io.File

class TimerService: Service(), MyTimer.TimerListener {
    lateinit var app: Application
    private lateinit var windowManager: WindowManager
    private lateinit var notificationManager: NotificationManager
    lateinit var mainTimer: MyTimer
    lateinit var subTimer: MyTimer

    var audioFile: File? = null
    val mediaPlayer = MediaPlayer()
    var timerRunning = false

    lateinit var notificationLayout: RemoteViews
    lateinit var notificationMiniLayout: RemoteViews

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
        notificationManager = ForegroundServiceNotification.createNotificationChannel(applicationContext)
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
    private fun startServiceSetting(intent: Intent) {
        // Log.i("service", "startServiceSetting")

        // Timerの設定
        mainTimer = MyTimer(this, "main")
        subTimer = MyTimer(this, "sub")
        subTimer.change(1,0)

        // アラームの再生が完了した際の処理
        mediaPlayer.setOnCompletionListener {
            if(!mediaPlayer.isLooping){
                mediaPlayer.stop()
                mediaPlayer.prepare()
            }
        }

        //サービス通知から停止可能なボタン
        val stopServicePendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, StopServiceBroadcastReceiver::class.java).apply {
                action = Intent.ACTION_SEND
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        // Timerの開始・停止用
        val timerPendingIntent = PendingIntent.getBroadcast(this, 1,
            Intent(this, TimerBroadcastReceiver::class.java).apply {
                action = Intent.ACTION_SEND
            }, PendingIntent.FLAG_IMMUTABLE)

        // Timerのリセットボタン用
        val resetTimerPendingIntent = PendingIntent.getBroadcast(this, 2,
            Intent(this, ResetTimerBroadcastReceiver::class.java).apply {
                action = Intent.ACTION_SEND
            }, PendingIntent.FLAG_IMMUTABLE)

        // Timerの時間変更用
        val changeTimePendingIntent = PendingIntent.getBroadcast(this, 3,
            Intent(this, ChangeTimeBroadcastReceiver::class.java).apply {
                action = Intent.ACTION_SEND
            }, PendingIntent.FLAG_IMMUTABLE)

        // 初期状態の通知を設定
        notificationLayout = RemoteViews(packageName, R.layout.notification_layout).apply {
            setTextViewText(R.id.muniteView,String.format("%02d", 15))
            setTextViewText(R.id.secondView,String.format("%02d", 0))
            setTextViewText(R.id.timerButton,"start")
            setOnClickPendingIntent(R.id.timerButton, timerPendingIntent)
            setOnClickPendingIntent(R.id.resetButton, resetTimerPendingIntent)
            setOnClickPendingIntent(R.id.timeChangeButton, changeTimePendingIntent)
            setOnClickPendingIntent(R.id.stopServiceButton, stopServicePendingIntent)
        }
        notificationMiniLayout = RemoteViews(packageName, R.layout.notification_mini_layout).apply {
            setTextViewText(R.id.muniteViewM,String.format("%02d", 15))
            setTextViewText(R.id.secondViewM,String.format("%02d", 0))
            setTextViewText(R.id.timerButtonM,"start")
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

    private fun notificationUiChange(minute: Int, second: Int, timer: String){
        //Log.i("service", "notificationUiChange")

        notificationLayout.apply {
            setTextViewText(R.id.muniteView,String.format("%02d", minute))
            setTextViewText(R.id.secondView,String.format("%02d", second))
            setTextViewText(R.id.timerButton,timer)
        }
        notificationMiniLayout.apply {
            setTextViewText(R.id.muniteViewM,String.format("%02d", minute))
            setTextViewText(R.id.secondViewM,String.format("%02d", second))
            setTextViewText(R.id.timerButtonM,timer)
        }
        val customNotification = ForegroundServiceNotification.createCustomNotification(
            applicationContext, notificationMiniLayout, notificationLayout
        )

        notificationManager.notify(
            ForegroundServiceNotification.FOREGROUND_SERVICE_NOTIFICATION_ID,
            customNotification
        )
    }

    override fun onTimerTick(time: Long, tag: String) {
        //Log.i("service", "onTimerTick:${time}-${tag}")
        when(tag){
            "main" -> {
                changeTimerView(time, "stop")
            }
        }
    }

    fun changeTimer(times: List<Int>){
        mainTimer.change(times[0],times[1])
        subTimer.change(times[2],times[3])

        notificationUiChange(mainTimer.minute, mainTimer.second, "start")
    }

    private fun changeTimerView(time: Long, string: String){
        val minute = (time/(60*1000)).toInt()
        val second = ((time/1000)%60).toInt()

        notificationUiChange(minute, second, string)
    }

    // Timer終了時
    override fun onTimerFinish(tag: String) {
        Log.i("service", "onTimerFinish")
        when(tag){
            "main" -> {
                // アラームを再生しSubTimerを開始
                mediaPlayer.start()
                mainTimer.reset()
                subTimer.start()
                changeTimerView(0,  "alarm stop")
            }
            "sub" -> {
                // アラームを停止しMainTimerを開始
                mediaPlayer.stop()
                mediaPlayer.prepare()
                subTimer.reset()
                mainTimer.start()
                notificationUiChange(mainTimer.minute, mainTimer.second, "stop")
            }
        }
    }

    // Timerの開始・停止
    fun timerStart(){
        when {
            // Timer停止時
            !timerRunning -> {
                Log.i("service", "timerStart-timerRunning")
                // MainTimerを開始
                mainTimer.start()
                timerRunning = true
            }
            // SubTimer稼働時
            subTimer.getTimerRunning() -> {
                Log.i("service", "timerStart-subTimer")
                // アラームの停止
                mediaPlayer.stop()
                mediaPlayer.prepare()

                // SubTimerをリセットしMainTimerを開始
                subTimer.reset()
                mainTimer.reset()
                mainTimer.start()
                timerRunning = true
                notificationUiChange(mainTimer.minute, mainTimer.second, "stop")
            }
            // MainTimer稼働時
            mainTimer.getTimerRunning() -> {
                Log.i("service", "timerStart-mainTimer")
                // MainTimerを停止
                mainTimer.stop()
                timerRunning = false
                changeTimerView(mainTimer.progressTime.value!!, "start")
            }
        }
    }

    // Timerをリセット
    fun timerReset() {
        if(mediaPlayer.isPlaying){
            // アラームの停止
            mediaPlayer.stop()
            mediaPlayer.prepare()
        }

        mainTimer.reset()
        subTimer.reset()
        timerRunning = false

        notificationUiChange(mainTimer.minute, mainTimer.second, "start")
    }

    // serviceからTimerを変更
    private fun changeTimer(){
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