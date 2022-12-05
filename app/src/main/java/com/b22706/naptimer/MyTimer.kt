package com.b22706.naptimer

import android.os.CountDownTimer
import androidx.lifecycle.MutableLiveData

class MyTimer(val listener: TimerListener, val tag: String) {
    private lateinit var timer: CountDownTimer
    val progressTime = MutableLiveData(15 * 60000L)

    var timerMinute = 15
    var timerSecond = 0
    var timerRunning = false

    fun start(){
        if(timerRunning) return
        timer = object : CountDownTimer((timerMinute*60+timerSecond)*1000L, 1000){
            override fun onTick(time: Long) {
                progressTime.postValue(time)
                listener.onTimerTick(time, tag)
            }

            override fun onFinish() {
                listener.onTimerFinish(tag)
                timerRunning = false
            }
        }.start()
        timerRunning = true
    }

    fun stop(){
        if(!timerRunning) return
        timer.cancel()
        timerRunning = false
    }

    fun reset(){
        if(timerRunning){
            timer.cancel()
            timerRunning = false
        }
        progressTime.postValue(timerMinute*60000L)
    }

    interface TimerListener{
        fun onTimerTick(time: Long, tag: String)
        fun onTimerFinish(tag: String)
    }
}