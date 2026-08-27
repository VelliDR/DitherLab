package com.ditherlab.ultra.core

import android.hardware.HardwareBuffer
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Sıfır gecikmeli (zero-copy) grafik işlemleri için API 33+ destekli 
 * HardwareBuffer nesnelerini yeniden kullanan (pool) bellek yöneticisi.
 */
@RequiresApi(Build.VERSION_CODES.O)
object HardwareBufferPool {
    private val pool = ConcurrentLinkedQueue<HardwareBuffer>()
    private const val MAX_POOL_SIZE = 5

    fun acquire(width: Int, height: Int, format: Int): HardwareBuffer {
        // Havuzda uygun boyutta ve formatta bir buffer varsa onu al
        val iterator = pool.iterator()
        while (iterator.hasNext()) {
            val buffer = iterator.next()
            if (buffer.width == width && buffer.height == height && buffer.format == format) {
                iterator.remove()
                return buffer
            }
        }
        
        // Yoksa yeni oluştur
        return HardwareBuffer.create(
            width, height, format, 1, 
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        )
    }

    fun release(buffer: HardwareBuffer) {
        if (pool.size < MAX_POOL_SIZE) {
            pool.offer(buffer)
        } else {
            buffer.close()
        }
    }
    
    fun clear() {
        while (pool.isNotEmpty()) {
            pool.poll()?.close()
        }
    }
}
