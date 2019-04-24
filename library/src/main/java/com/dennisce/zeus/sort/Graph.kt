package com.dennisce.zeus.sort

import java.util.*
import kotlin.collections.ArrayList

/**
 * 有向无环图的拓扑排序算法
 */
class Graph(private val graphCount: Int) {
    // 邻接表
    private val mAdj: Array<ArrayList<Int>> by lazy {
        Array<ArrayList<Int>>(graphCount) { ArrayList() }
    }

    /**
     * 添加边
     *
     * @param u from
     * @param v to
     */
    fun addEdge(u: Int, v: Int) {
        mAdj[u].add(v)
    }

    /**
     * 拓扑排序
     */
    fun topologicalSort(): Vector<Int> {
        val inDegree = IntArray(graphCount)
        for (i in 0 until graphCount) { // 初始化所有点的入度数量
            val temp = mAdj[i]
            for (node in temp) {
                inDegree[node]++
            }
        }
        val queue = LinkedList<Int>()
        for (i in 0 until graphCount) { // 找出所有入度为0的点
            if (inDegree[i] == 0) {
                queue.add(i)
            }
        }
        var cnt = 0
        val topOrder = Vector<Int>()
        while (!queue.isEmpty()) {
            val u = queue.poll()
            topOrder.add(u)
            for (node in mAdj[u]) { // 找到该点（入度为0）的所有邻接点
                if (--inDegree[node] == 0) { // 把这个点的入度减一，如果入度变成了0，那么添加到入度0的队列里
                    queue.add(node)
                }
            }
            cnt++
        }
        if (cnt != graphCount) { // 检查是否有环，理论上拿出来的点的次数和点的数量应该一致，如果不一致，说明有环
            throw IllegalStateException("Exists a cycle in the graph")
        }
        return topOrder
    }
}
