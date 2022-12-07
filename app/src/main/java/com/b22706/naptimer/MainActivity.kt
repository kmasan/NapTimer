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
import android.widget.Toast
import com.b22706.naptimer.UriToFileUtil.toFile
import com.b22706.naptimer.databinding.ActivityMainBinding
import java.io.File

class MainActivity :
    AppCompatActivity(),
    NumPickerDialog.DialogListener,
    FileSelectionDialog.OnFileSelectListener,
    MyTimer.TimerListener
{
    private lateinit var binding:ActivityMainBinding
    private lateinit var fileSelector: FileSelector
    private lateinit var mService: TimerService
    private var mBound: Boolean = false
    private lateinit var mainTimer: MyTimer
    private lateinit var subTimer: MyTimer

    private var audioFile: File? = null
    private var mediaPlayer = MediaPlayer()
    private var mainTimerRunning = true

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
                startButtonText.observe(this@MainActivity){
                    binding.startButton.text = it
                }
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
        fileSelector = FileSelector(this,this)
        mainTimer = MyTimer(this, "main")
        subTimer = MyTimer(this, "sub")
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContentView(binding.root)

        subTimer.timerMinute = 1
        subTimer.timerSecond = 0
        changeTimerText(mainTimer.timerMinute*60*1000L)

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
            val dialog = NumPickerDialog.newInstance(mainTimer.timerMinute, mainTimer.timerSecond, "main")
            dialog.show(supportFragmentManager, "step")
        }

        binding.startButton.setOnClickListener {
            when {
                mainTimer.timerRunning -> {
                    mainTimer.stop()
                    binding.startButton.text = "再開"
                }
                subTimer.timerRunning -> {
                    mediaPlayer.stop()
                    mediaPlayer.prepare()
                    subTimer.reset()
                    mainTimer.start()
                    mainTimerRunning = true
                    binding.startButton.text = "一時停止"
                }
                else -> {
                    if(mainTimerRunning){
                        mainTimer.start()
                        binding.startButton.text = "一時停止"
                    }
                }
            }

            if(mBound) mService.timerStart()
        }
        binding.resetButton.setOnClickListener {
            mainTimer.reset()
            subTimer.reset()
            mainTimerRunning = true
            changeTimerText(mainTimer.timerMinute*60*1000L)
            binding.startButton.text = "スタート"

            if(mBound) mService.timerReset()
        }

        binding.fileSelectButton.setOnClickListener {
            fileSelector.selectFile()
        }

        mediaPlayer.setOnCompletionListener {
            if(!mediaPlayer.isLooping){
                mediaPlayer.stop()
                mediaPlayer.prepare()
            }
        }

        binding.button2.setOnClickListener {
            mService.notificationUiChange()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //Serviceと切断
        this.unbindService(serviceConnection)
    }

    override fun onDialogPositiveClick(minute: Int, second: Int, tag: String) {
        mainTimer.timerMinute = minute
        mainTimer.timerSecond = second
        changeTimerText((minute*60+second)*1000L)

        if(mBound) mService.apply {
            mainTimer.timerMinute = minute
            mainTimer.timerSecond = second
        }
    }

    private fun changeTimerText(time: Long){
        val minute = (time/(60*1000)).toInt()
        val second = ((time/1000)%60).toInt()
        binding.minuteText.text = String.format("%02d", minute)
        binding.secText.text = String.format("%02d", second)
    }

    override fun onTimerTick(time: Long, tag:String) {
        when(tag){
            "main" -> {
                changeTimerText(time)
            }
        }
    }

    override fun onTimerFinish(tag:String) {
        when(tag){
            "main" -> {
                mainTimerRunning = false
                mediaPlayer.start()
                mainTimer.reset()
                subTimer.start()
                binding.startButton.text = "アラーム停止"
            }
            "sub" -> {
                mainTimerRunning = true
                mediaPlayer.stop()
                mediaPlayer.prepare()
                subTimer.reset()
                mainTimer.start()
                binding.startButton.text = "一時停止"
            }
        }
    }

    override fun onFileSelect(file: File?) {
        Toast.makeText(
            this,
            "API:" + Build.VERSION.SDK_INT + " ファイルが選択されました。\n : " + file!!.path,
            Toast.LENGTH_LONG
        ).show()
        audioFile = file
        binding.fileNameText.text = file.name

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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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
        super.onActivityResult(requestCode, resultCode, data)
    }
}
