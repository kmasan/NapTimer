package com.b22706.naptimer

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class NumPickerDialog:DialogFragment(),NumberPicker.OnValueChangeListener {
    private lateinit var listener: DialogListener
    private var minuteItem: Int = 0
    private var secondItem: Int = 0

    companion object {
        private const val MINUTE = "minute"
        private const val SECOND = "second"
        private const val TAG = "tag"
        fun newInstance(minute: Int, second: Int, tag: String): NumPickerDialog {
            val fragment = NumPickerDialog()
            val args = Bundle()
            args.putInt(MINUTE, minute)
            args.putInt(SECOND, second)
            args.putString(TAG, tag)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val minute = arguments?.getInt(MINUTE,0)
        val second = arguments?.getInt(SECOND,10)
        val tag = arguments?.getString(TAG,"")

        val inflater = activity?.layoutInflater
        val dialogView = inflater?.inflate(R.layout.num_picker_dialog, null)
        val dialogBuilder = AlertDialog.Builder(requireActivity())
            .setView(dialogView)
            .setTitle("")
            .setPositiveButton("決定") { _, _ ->
                dismiss()
                listener.onDialogPositiveClick(this.minuteItem, this.secondItem, tag!!)
            }
            .setNegativeButton("キャンセル") { _, _ ->
                dismiss()
            }

        val npMinute = dialogView?.findViewById(R.id.minutePicker) as NumberPicker
        npMinute.setOnValueChangedListener(this)
        npMinute.minValue = 0 // NumberPickerの最小値設定
        npMinute.maxValue = 60 // NumberPickerの最大値設定
        npMinute.value = minute!!  // NumberPickerの初期値
        minuteItem = minute

        val npSecond = dialogView.findViewById(R.id.secondPicker) as NumberPicker
        npSecond.setOnValueChangedListener(this)
        npSecond.minValue = 0 // NumberPickerの最小値設定
        npSecond.maxValue = 59 // NumberPickerの最大値設定
        npSecond.value = second!!  // NumberPickerの初期値
        secondItem = second
        return dialogBuilder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        when{
            context is DialogListener -> listener = context
            parentFragment is DialogListener -> listener = parentFragment as DialogListener
        }
    }

    override fun onValueChange(picker: NumberPicker?, oldVal: Int, newVal: Int) {
        when(picker!!.id){
            R.id.minutePicker -> minuteItem = newVal
            R.id.secondPicker -> secondItem = newVal
        }
        //Log.d("numPicker",newVal.toString())
    }

    interface DialogListener {
        fun onDialogPositiveClick(minute:Int, second: Int, tag: String)
        //fun onDialogNegativeClick(dialog: DialogFragment)
    }
}

