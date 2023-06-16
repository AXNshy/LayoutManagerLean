package com.luffyxu.recyclerviewdemo.layoutmanager

import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.annotation.FloatRange
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import java.lang.Integer.max
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor

class PathLayoutManager(var itemOffset: Float, @Orientation val orientation: Int = VERTICAL) :
    LayoutManager() {

    data class State(
        var itemOffset: Float = 10f,
        var itemMaxSizeOnScreen: Int = 0,
        @Orientation var orientation: Int = VERTICAL,
        var mFirstVisiblePosition: Int = 0,
        var mLastVisiblePosition: Int = 0,
        var enableLoop: Boolean = true
    )

    val state: State = State(itemOffset = itemOffset, orientation = orientation)

    var pointHelper: PathHelper? = null

    var mScrollOffset: Float
        get() = if (canScrollVertically()) mScrollOffsetY else mScrollOffsetX
        set(value) = if (canScrollVertically()) mScrollOffsetY = value else mScrollOffsetX = value
    private var mScrollOffsetX: Float = 0F
    private var mScrollOffsetY: Float = 0F

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
    }

    fun updatePath(path: Path) {
        pointHelper = PathHelper(path)
        state.itemMaxSizeOnScreen = pointHelper!!.len.div(state.itemOffset).toInt() + 1
        requestLayout()
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        super.onLayoutChildren(recycler, state)
        if (itemCount == 0) {
            removeAndRecycleAllViews(recycler)
            return
        }
        detachAndScrapAttachedViews(recycler)
        relayoutChildren(recycler, state)
    }

    private fun onLayout(
        recycler: Recycler,
        visibleItems: List<PosTan>
    ) {
        var view: View
        var x: Int = -1
        var y: Int = -1
        for (postan in visibleItems) {
            view = recycler.getViewForPosition(postan.index)
            addView(view)
            measureChildWithMargins(view, 0, 0)
            x = postan.point.x.toInt() - getDecoratedMeasuredWidth(view)
            y = postan.point.y.toInt() - getDecoratedMeasuredHeight(view)
            layoutDecorated(
                view,
                x,
                y,
                x + getDecoratedMeasuredWidth(view),
                y + getDecoratedMeasuredHeight(view)
            )
            view.rotation = postan.angle
        }
    }

    fun initItemsNeedLayoutEnableLoop(): List<PosTan> {
        val result = mutableListOf<PosTan>()
        if (pointHelper!!.len > itemCount * itemOffset) return result

        if (mScrollOffset > itemOffset * itemCount) {
            mScrollOffset -= itemOffset * itemCount
        }

        if (mScrollOffset < -itemOffset * itemCount) {
            mScrollOffset += itemOffset * itemCount
        }
        var startPoint: Int = floor(mScrollOffset / itemOffset).toInt()

        if(startPoint <0){
            startPoint += itemCount
        }

        var endPos: Int =
            ceil(mScrollOffset + state.itemMaxSizeOnScreen * itemOffset).div(itemOffset).toInt()

        if(endPos >= itemCount){
            endPos -=itemCount
        }
        /*可见第一项索引小于最后一项，表面列表显示没有越界*/
        if (startPoint <= endPos) {
            Log.d("initItemsNeedLayoutEnableLoop", "startPoint:$startPoint,endPos:$endPos")
            for (i in startPoint..endPos) {
                var distance = i * itemOffset - mScrollOffset
                var percent = distance.div(pointHelper?.len ?: 1f)
                pointHelper?.findPosTan(percent)?.let {
                    it.addIndex(i)
                    result.add(it)
                }
            }
            state.mFirstVisiblePosition = startPoint
            state.mLastVisiblePosition = endPos
            Log.d("initItemsNeedLayoutEnableLoop", "result:$result")
        } else {
            /*可见第一项索引 大于最后一项，列表显示越界*/
            Log.d("initItemsNeedLayoutEnableLoop", "startPoint:$startPoint,endPos:$endPos")
            for(i in startPoint until itemCount){
                var distance = i * itemOffset - mScrollOffset
                Log.d("initItemsNeedLayoutEnableLoop", "distance:$distance")
                var percent = distance.div(pointHelper?.len ?: 1f)
                pointHelper?.findPosTan(percent)?.let {
                    it.addIndex(i)
                    result.add(it)
                }
            }

            for(i in 0..endPos){
                var distance = (i + itemCount) * itemOffset - mScrollOffset
                var percent = distance.div(pointHelper?.len ?: 1f)
                pointHelper?.findPosTan(percent)?.let {
                    it.addIndex(i)
                    result.add(it)
                }
            }
        }

        return result
    }

    fun initItemsNeedLayout(): List<PosTan> {
        val result = mutableListOf<PosTan>()
        val startPoint: Int = (mScrollOffset / itemOffset).toInt()
        var endPos: Int = startPoint + state.itemMaxSizeOnScreen


        Log.d("initItemsNeedLayout", "startPoint:$startPoint,endPos:$endPos")
        endPos = max(endPos, itemCount)
        for (i in startPoint..endPos) {
            var distance = i * itemOffset - mScrollOffset
            var percent = distance.div(pointHelper?.len ?: 1f)
            pointHelper?.findPosTan(percent)?.let {
                it.addIndex(i)
                result.add(it)
            }
        }
        state.mFirstVisiblePosition = startPoint
        state.mLastVisiblePosition = endPos
        Log.d("initItemsNeedLayout", "result:$result")
        return result
    }

    override fun scrollVerticallyBy(dy: Int, recycler: Recycler, state: RecyclerView.State): Int {
        checkPathHelper()
        detachAndScrapAttachedViews(recycler)
        val offset = mScrollOffsetY
        updateOffsetY(dy)
        relayoutChildren(recycler, state)
        return if (offset == mScrollOffsetY) 0 else dy
    }


    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: Recycler,
        state: RecyclerView.State
    ): Int {
        checkPathHelper()
        detachAndScrapAttachedViews(recycler)
        val offset = mScrollOffsetX
        updateOffsetX(dx)
        relayoutChildren(recycler, state)
        return if (offset == mScrollOffsetX) 0 else dx
    }

    private fun relayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        val visibleItems =  if(!this.state.enableLoop) initItemsNeedLayout() else initItemsNeedLayoutEnableLoop()
        if (visibleItems.isEmpty() || state.itemCount == 0 || pointHelper == null) {
            removeAndRecycleAllViews(recycler)
            return
        }
        Log.d("relayoutChildren", "scrapList before layout:${recycler.scrapList.size}")
        onLayout(recycler, visibleItems)
        Log.d("relayoutChildren", "scrapList after layout:${recycler.scrapList.size}")
        recycleChildren(recycler)
    }

    private fun updateOffsetY(dy: Int) {
        mScrollOffsetY += dy
        if (state.enableLoop) {
            return
        }
        val pathLength = pointHelper!!.len
        val itemLength = getItemLength()
        val overflowLength = itemLength - pathLength
        if (mScrollOffsetY < 0) {
            mScrollOffsetY = 0f
        } else if (overflowLength < 0) {
            mScrollOffsetY -= dy
        } else if (mScrollOffsetY > overflowLength) {
            mScrollOffsetY = overflowLength
        }
    }

    private fun updateOffsetX(dx: Int) {
        mScrollOffsetX += dx
        if (state.enableLoop) {
            return
        }
        val pathLength = pointHelper!!.len
        val itemLength = getItemLength()
        val overflowLength = itemLength - pathLength
        if (mScrollOffsetX < 0) {
            mScrollOffsetX = 0f
        } else if (overflowLength < 0) {
            mScrollOffsetX -= dx
        } else if (mScrollOffsetX > overflowLength) {
            mScrollOffsetX = overflowLength
        }
    }

    private fun getItemLength(): Float {
        return itemCount * itemOffset - itemOffset + 1
    }

    private fun checkPathHelper() {
        if (pointHelper == null) {
            throw java.lang.IllegalArgumentException("PathHelper must be initialized")
        }
    }

    /**
     * 回收屏幕外需回收的Item
     */
    private fun recycleChildren(recycler: Recycler) {
        val scrapList = recycler.scrapList
        Log.d("recycleChildren", "scrapList.indices :${scrapList.indices} :size:${scrapList.size}")
        for (i in scrapList.indices.reversed()) {
            Log.d("recycleChildren", "size:$i scrapList.size:${scrapList.size}")
            val holder = scrapList[i]
            removeView(holder.itemView)
            recycler.recycleView(holder.itemView)
        }
    }


    override fun canScrollHorizontally(): Boolean {
        return orientation == HORIZONTAL
    }

    override fun canScrollVertically(): Boolean {
        return orientation == VERTICAL
    }

    class PathHelper(val path: Path) {

        val points: MutableList<PosTan> = mutableListOf()
        var mXs: FloatArray
        var mYs: FloatArray
        var mAngles: FloatArray

        //        val pathMeasure : PathMeasure
        val PRECISION = .5F
        var itemSize: Int = 0
        var len: Float = 0F

        init {
            val pathMeasure = PathMeasure(path, false)
            len = pathMeasure.length
            itemSize = len.div(PRECISION).toInt() + 1
            mXs = FloatArray(itemSize)
            mYs = FloatArray(itemSize)
            mAngles = FloatArray(itemSize)
            var distance: Float = 0f
            val pointTmp = floatArrayOf(0f, 0f)
            val tanTmp = floatArrayOf(0f, 0f)

            for (i in 0 until itemSize) {
                distance = i * len / (itemSize - 1)
                pathMeasure.getPosTan(distance, pointTmp, tanTmp)
                val angle = fixAngle(
                    (atan2(
                        tanTmp[1].toDouble(),
                        tanTmp[0].toDouble()
                    ) * 180F / Math.PI).toFloat()
                )
                points.add(PosTan(PointF(pointTmp[0], pointTmp[1]), angle))
            }
        }

        private fun fixAngle(rotation: Float): Float {
            var rot = rotation
            val angle = 360F
            if (rot < 0) {
                rot += angle
            }
            if (rot > angle) {
                rot %= angle
            }
            return rot
        }

        fun findPosTan(@FloatRange(from = 0.0, to = 1.0) percent: Float): PosTan? {
            val index: Int = (itemSize * percent).toInt()
            if (index in points.indices) {
                return points[index]
            }
            return null
        }
    }


    data class PosTan(var point: PointF, var angle: Float, var index: Int = -1) {
        fun addIndex(index: Int) {
            this.index = index
        }
    }
}