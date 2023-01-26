package com.b22706.naptimer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import java.io.File

class FileSelector(private val appContext: Context, private val listener: FileSelectionDialog.OnFileSelectListener) : Activity() {
    companion object{
        const val MENU_ID_FILE = 0     // オプションメニューID
    }
    private lateinit var m_strInitialDir: File       // 初期フォルダ

    // ファイルを選択するダイアログを表示する
    fun selectFile(fType: String) {
        //外部ストレージ　ルートフォルダパス取得
        val externalFilesDirs =
            appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.path.split("/")
        var externalFilesDirsPath = ""
        var i = 0
        externalFilesDirs.forEach { externalFilesDirsPeart ->
            if (i > 3) {
                return@forEach
            }
            if (externalFilesDirsPeart != "") {
                externalFilesDirsPath = "$externalFilesDirsPath/$externalFilesDirsPeart"
            }

            i++
        }

        //外部ストレージ　ダウンロードフォルダパスを初期フォルダとして変数に保存
        m_strInitialDir = File(externalFilesDirsPath)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // ファイル選択ダイアログ表示　Android 9.0　(API 28)　以下の場合の処理
            // アプリが minSdkVersion 26　なのでそれ以下の端末処置は考慮していない
            val dlg = FileSelectionDialog(appContext, listener)
            dlg.show(m_strInitialDir)
        } else {
            // ファイル選択Activity表示　Android 9.0　(API 28)　を超えるの場合の処理
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = fType
            }
            appContext.startActivity(intent)
        }
    }
}