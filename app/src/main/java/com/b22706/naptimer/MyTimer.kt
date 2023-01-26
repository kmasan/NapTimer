package com.b22706.naptimer

import android.os.CountDownTimer
import androidx.lifecycle.MutableLiveData

class MyTimer(val listener: TimerListener, val tag: String) {
    private lateinit var timer: CountDownTimer
    private var timerRunning = false
    fun getTimerRunning() = timerRunning

    var minute = 15
    var second = 0
    val progressTime = MutableLiveData((minute*60+second)*1000L)

    fun start(){
        if(timerRunning) return
        timer = object : CountDownTimer(progressTime.value!!, 1000){
            override fun onTick(time: Long) {
                progressTime.postValue(time)
                listener.onTimerTick(time, tag)
            }

            override fun onFinish() {
                listener.onTimerFinish(tag)
                //timerRunning = false
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
        progressTime.postValue((minute*60+second)*1000L)
    }

    fun change(min: Int, sec:Int){
        minute = min
        second = sec
        progressTime.postValue((minute*60+second)*1000L)
    }

    interface TimerListener{
        fun onTimerTick(time: Long, tag: String)
        fun onTimerFinish(tag: String)
    }
}