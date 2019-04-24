package com.dennisce.zeus

import android.util.Log
import com.dennisce.zeus.task.Task

/**
 * @program: HbbApp
 * @description:
 * @author:taicheng
 * @create: 19-4-24
 **/
class TestTask : Task() {

    override fun needWait(): Boolean {
        return true
    }

    override fun run() {
        Thread.sleep(2000)
        Log.d("initTag", "init success")
    }

}