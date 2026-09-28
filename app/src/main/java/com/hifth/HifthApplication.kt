package com.hifth

import android.app.Application
import com.hifth.data.HifthDatabase
import com.hifth.data.QuranRepository

class HifthApplication : Application() {
    val repository: QuranRepository by lazy {
        QuranRepository(this, HifthDatabase.create(this).quranDao())
    }
}
