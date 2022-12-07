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
        context.startForegroundService(targetIntent)
    }
}

class ResetTimerBroadcastReceiver: BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ResetTimerBroadcastReceiver","onReceive")
        val targetIntent = Intent(context, TimerService::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra("tag", "reset")
            type = "text/plain"
        }
        context.startForegroundService(targetIntent)
    }
}

class ChangeTimerBroadcastReceiver: BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ChangeTimerBroadcastReceiver","onReceive")
        val targetIntent = Intent(context, TimerService::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra("tag", "change")
            putExtra("minute", "change")
            putExtra("second", "change")
            type = "text/plain"
        }
        context.startForegroundService(targetIntent)
    }
}

interface BroadcastReceiverListener{
    fun receiverListener(tag: String)
}