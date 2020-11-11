package social.tsu.trimmer.utils

import android.os.Environment
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File

object GifUtils {

    fun convertToGif(originalFile: File, callback: (String?) -> Unit) {


        val name = "tsuGif".plus(System.currentTimeMillis())

        val gifFIle = Environment
            .getExternalStoragePublicDirectory(
                Environment
                    .DIRECTORY_DOWNLOADS
            ).toString() + "/${name}gif.gif"

        if (!File(gifFIle).exists()) {
            File(gifFIle).createNewFile()
        }

        val cmd = "-v warning -ss 4 -t 2 -i ".plus(originalFile.path)
            .plus(" -vf scale=300:-1 -gifflags -transdiff -y ").plus(gifFIle)
        val result: Int = FFmpeg.execute(cmd)
        if (result == Config.RETURN_CODE_SUCCESS) {
            callback(gifFIle)
        } else if (result == Config.RETURN_CODE_CANCEL) {
            callback(null)
        }
    }
}