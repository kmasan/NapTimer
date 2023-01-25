package com.b22706.naptimer

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class NumPickerDialog:DialogFragment(),NumberPicker.OnValueChangeListener {
    private lateinit var listener: TimerChangeDialogListener
    private var mainMinuteItem: Int = 0
    private var mainSecondItem: Int = 0
    private var subMinuteItem: Int = 0
    private var subSecondItem: Int = 0

    companion object {
        private const val MAIN_MINUTE = "mainMinute"
        private const val MAIN_SECOND = "second"
        private const val SUB_MINUTE = "minute"
        private const val SUB_SECOND = "second"
        private const val TAG = "tag"
        fun newInstance(mainMinute: Int, mainSecond: Int, subMinute: Int, subSecond: Int): NumPickerDialog {
            val fragment = NumPickerDialog()
            val args = Bundle()
            args.putInt(MAIN_MINUTE, mainMinute)
            args.putInt(MAIN_SECOND, mainSecond)
            args.putInt(SUB_MINUTE, subMinute)
            args.putInt(SUB_SECOND, subSecond)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val mainMinute = arguments?.getInt(MAIN_MINUTE,0)
        val mainSecond = arguments?.getInt(MAIN_SECOND,10)
        val subMinute = arguments?.getInt(SUB_MINUTE,0)
        val subSecond = arguments?.getInt(SUB_SECOND,1)
        val tag = arguments?.getString(TAG,"")

        val inflater = activity?.layoutInflater
        val dialogView = inflater?.inflate(R.layout.num_picker_dialog, null)
        val dialogBuilder = AlertDialog.Builder(requireActivity())
            .setView(dialogView)
            .setTitle("")
            .setPositiveButton("決定") { _, _ ->
                dismiss()
                listener.onDialogPositiveClick(
                    listOf(this.mainMinuteItem, this.mainSecondItem, this.subMinuteItem, this.subSecondItem),
                    tag!!
                )
            }
            .setNegativeButton("キャンセル") { _, _ ->
                dismiss()
            }

        val npMainMinute = dialogView?.findViewById(R.id.mainMinutePicker) as NumberPicker
        npMainMinute.apply {
            setOnValueChangedListener(this@NumPickerDialog)
            minValue = 0 // NumberPickerの最小値設定
            maxValue = 60 // NumberPickerの最大値設定
            value = mainMinute!!  // NumberPickerの初期値
        }
        mainMinuteItem = mainMinute!!

        val npMainSecond = dialogView.findViewById(R.id.mainSecondPicker) as NumberPicker
        npMainSecond.apply {
            setOnValueChangedListener(this@NumPickerDialog)
            minValue = 0 // NumberPickerの最小値設定
            maxValue = 59 // NumberPickerの最大値設定
            value = mainSecond!!  // NumberPickerの初期値
        }
        mainSecondItem = mainSecond!!

        val npSubMinute = dialogView.findViewById(R.id.subMinutePicker) as NumberPicker
        npSubMinute.apply {
            setOnValueChangedListener(this@NumPickerDialog)
            minValue = 0 // NumberPickerの最小値設定
            maxValue = 60 // NumberPickerの最大値設定
            value = subMinute!!  // NumberPickerの初期値
        }
        mainMinuteItem = subMinute!!

        val npSubSecond = dialogView.findViewById(R.id.subSecondPicker) as NumberPicker
        npSubSecond.apply {
            setOnValueChangedListener(this@NumPickerDialog)
            minValue = 0 // NumberPickerの最小値設定
            maxValue = 59 // NumberPickerの最大値設定
            value = subSecond!!  // NumberPickerの初期値
        }
        mainSecondItem = subSecond!!
        return dialogBuilder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        when{
            context is TimerChangeDialogListener -> listener = context
            parentFragment is TimerChangeDialogListener -> listener = parentFragment as TimerChangeDialogListener
        }
    }

    override fun onValueChange(picker: NumberPicker?, oldVal: Int, newVal: Int) {
        when(picker!!.id){
            R.id.mainMinutePicker -> mainMinuteItem = newVal
            R.id.mainSecondPicker -> mainSecondItem = newVal
            R.id.subMinutePicker -> subMinuteItem = newVal
            R.id.subSecondPicker -> subSecondItem = newVal
        }
        //Log.d("numPicker",newVal.toString())
    }

    interface TimerChangeDialogListener {
        fun onDialogPositiveClick(times: List<Int>, tag: String)
        //fun onDialogNegativeClick(dialog: DialogFragment)
    }
}

