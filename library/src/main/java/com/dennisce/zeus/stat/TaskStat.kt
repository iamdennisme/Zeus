package com.dennisce.zeus.stat

import com.dennisce.zeus.Zeus
import com.dennisce.zeus.utils.DispatcherLog

import java.util.ArrayList
import java.util.concurrent.atomic.AtomicInteger

object TaskStat {

    @Volatile
    var currentSituation = ""
        set(currentSituation) {
            if (!Zeus.openLaunchStat) {
                return
            }
            DispatcherLog.i("currentSituation   $currentSituation")
            field = currentSituation
            setLaunchStat()
        }

    private val sBeans = ArrayList<TaskStatBean>()
    private var sTaskDoneCount = AtomicInteger()

    fun markTaskDone() {
        sTaskDoneCount.getAndIncrement()
    }

    private fun setLaunchStat() {
        val bean = TaskStatBean()
        bean.situation = currentSituation
        bean.count = sTaskDoneCount.get()
        sBeans.add(bean)
        sTaskDoneCount = AtomicInteger(0)
    }
}
