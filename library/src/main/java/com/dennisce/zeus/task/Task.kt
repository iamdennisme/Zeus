package com.dennisce.zeus.task

import android.app.Application
import android.os.Process
import com.dennisce.zeus.Zeus
import com.dennisce.zeus.utils.DispatcherExecutor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService

abstract class Task : ITask {
    protected var mApplication: Application = Zeus.getApp()

    @Volatile
    var isWaiting: Boolean = false // 是否正在等待
    @Volatile
    var isRunning: Boolean = false // 是否正在执行
    @Volatile
    var isFinished: Boolean = false // Task是否执行完成
    @Volatile
    var isSend: Boolean = false // Task是否已经被分发
    private val mDepends = CountDownLatch(dependsOn().size) // 当前Task依赖的Task数量（需要等待被依赖的Task执行完毕才能执行自己），默认没有依赖

    override val tailRunnable: Runnable?
        get() = null

    /**
     * 当前Task等待，让依赖的Task先执行
     */
    fun waitToSatisfy() {
        try {
            mDepends.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * 依赖的Task执行完一个
     */
    fun satisfy() {
        mDepends.countDown()
    }

    /**
     * 是否需要尽快执行，解决特殊场景的问题：一个Task耗时非常多但是优先级却一般，很有可能开始的时间较晚，
     * 导致最后只是在等它，这种可以早开始。
     *
     * @return
     */
    fun needRunAsSoon(): Boolean {
        return false
    }

    /**
     * Task的优先级，运行在主线程则不要去改优先级
     *
     * @return
     */
    override fun priority(): Int {
        return Process.THREAD_PRIORITY_BACKGROUND
    }

    /**
     * Task执行在哪个线程池，默认在IO的线程池；
     * CPU 密集型的一定要切换到DispatcherExecutor.getCPUExecutor();
     *
     * @return
     */
    override fun runOn(): ExecutorService {
        return DispatcherExecutor.ioExecutor
    }

    /**
     * 异步线程执行的Task是否需要在被调用await的时候等待，默认不需要
     *
     * @return
     */
    override fun needWait(): Boolean {
        return false
    }

    /**
     * 当前Task依赖的Task集合（需要等待被依赖的Task执行完毕才能执行自己），默认没有依赖
     *
     * @return
     */
    override fun dependsOn(): List<Class<out Task>> {
        return listOf()
    }

    override fun runOnMainThread(): Boolean {
        return false
    }

    override fun setTaskCallBack(callBack: TaskCallBack) {}

    override fun needCall(): Boolean {
        return false
    }

    /**
     * 是否只在主进程，默认是
     *
     * @return
     */
    override fun onlyInMainProcess(): Boolean {
        return true
    }
}
