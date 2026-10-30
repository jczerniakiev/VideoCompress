package com.example.video_compress

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject
import java.io.File

class Utility(private val channelName: String) {

    fun isLandscapeImage(orientation: Int) = orientation != 90 && orientation != 270

    fun deleteFile(file: File) {
        if (file.exists()) {
            file.delete()
        }
    }

    fun timeStrToTimestamp(time: String): Long {
        val timeArr = time.split(":")
        val hour = Integer.parseInt(timeArr[0])
        val min = Integer.parseInt(timeArr[1])
        val secArr = timeArr[2].split(".")
        val sec = Integer.parseInt(secArr[0])
        val mSec = Integer.parseInt(secArr[1])

        val timeStamp = (hour * 3600 + min * 60 + sec) * 1000 + mSec
        return timeStamp.toLong()
    }

    fun getMediaInfoJson(context: Context, path: String): JSONObject {
        val file = File(path)
        val retriever = MediaMetadataRetriever()

        retriever.setDataSource(context, Uri.fromFile(file))

        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
        val author = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR) ?: ""
        val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val duration = java.lang.Long.parseLong(durationStr)
        var width = java.lang.Long.parseLong(widthStr)
        var height = java.lang.Long.parseLong(heightStr)
        val filesize = file.length()
        val orientation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        } else {
            null
        }
        val ori = orientation?.toIntOrNull()
        if (ori != null && isLandscapeImage(ori)) {
            val tmp = width
            width = height
            height = tmp
        }

        retriever.release()

        val json = JSONObject()

        json.put("path", path)
        json.put("title", title)
        json.put("author", author)
        json.put("width", width)
        json.put("height", height)
        json.put("duration", duration)
        json.put("filesize", filesize)
        if (ori != null) {
            json.put("orientation", ori)
        }

        return json
    }

    /**
     * The frame at [positionMs], at most 1920px on its longer side: enough for
     * a preview that fills a screen. A negative position is any frame the
     * retriever picks. Null when there is none, or the file cannot be read.
     */
    fun getBitmap(path: String, positionMs: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        var bitmap: Bitmap? = try {
            retriever.setDataSource(path)
            // The retriever works in microseconds, and the closest frame, not
            // the closest keyframe, is the one at the position.
            if (positionMs < 0) retriever.frameAtTime
            else retriever.getFrameAtTime(positionMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
        } catch (ex: RuntimeException) {
            // a corrupt video file
            null
        } finally {
            try {
                retriever.release()
            } catch (ex: RuntimeException) {
                // ignore failures while cleaning up
            }
        }

        val frame = bitmap ?: return null
        val max = Math.max(frame.width, frame.height)
        if (max > 1920) {
            val scale = 1920f / max
            bitmap = Bitmap.createScaledBitmap(frame, Math.round(scale * frame.width),
                    Math.round(scale * frame.height), true)
        }

        return bitmap
    }

    fun getFileNameWithGifExtension(path: String): String {
        val file = File(path)
        var fileName = ""
        val gifSuffix = "gif"
        val dotGifSuffix = ".$gifSuffix"

        if (file.exists()) {
            val name = file.name
            fileName = name.replaceAfterLast(".", gifSuffix)

            if (!fileName.endsWith(dotGifSuffix)) {
                fileName += dotGifSuffix
            }
        }
        return fileName
    }

    fun deleteAllCache(context: Context, result: MethodChannel.Result) {
        val dir = context.getExternalFilesDir("video_compress")
        result.success(dir?.deleteRecursively())
    }
}