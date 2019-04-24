package com.dennisce.zeus

import android.app.Application
import android.os.Looper
import android.util.Log
import androidx.annotation.UiThread
import com.dennisce.zeus.sort.TaskSortUtil
import com.dennisce.zeus.stat.TaskStat
import com.dennisce.zeus.task.DispatchRunnable
import com.dennisce.zeus.task.Task
import com.dennisce.zeus.task.TaskCallBack
import com.dennisce.zeus.utils.DispatcherLog
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 启动器调用类
 */

class Zeus private constructor() {

    private var mStartTime: Long = 0
    private val mFutures = ArrayList<Future<*>>()
    private var mAllTasks: MutableList<Task> = ArrayList()
    private val mClsAllTasks = ArrayList<Class<out Task>>()
    private var mMainThreadTasks = ArrayList<Task>()
    private var mCountDownLatch: CountDownLatch? = null
    private val mNeedWaitCount = AtomicInteger() // 保存需要Wait的Task的数量
    private val mNeedWaitTasks = ArrayList<Task>() // 调用了await的时候还没结束的且需要等待的Task
    private var mFinishedTasks = ArrayList<Class<out Task>>(100) // 已经结束了的Task
    private val mDependedHashMap = HashMap<Class<out Task>, ArrayList<Task>>()
    private val mAnalyseCount = AtomicInteger() // 启动器分析的次数，统计下分析的耗时；

    fun addTask(task: Task?): Zeus {
        if (task != null) {
            collectDepends(task)
            mAllTasks.add(task)
            mClsAllTasks.add(task.javaClass)
            // 非主线程且需要wait的，主线程不需要CountDownLatch也是同步的
            if (ifNeedWait(task)) {
                mNeedWaitTasks.add(task)
                mNeedWaitCount.getAndIncrement()
            }
        }
        return this
    }

    private fun collectDepends(task: Task) {
        if (task.dependsOn().isNotEmpty()) {
            for (cls in task.dependsOn()) {
                if (mDependedHashMap[cls] == null) {
                    mDependedHashMap[cls] = ArrayList()
                }
                mDependedHashMap[cls]!!.add(task)
                if (mFinishedTasks.contains(cls)) {
                    task.satisfy()
                }
            }
        }
    }

    private fun ifNeedWait(task: Task): Boolean {
        return !task.runOnMainThread() && task.needWait()
    }

    @UiThread
    fun start() {
        mStartTime = System.currentTimeMillis()
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw RuntimeException("must be called from UiThread")
        }
        if (mAllTasks.size > 0) {
            mAnalyseCount.getAndIncrement()
            printDependedMsg()
            mAllTasks = TaskSortUtil.getSortResult(mAllTasks, mClsAllTasks)
            mCountDownLatch = CountDownLatch(mNeedWaitCount.get())

            sendAndExecuteAsyncTasks()

            DispatcherLog.i("task analyse cost " + (System.currentTimeMillis() - mStartTime) + "  begin main ")
            executeTaskMain()
        }
        DispatcherLog.i("task analyse cost startTime cost " + (System.currentTimeMillis() - mStartTime))
    }

    fun cancel() {
        for (future in mFutures) {
            future.cancel(true)
        }
    }

    private fun executeTaskMain() {
        mStartTime = System.currentTimeMillis()
        for (task in mMainThreadTasks) {
            val time = System.currentTimeMillis()
            DispatchRunnable(task, this).run()
            DispatcherLog.i(
                "real main " + task.javaClass.simpleName + " cost   " +
                        (System.currentTimeMillis() - time)
            )
        }
        DispatcherLog.i("maintask cost " + (System.currentTimeMillis() - mStartTime))
    }

    private fun sendAndExecuteAsyncTasks() {
        for (task in mAllTasks) {
            if (task.onlyInMainProcess() && !isMainProcess) {
                markTaskDone(task)
            } else {
                sendTaskReal(task)
            }
            task.isSend = true
        }
    }

    /**
     * 查看被依赖的信息
     */
    private fun printDependedMsg() {
        DispatcherLog.i("needWait size : " + mNeedWaitCount.get())
        if (printDependedMsg) {
            for (cls in mDependedHashMap.keys) {
                DispatcherLog.i("cls " + cls.simpleName + "   " + mDependedHashMap[cls]!!.size)
                for (task in mDependedHashMap[cls]!!) {
                    DispatcherLog.i("cls       " + task.javaClass.simpleName)
                }
            }
        }
    }

    /**
     * 通知Children一个前置任务已完成
     *
     * @param launchTask
     */
    fun satisfyChildren(launchTask: Task) {
        val arrayList = mDependedHashMap[launchTask.javaClass]
        if (arrayList != null && arrayList.size > 0) {
            for (task in arrayList) {
                task.satisfy()
            }
        }
    }

    fun markTaskDone(task: Task) {
        if (ifNeedWait(task)) {
            mFinishedTasks.add(task.javaClass)
            mNeedWaitTasks.remove(task)
            mCountDownLatch!!.countDown()
            mNeedWaitCount.getAndDecrement()
        }
    }

    private fun sendTaskReal(task: Task) {
        if (task.runOnMainThread()) {
            mMainThreadTasks.add(task)

            if (task.needCall()) {
                task.setTaskCallBack(object : TaskCallBack {
                    override fun call() {
                        TaskStat.markTaskDone()
                        task.isFinished = true
                        satisfyChildren(task)
                        markTaskDone(task)
                        DispatcherLog.i(task.javaClass.simpleName + " finish")

                        Log.i("testLog", "call")
                    }
                })
            }
        } else {
            // 直接发，是否执行取决于具体线程池
            val future = task.runOn().submit(DispatchRunnable(task, this))
            mFutures.add(future)
        }
    }

    fun executeTask(task: Task) {
        if (ifNeedWait(task)) {
            mNeedWaitCount.getAndIncrement()
        }
        task.runOn().execute(DispatchRunnable(task, this))
    }

    @UiThread
    fun await() {
        try {
            DispatcherLog.i("still has " + mNeedWaitCount.get())
            for (task in mNeedWaitTasks) {
                DispatcherLog.i("needWait: " + task.javaClass.simpleName)
            }

            if (mNeedWaitCount.get() > 0) {
                mCountDownLatch!!.await(WAIT_TIME.toLong(), TimeUnit.MILLISECONDS)
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val WAIT_TIME = 10
        var printAllTaskName = false // 打印所有taskname
        var openLaunchStat = false // 统计
        var printLog = false // 打开日志
        var printDependedMsg = false // 打开日志

        private lateinit var context: Application

        var isMainProcess: Boolean = false
            private set

        @Volatile
        private var sHasInit: Boolean = false

        fun getApp(): Application {
            if (Companion::context.isInitialized) {
                return context
            }
            throw Throwable("you must init zeus first")
        }

        fun init(application: Application?) {
            if (application != null) {
                context = application
                sHasInit = true
                isMainProcess = Looper.getMainLooper() == Looper.myLooper()
            }
        }

        /**
         * 注意：每次获取的都是新对象
         *
         * @return
         */
        fun createInstance(): Zeus {
            if (!sHasInit) {
                throw RuntimeException("must call TaskDispatcher.init first")
            }
            return Zeus()
        }
    }
}
