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

class TimerBroadcastReceiver : BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TimerBroadcastReceiver","onReceive")
        val targetIntent = Intent(context, TimerService::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra("tag", "timer")
            type = "text/plain"
        }
        context.startForegroundService(targetIntent)
    }
}

class StopTimerBroadcastReceiver : BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("StopTimerBroadcastReceiver","onReceive")
        val targetIntent = Intent(context, TimerService::class.java).apply {
            action = Intent.ACTION_SEND
            putExtra("tag", "stop")
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

class ChangeTimeBroadcastReceiver: BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ChangeTimeBroadcastReceiver","onReceive")
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