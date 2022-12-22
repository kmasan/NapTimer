package com.b22706.naptimer

import android.os.CountDownTimer
import androidx.lifecycle.MutableLiveData

class MyTimer(val listener: TimerListener, val tag: String) {
    private lateinit var timer: CountDownTimer
    private var timerRunning = false
    fun getTimerRunning() = timerRunning

    var timerMinute = 15
    var timerSecond = 0
    var progressTime = (timerMinute*60+timerSecond)*1000L
    val progressTimeL = MutableLiveData((timerMinute*60+timerSecond)*1000L)

    fun start(){
        if(timerRunning) return
        timer = object : CountDownTimer(progressTime, 1000){
            override fun onTick(time: Long) {
                progressTime = time
                progressTimeL.postValue(time)
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
        progressTime = (timerMinute*60+timerSecond)*1000L
        progressTimeL.postValue((timerMinute*60+timerSecond)*1000L)
    }

    fun change(min: Int, sec:Int){
        timerMinute = min
        timerSecond = sec
        progressTime = (timerMinute*60+timerSecond)*1000L
        progressTimeL.postValue((timerMinute*60+timerSecond)*1000L)
    }

    interface TimerListener{
        fun onTimerTick(time: Long, tag: String)
        fun onTimerFinish(tag: String)
    }
}