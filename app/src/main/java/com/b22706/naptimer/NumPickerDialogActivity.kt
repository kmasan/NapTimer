package com.b22706.naptimer


import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity


class NumPickerDialogActivity: AppCompatActivity(),
    NumPickerDialog.TimerChangeDialogListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("dialogActivity", "onCreate")
        //val dialog = NumPickerDialog.newInstance(mainTimer.timerMinute, mainTimer.timerSecond, "main")
        val dialog = NumPickerDialog.newInstance(0, 0, 0,0)
        dialog.show(supportFragmentManager, "step")
        val alertDialog = AlertDialog.Builder(this).apply {
            setTitle("title")
            setMessage("message")
            setPositiveButton("OK") { _, _ ->
                this@NumPickerDialogActivity.finish() //選択をしたら自信のActivityを終了させる
            }
        }.create()
        //alertDialog.show()
    }

    override fun onDialogPositiveClick(times: List<Int>, tag: String) {
        TODO("Not yet implemented")
    }
}