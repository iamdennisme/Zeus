package com.dennisce.zeus.sort

import androidx.collection.ArraySet
import com.dennisce.zeus.Zeus
import com.dennisce.zeus.task.Task
import com.dennisce.zeus.utils.DispatcherLog
import java.util.*

object TaskSortUtil {

    private val sNewTasksHigh = ArrayList<Task>() // 高优先级的Task

    /**
     * 任务的有向无环图的拓扑排序
     *
     * @return
     */
    @Synchronized
    fun getSortResult(originTasks: List<Task>, clsLaunchTasks: List<Class<out Task>>): MutableList<Task> {
        val makeTime = System.currentTimeMillis()
        val dependSet = ArraySet<Int>()
        val graph = Graph(originTasks.size)
        for (i in originTasks.indices) {
            val task = originTasks[i]
            if (task.isSend || task.dependsOn().isEmpty()) {
                continue
            }
            for (cls in task.dependsOn()) {
                val indexOfDepend = getIndexOfTask(originTasks, clsLaunchTasks, cls)
                if (indexOfDepend < 0) {
                    throw IllegalStateException(
                        task.javaClass.simpleName +
                                " depends on " + cls.simpleName + " can not be found in task list "
                    )
                }
                dependSet.add(indexOfDepend)
                graph.addEdge(indexOfDepend, i)
            }
        }
        val indexList = graph.topologicalSort()
        val newTasksAll = getResultTasks(originTasks, dependSet, indexList)

        DispatcherLog.i("task analyse cost makeTime " + (System.currentTimeMillis() - makeTime))
        printAllTaskName(newTasksAll)
        return newTasksAll
    }

    private fun getResultTasks(originTasks: List<Task>, dependSet: Set<Int>, indexList: List<Int>): MutableList<Task> {
        val newTasksAll = ArrayList<Task>(originTasks.size)
        val newTasksDepended = ArrayList<Task>() // 被别人依赖的
        val newTasksWithOutDepend = ArrayList<Task>() // 没有依赖的
        val newTasksRunAsSoon = ArrayList<Task>() // 需要提升自己优先级的，先执行（这个先是相对于没有依赖的先）
        for (index in indexList) {
            if (dependSet.contains(index)) {
                newTasksDepended.add(originTasks[index])
            } else {
                val task = originTasks[index]
                if (task.needRunAsSoon()) {
                    newTasksRunAsSoon.add(task)
                } else {
                    newTasksWithOutDepend.add(task)
                }
            }
        }
        // 顺序：被别人依赖的————》需要提升自己优先级的————》需要被等待的————》没有依赖的
        sNewTasksHigh.addAll(newTasksDepended)
        sNewTasksHigh.addAll(newTasksRunAsSoon)
        newTasksAll.addAll(sNewTasksHigh)
        newTasksAll.addAll(newTasksWithOutDepend)
        return newTasksAll
    }

    private fun printAllTaskName(newTasksAll: List<Task>) {
        if (!Zeus.printAllTaskName) {
            return
        }
        for (task in newTasksAll) {
            DispatcherLog.i(task.javaClass.simpleName)
        }
    }

    /**
     * 获取任务在任务列表中的index
     *
     * @param originTasks
     * @param
     * @return
     */
    private fun getIndexOfTask(
        originTasks: List<Task>,
        clsLaunchTasks: List<Class<out Task>>,
        cls: Class<*>
    ): Int {
        val index = clsLaunchTasks.indexOf(cls)
        if (index >= 0) {
            return index
        }

        // 仅仅是保护性代码
        val size = originTasks.size
        for (i in 0 until size) {
            if (cls.simpleName == originTasks[i].javaClass.simpleName) {
                return i
            }
        }
        return index
    }
}
