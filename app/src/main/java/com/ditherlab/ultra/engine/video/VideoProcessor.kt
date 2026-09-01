package com.ditherlab.ultra.engine.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import io.microshow.rxffmpeg.RxFFmpegInvoke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger

class VideoProcessor(private val context: Context) {
    private val TAG = "VideoProcessor"

    /**
     * Processes a video frame by frame with memory and thread optimizations.
     */
    suspend fun processVideo(
        inputVideoPath: String,
        outputVideoPath: String,
        startTimeMs: Long = 0L,
        endTimeMs: Long = -1L,
        trimVideo: Boolean = false,
        maxDurationSeconds: Int = 30,
        maxConcurrency: Int = 4,
        onProgress: (Float) -> Unit,
        frameProcessor: suspend (Bitmap) -> Bitmap
    ): Boolean = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir
        val framesDir = File(cacheDir, "ditherlab_frames_${System.currentTimeMillis()}")
        if (!framesDir.exists()) framesDir.mkdirs()

        try {
            onProgress(0.05f)
            Log.d(TAG, "Extracting frames to ${framesDir.absolutePath}")

            // Capping max video frame resolution to 1080p for fast processing and low memory overhead
            val extractCmdList = mutableListOf(
                "ffmpeg",
                "-y"
            )
            
            val durationToExtract = if (trimVideo && endTimeMs != -1L) {
                ((endTimeMs - startTimeMs) / 1000f).coerceAtMost(maxDurationSeconds.toFloat())
            } else {
                maxDurationSeconds.toFloat()
            }
            
            if (trimVideo && startTimeMs > 0) {
                extractCmdList.add("-ss")
                extractCmdList.add((startTimeMs / 1000f).toString())
            }
            
            extractCmdList.addAll(listOf(
                "-t", durationToExtract.toString(),
                "-i", inputVideoPath,
                "-vf", "scale='min(1080,iw)':-2",
                "-qscale:v", "3",
                "-r", "30",
                "${framesDir.absolutePath}/frame_%05d.jpg"
            ))

            val extractCmdArray = extractCmdList.toTypedArray()
            
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

            val startFrameIndex = if (!trimVideo) (startTimeMs / 1000f * 30).toInt() else 0
            val endFrameIndex = if (!trimVideo && endTimeMs != -1L) (endTimeMs / 1000f * 30).toInt() else Int.MAX_VALUE

            // Controlled parallelism (max 4 concurrent frame tasks to prevent OOM, 1 for ML Kit)
            val processedCount = AtomicInteger(0)
            val parallelism = Runtime.getRuntime().availableProcessors().coerceIn(1, maxConcurrency)
            val dispatcher = Dispatchers.Default.limitedParallelism(parallelism)

            withContext(dispatcher) {
                frameFiles.map { file ->
                    async {
                        kotlinx.coroutines.yield()
                        val indexStr = file.nameWithoutExtension.substringAfter("frame_")
                        val frameIndex = indexStr.toIntOrNull() ?: 0
                        
                        val isEffectActive = frameIndex in startFrameIndex..endFrameIndex
                        
                        if (isEffectActive) {
                            val options = BitmapFactory.Options().apply {
                                inPreferredConfig = Bitmap.Config.ARGB_8888
                                inMutable = true
                            }
                            val originalBitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                            if (originalBitmap != null) {
                                try {
                                    val processedBitmap = frameProcessor(originalBitmap)
        
                                    FileOutputStream(file).use { out ->
                                        processedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                                    }
                                    if (processedBitmap != originalBitmap) {
                                        processedBitmap.recycle()
                                    }
                                } finally {
                                    originalBitmap.recycle()
                                }
                            }
                        }

                        val count = processedCount.incrementAndGet()
                        if (count % 20 == 0) {
                            System.gc() // Regular GC nudge for native bitmap buffers
                        }
                        if (count % 5 == 0 || count == totalFrames) {
                            val progress = 0.10f + (0.75f * (count.toFloat() / totalFrames))
                            onProgress(progress)
                        }
                    }
                }.awaitAll()
            }

            Log.d(TAG, "Frame processing complete. Reassembling video.")
            onProgress(0.88f)

            // Re-assembling with optimized bitrate and yuv420p pixel format
            val assembleCmdList = mutableListOf(
                "ffmpeg",
                "-framerate", "30",
                "-i", "${framesDir.absolutePath}/frame_%05d.jpg",
                "-t", maxDurationSeconds.toString()
            )
            
            if (trimVideo && startTimeMs > 0) {
                assembleCmdList.addAll(listOf("-ss", (startTimeMs / 1000f).toString()))
            }
            
            assembleCmdList.addAll(listOf(
                "-i", inputVideoPath,
                "-map", "0:v:0",
                "-map", "1:a:0?",
                "-c:v", "mpeg4",
                "-pix_fmt", "yuv420p",
                "-b:v", "10M",
                "-r", "30",
                "-c:a", "aac",
                "-b:a", "192k",
                "-y",
                outputVideoPath
            ))

            val assembleCmdArray = assembleCmdList.toTypedArray()
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
            framesDir.deleteRecursively()
        }
    }
}
