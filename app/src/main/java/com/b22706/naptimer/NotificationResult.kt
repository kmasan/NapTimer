package com.b22706.naptimer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder

class NotificationResult: Activity() {
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
}