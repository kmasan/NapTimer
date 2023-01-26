package com.b22706.naptimer

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import com.b22706.naptimer.UriToFileUtil.toFile
import com.b22706.naptimer.databinding.ActivityMainBinding
import java.io.File

class MainActivity :
    AppCompatActivity(),
    NumPickerDialog.TimerChangeDialogListener,
    FileSelectionDialog.OnFileSelectListener
{
    private lateinit var binding:ActivityMainBinding
    private lateinit var fileSelector: FileSelector
    private lateinit var mService: TimerService
    private var mBound: Boolean = false

    private var audioFile: File? = null
    private var mediaPlayer = MediaPlayer()

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        @SuppressLint("SetTextI18n")
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // サービスとの接続確立時に呼び出される
            // サービスにはIBinder経由で#getService()してダイレクトにアクセス可能
            //必要であればmServiceを使ってバインドしたサービスへの制御を行う
            val binder = service as TimerService.ServiceBinder
            mService = binder.getService()
            mBound = true

            mService.apply {
                mainTimer.progressTime.observe(this@MainActivity){
                    changeTimerText(it)
                }
                subTimer.progressTime.observe(this@MainActivity){

                }
            }
            binding.fileNameText.text = "binding True"
        }

        override fun onServiceDisconnected(className: ComponentName) {
            // サービスとの切断(異常系処理)
            // プロセスのクラッシュなど意図しないサービスの切断が発生した場合に呼ばれる。
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fileSelector = FileSelector(this,this)
        volumeControlStream = AudioManager.STREAM_MUSIC

        // Serviceの開始
        val intent = Intent(this, TimerService::class.java).apply {
            //flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("REQUEST_CODE", 1)
        }
        startForegroundService(intent)

        //Serviceと接続
        Intent(this, TimerService::class.java).also {
            this.bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        binding.changeMinuteButton.setOnClickListener {
            if (!mBound) return@setOnClickListener
            val dialog = NumPickerDialog.newInstance(
                mService.mainTimer.minute,
                mService.mainTimer.second,
                mService.subTimer.minute,
                mService.subTimer.second
            )
            dialog.show(supportFragmentManager, "step")
        }

        binding.startButton.setOnClickListener {
//            when {
//                mainTimer.getTimerRunning() -> {
//                    mainTimer.stop()
//                    binding.startButton.text = "再開"
//                }
//                subTimer.getTimerRunning() -> {
//                    mediaPlayer.stop()
//                    mediaPlayer.prepare()
//                    subTimer.reset()
//                    mainTimer.start()
//                    mainTimerRunning = true
//                    binding.startButton.text = "一時停止"
//                }
//                else -> {
//                    if(mainTimerRunning){
//                        mainTimer.start()
//                        binding.startButton.text = "一時停止"
//                    }
//                }
//            }

            if(mBound) {
                binding.startButton.text =when{
                    mService.mainTimer.getTimerRunning()->"再開"
                    else->"一時停止"
                }
                mService.timerStart()
            }
        }
        binding.resetButton.setOnClickListener {
//            mainTimer.reset()
//            subTimer.reset()
//            mainTimerRunning = true
//            changeTimerText(mainTimer.minute*60*1000L)
            binding.startButton.text = "スタート"

            if(mBound) mService.timerReset()
        }

        binding.fileSelectButton.setOnClickListener {
            //fileSelector.selectFile("*/*")
            val fileSelectorIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/*"
            }
            startActivityForResult(fileSelectorIntent, 0)
        }

        mediaPlayer.setOnCompletionListener {
            if(!mediaPlayer.isLooping){
                mediaPlayer.stop()
                mediaPlayer.prepare()
            }
        }

        binding.button2.setOnClickListener {
            //mService.notificationUiChange()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //Serviceと切断
        this.unbindService(serviceConnection)
    }

    override fun onDialogPositiveClick(times: List<Int>, tag: String) {
//        mainTimer.minute = times[0]
//        mainTimer.second = times[1]
//        subTimer.minute = times[2]
//        subTimer.second = times[3]
//        changeTimerText((mainTimer.minute*60+ mainTimer.second)*1000L)

        if(mBound) mService.changeTimer(times)
    }

    private fun changeTimerText(time: Long){
        val minute = (time/(60*1000)).toInt()
        val second = ((time/1000)%60).toInt()
        binding.minuteText.text = String.format("%02d", minute)
        binding.secText.text = String.format("%02d", second)
    }

//    override fun onTimerFinish(tag:String) {
//        when(tag){
//            "main" -> {
////                mainTimerRunning = false
//                mediaPlayer.start()
////                mainTimer.reset()
////                subTimer.start()
//                binding.startButton.text = "アラーム停止"
//            }
//            "sub" -> {
////                mainTimerRunning = true
//                mediaPlayer.stop()
//                mediaPlayer.prepare()
////                subTimer.reset()
////                mainTimer.start()
//                binding.startButton.text = "一時停止"
//            }
//        }
//    }

    override fun onFileSelect(file: File?) {
        Log.i("MainActivity","onFileSelect")
        Toast.makeText(
            this,
            "API:" + Build.VERSION.SDK_INT + " ファイルが選択されました。\n : " + file!!.path,
            Toast.LENGTH_LONG
        ).show()
        audioFile = file
        binding.fileNameText.text = file.name

        if(mediaPlayer.isPlaying){
            // アラームの停止
            mediaPlayer.stop()
            mediaPlayer.reset()
        }
        mediaPlayer.setDataSource(file.path)
        mediaPlayer.prepare()
        mediaPlayer.isLooping = true

        if(mBound){
            mService.apply {
                audioFile = file
                if(mediaPlayer.isPlaying){
                    // アラームの停止
                    mediaPlayer.stop()
                    mediaPlayer.reset()
                }
                mediaPlayer.setDataSource(file.path)
                mediaPlayer.prepare()
                mediaPlayer.isLooping = true
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i("MainActivity","onActivityResult")
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                FileSelector.MENU_ID_FILE -> {
                    // ファイル名を取得
                    var selectFileName = "ファイル名を取得できませんでした"
                    // ファイルURIを取得
                    val uri: Uri = data?.data!!
                    // ファイルURIをファイルパスに変換
                    val file: File = toFile(this, uri)
                    audioFile = file
                    data.data?.let { selectFileUri ->
                        contentResolver.query(selectFileUri, null, null, null, null)
                    }?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        //val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        cursor.moveToFirst()
                        selectFileName = cursor.getString(nameIndex)
                    }

                    // トーストの表示
                    Toast.makeText(
                        this,
                        "API:" + Build.VERSION.SDK_INT + " ファイルが選択されました。\n : " + selectFileName + ": ${file.name}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.fileNameText.text = selectFileName

                    mediaPlayer.apply {
                        stop()
                        reset()
                    }
                    mediaPlayer.setDataSource(file.path)
                    mediaPlayer.prepare()
                    mediaPlayer.isLooping = true

                    if(mBound){
                        mService.apply {
                            audioFile = file
                            mediaPlayer.apply {
                                stop()
                                reset()
                            }
                            mediaPlayer.setDataSource(file.path)
                            mediaPlayer.prepare()
                            mediaPlayer.isLooping = true
                        }
                    }
                }
                else -> {/*何もしない*/
                }
            }
        }
    }
}
