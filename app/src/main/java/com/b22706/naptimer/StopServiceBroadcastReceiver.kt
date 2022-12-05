package com.b22706.naptimer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.*
import android.os.IBinder
import android.util.Log

class StopServiceBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val targetIntent = Intent(context, TimerService::class.java)
        context.stopService(targetIntent)
    }
}

class StartTimerBroadcastReceiver : BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("StartTimerBroadcastReceiver","onReceive")
        val targetIntent = Intent(context, TimerService::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra("tag", "start")
            type = "text/plain"
        }
        //context.startService(Intent.createChooser(targetIntent,null))
        context.startForegroundService(targetIntent)
    }
}

class ResetTimerBroadcastReceiver: BroadcastReceiver(){
    private lateinit var mService: TimerService
    private var mBound: Boolean = false
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        @SuppressLint("SetTextI18n")
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // サービスとの接続確立時に呼び出される
            // サービスにはIBinder経由で#getService()してダイレクトにアクセス可能
            //必要であればmServiceを使ってバインドしたサービスへの制御を行う
            val binder = service as TimerService.ServiceBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            // サービスとの切断(異常系処理)
            // プロセスのクラッシュなど意図しないサービスの切断が発生した場合に呼ばれる。
            mBound = false
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val targetIntent = Intent(context, TimerService::class.java).also {
            context.bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }


        if(mBound) mService.timerStart()
    }
}

interface BroadcastReceiverListener{
    fun receiverListener(tag: String)
}