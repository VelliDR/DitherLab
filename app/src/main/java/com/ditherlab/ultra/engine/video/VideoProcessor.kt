package com.ditherlab.ultra.engine.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import io.microshow.rxffmpeg.RxFFmpegInvoke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.io.File
import java.io.FileOutputStream

class VideoProcessor(private val context: Context) {
    private val TAG = "VideoProcessor"

    /**
     * Processes a video frame by frame.
     * @param inputVideoPath The path to the source video.
     * @param outputVideoPath The path where the processed video will be saved.
     * @param maxDurationSeconds Limit the processing to the first N seconds.
     * @param onProgress Callback for progress updates (0.0 to 1.0).
     * @param frameProcessor Suspend function to process each frame's Bitmap.
     * @return true if successful, false otherwise.
     */
    suspend fun processVideo(
        inputVideoPath: String,
        outputVideoPath: String,
        maxDurationSeconds: Int = 30,
        onProgress: (Float) -> Unit,
        frameProcessor: suspend (Bitmap) -> Bitmap
    ): Boolean = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir
        val framesDir = File(cacheDir, "ditherlab_frames_${System.currentTimeMillis()}")
        if (!framesDir.exists()) framesDir.mkdirs()

        try {
            onProgress(0.05f)
            Log.d(TAG, "Extracting frames to ${framesDir.absolutePath}")
            
            val extractCmdArray = arrayOf(
                "ffmpeg",
                "-t", maxDurationSeconds.toString(),
                "-i", inputVideoPath,
                "-qscale:v", "2",
                "-r", "30",
                "${framesDir.absolutePath}/frame_%05d.jpg"
            )
            
            RxFFmpegInvoke.getInstance().setDebug(true)
            
            val extractResult = RxFFmpegInvoke.getInstance().runCommand(extractCmdArray, object : io.microshow.rxffmpeg.RxFFmpegInvoke.IFFmpegListener {
                override fun onFinish() {}
                override fun onProgress(progress: Int, pts: Long) {}
                override fun onCancel() {}
                override fun onError(message: String?) { Log.e(TAG, "extract error: $message") }
            })
            if (extractResult != 0) {
                Log.e(TAG, "Frame extraction failed with code $extractResult")
                return@withContext false
            }

            val frameFiles = framesDir.listFiles()?.filter { it.name.startsWith("frame_") && it.extension == "jpg" }
                ?.sortedBy { it.name } ?: emptyList()

            if (frameFiles.isEmpty()) {
                Log.e(TAG, "No frames extracted.")
                return@withContext false
            }

            val totalFrames = frameFiles.size
            Log.d(TAG, "Extracted $totalFrames frames. Starting processing.")

            // 2. Process each frame concurrently
            val processedCount = java.util.concurrent.atomic.AtomicInteger(0)
            val parallelism = Runtime.getRuntime().availableProcessors().coerceIn(2, 6)
            val dispatcher = kotlinx.coroutines.Dispatchers.Default.limitedParallelism(parallelism)
            
            kotlinx.coroutines.withContext(dispatcher) {
                frameFiles.map { file ->
                    async {
                        val originalBitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        if (originalBitmap != null) {
                            val processedBitmap = frameProcessor(originalBitmap)
                            
                            java.io.FileOutputStream(file).use { out ->
                                processedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
                            }
                            if (processedBitmap != originalBitmap) {
                                processedBitmap.recycle()
                            }
                            originalBitmap.recycle()
                        }
                        
                        val count = processedCount.incrementAndGet()
                        if (count % 5 == 0 || count == totalFrames) {
                            val progress = 0.1f + (0.7f * (count.toFloat() / totalFrames))
                            onProgress(progress)
                        }
                    }
                }.awaitAll()
            }

            Log.d(TAG, "Frame processing complete. Reassembling video.")
            onProgress(0.85f)

            val assembleCmdArray = arrayOf(
                "ffmpeg",
                "-framerate", "30",
                "-i", "${framesDir.absolutePath}/frame_%05d.jpg",
                "-t", maxDurationSeconds.toString(),
                "-i", inputVideoPath,
                "-map", "0:v:0",
                "-map", "1:a:0?",
                "-c:v", "mpeg4",
                "-q:v", "2",
                "-r", "30",
                "-c:a", "aac",
                "-b:a", "192k",
                "-y",
                outputVideoPath
            )
            
            val assembleResult = RxFFmpegInvoke.getInstance().runCommand(assembleCmdArray, object : io.microshow.rxffmpeg.RxFFmpegInvoke.IFFmpegListener {
                override fun onFinish() {}
                override fun onProgress(progress: Int, pts: Long) {}
                override fun onCancel() {}
                override fun onError(message: String?) { Log.e(TAG, "assemble error: $message") }
            })
            
            if (assembleResult != 0) {
                Log.e(TAG, "Video assembly failed with code $assembleResult")
                return@withContext false
            }

            onProgress(1.0f)
            Log.d(TAG, "Video processing successful: $outputVideoPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error processing video", e)
            false
        } finally {
            // Clean up temporary frames
            framesDir.deleteRecursively()
        }
    }
}
