package com.dennisce.zeus

import android.app.Application
import android.util.Log

/**
 * @program: HbbApp
 * @description:
 * @author:taicheng
 * @create: 19-4-24
 **/

class TestApplication : Application() {
    private val enableZeus = true

    override fun onCreate() {
        super.onCreate()
        if (enableZeus) {
            Zeus.init(this)
            Zeus.createInstance().run {
                addTask(TestTask())
                    .start()
                await()
            }
        } else {
            Thread.sleep(2000)
            Log.d("initTag", "init success")
        }
    }
}