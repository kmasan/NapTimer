package com.b22706.naptimer

import android.app.Application
import android.widget.RemoteViews

class Application: Application() {
    lateinit var notificationMiniLayout: RemoteViews

    override fun onCreate() {
        super.onCreate()
        notificationMiniLayout = RemoteViews(packageName, R.layout.notification_mini_layout)
    }
}