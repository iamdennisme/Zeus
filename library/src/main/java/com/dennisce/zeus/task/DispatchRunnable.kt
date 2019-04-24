package com.dennisce.zeus.task

import android.os.Looper
import android.os.Process
import androidx.core.os.TraceCompat
import com.dennisce.zeus.Zeus
import com.dennisce.zeus.stat.TaskStat
import com.dennisce.zeus.utils.DispatcherLog

/**
 * 任务真正执行的地方
 */

class DispatchRunnable(val task: Task, private val zeus: Zeus? = null) : Runnable {

    override fun run() {
        TraceCompat.beginSection(task.javaClass.simpleName)
        DispatcherLog.i(
            task.javaClass.simpleName +
                    " begin run" + "  Situation  " + TaskStat.currentSituation
        )

        Process.setThreadPriority(task.priority())

        var startTime = System.currentTimeMillis()

        task.isWaiting = true
        task.waitToSatisfy()

        val waitTime = System.currentTimeMillis() - startTime
        startTime = System.currentTimeMillis()

        // 执行Task
        task.isRunning = true
        task.run()

        // 执行Task的尾部任务
        val tailRunnable = task.tailRunnable
        tailRunnable?.run()

        if (!task.needCall() || !task.runOnMainThread()) {
            printTaskLog(startTime, waitTime)

            TaskStat.markTaskDone()
            task.isFinished = true
            if (zeus != null) {
                zeus.satisfyChildren(task)
                zeus.markTaskDone(task)
            }
            DispatcherLog.i(task.javaClass.simpleName + " finish")
        }
        TraceCompat.endSection()
    }

    /**
     * 打印出来Task执行的日志
     *
     * @param startTime
     * @param waitTime
     */
    private fun printTaskLog(startTime: Long, waitTime: Long) {
        val runTime = System.currentTimeMillis() - startTime
            DispatcherLog.i(
                task.javaClass.simpleName + "  wait " + waitTime + "    run " +
                        runTime + "   isMain " + (Looper.getMainLooper() == Looper.myLooper()) +
                        "  needWait " + (task.needWait() || Looper.getMainLooper() == Looper.myLooper()) +
                        "  ThreadId " + Thread.currentThread().id +
                        "  ThreadName " + Thread.currentThread().name +
                        "  Situation  " + TaskStat.currentSituation
            )
    }
}
