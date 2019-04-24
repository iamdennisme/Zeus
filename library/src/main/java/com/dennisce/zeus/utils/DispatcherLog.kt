package com.dennisce.zeus.utils

import android.util.Log
import com.dennisce.zeus.Zeus

object DispatcherLog {

    fun i(msg: String) {
        if (!Zeus.printLog) {
            return
        }
        Log.i("zeus_launch_starter", msg)
    }
}
