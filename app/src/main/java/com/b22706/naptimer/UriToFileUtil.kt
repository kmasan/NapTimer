package com.b22706.naptimer

import android.content.Context
import android.net.Uri
import com.b22706.naptimer.RealPathUtil.getRealPath
import java.io.File


object UriToFileUtil {
    fun toFile(context: Context, uri: Uri): File {
        val realPath: String = getRealPath(context, uri)
        return File(realPath)
    }
}