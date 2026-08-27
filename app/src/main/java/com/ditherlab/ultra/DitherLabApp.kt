package com.ditherlab.ultra

import android.app.Application

class DitherLabApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Bellek havuzları, native kütüphaneler vb. burada başlatılabilir.
    }
}
