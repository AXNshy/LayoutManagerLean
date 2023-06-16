package com.luffyxu.recyclerviewdemo.layoutmanager

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

class CircleLayoutManager(val radii: Float,val center : Position3D,val normal :Position3D,val angleOffset : Float = 45f) : LayoutManager(){

    var offsetX : Float = 0f
    var offsetY : Float = 0f
    private var mOffset : Float
        get(){
            return if(canScrollVertically()){
                offsetY
            }else{
                offsetX
            }
        }
        set(value) {
             if(canScrollVertically()){
                 offsetY = value
             }else{
                 offsetX = value
             }
        }


    var mPerimeter:Float = 0f


    init {
        mPerimeter = 2 * radii * Math.PI.toFloat()


    }
    data class CirclePosition(var index : Int = -1, var radius : Float,var position : Position3D)

    data class Position3D(val x: Float,val y:Float,var z:Float){
        operator fun plus(other : Position3D) : Position3D{
            return Position3D(x+ other.x,y+other.y,z+other.z)
        }

        operator fun minus(other : Position3D) : Position3D{
            return Position3D(x- other.x,y-other.y,z-other.z)
        }
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT)
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        super.onLayoutChildren(recycler, state)
        if (itemCount == 0) {
            removeAndRecycleAllViews(recycler)
            return
        }
        detachAndScrapAttachedViews(recycler)
        relayoutChildren(recycler,state)
    }

    private fun relayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        val items = initItems()
        Log.d(TAG,"initItems items count is ${items.size}")
        if(items == null || items.isEmpty()){
            removeAndRecycleAllViews(recycler)
            return
        }
        onLayout(recycler,items)
        recycleChildren(recycler)
    }

    private fun onLayout(
        recycler: RecyclerView.Recycler,
        items: List<CirclePosition>
    ) {
        items.sortedBy {
            it.position.z
        }.forEach {
            val view = recycler.getViewForPosition( if(it.index < 0) itemCount + it.index else it.index)
            addView(view)
            measureChildWithMargins(view,0,0)
            val scale = 0.8 + it.position.z /radii
            Log.d("onLayout","index:${it.index}")
            Log.d("onLayout","position:${it.position}")
            Log.d("onLayout","scale:$scale")

            val width = getDecoratedMeasuredWidth(view) * scale
            val height = getDecoratedMeasuredWidth(view) * scale

            Log.d("onLayout","width:$width,height:$height")
            layoutDecorated(view,
                (it.position.x - width / 2).toInt(),
                (it.position.y - height / 2).toInt(), (it.position.x + width / 2).toInt(), (it.position.y + height / 2).toInt()
            )

        }
    }


    fun initItems():List<CirclePosition>{

        Log.d(TAG, " mOffset :${mOffset}")
        Log.d(TAG, " mPerimeter :${mPerimeter}")
        val currentTopRadius = mOffset / mPerimeter * 360
        Log.d(TAG, " currentTopRadius :${currentTopRadius}")

        val currentItem : Int = ((currentTopRadius + angleOffset/2) / angleOffset).toInt()
        Log.d(TAG, " currentItem :${currentItem}")
//        if(currentTopRadius < 0){
//            if((currentTopRadius + 360) % angleOffset > angleOffset / 2){
//
//            }
//        }

        val itemList:MutableList<CirclePosition> = mutableListOf()
        val midRadius = (currentTopRadius+ angleOffset/2) % angleOffset
        Log.d(TAG, "midRadius:${midRadius}")
        var startRadius = currentItem
        var startItem = currentItem
        /*计算左半部分item*/
        var step = -1

        var curPos = findCurrentPosition(currentItem,midRadius)
        if(curPos != null){
            itemList.add(curPos)
            Log.d(TAG,"findCurrentPosition $curPos")
        }

        while(true){

            val pos = findNextLeftPosition(currentItem,midRadius,step)
            Log.d(TAG,"findNextLeftPosition $pos")
            if(pos != null){
                itemList.add(pos)
                step--
            }else
                break
        }
        step = 1
        while(true){
            val pos = findNextRightPosition(currentItem,midRadius,step)
            Log.d(TAG,"findNextRightPosition $pos")
            if(pos != null){
                itemList.add(pos)
                step++
            }else
                break
        }

        return itemList
    }

    fun findNextLeftPosition(topIndex:Int,topAngle:Float,step: Int) : CirclePosition?{
        var index = topIndex + step
        if(index >= itemCount){
            index -=itemCount
        }
        if(index <0){
            index += itemCount
        }
        val ang = topAngle + angleOffset * step
        if(ang > -180f){
            val x = center.x + radii * sin(ang)
            val y = center.y
            val z = center.z + radii + radii * cos(ang)

            return CirclePosition(index,ang, Position3D(x,y,z))
        }
        return null
    }

    fun findCurrentPosition(index : Int,topAngle:Float) : CirclePosition ? {
        var i = index
            if(i >= itemCount){
                i -=itemCount
            }
            if(i <0){
                i += itemCount
            }
            val ang = topAngle
            if(ang > -180f){
                val x = center.x + radii * sin(ang)
                val y = center.y
                val z = center.z + radii + radii * cos(ang)

                return CirclePosition(i,ang, Position3D(x,y,z))
            }
            return null
    }


    fun findNextRightPosition(topIndex:Int,topAngle:Float,step: Int) : CirclePosition?{
        var index = topIndex + step
        if(index >= itemCount){
            index -=itemCount
        }
        if(index <0){
            index += itemCount
        }
        val ang = topAngle + angleOffset * step
        if(ang < 180f){
            val x = center.x + radii * sin(ang)
            val y = center.y
            val z = center.z + radii + radii * cos(ang)
            return CirclePosition(index,ang, Position3D(x,y,z))
        }
        return null
    }

    /**
     * 回收屏幕外需回收的Item
     */
    private fun recycleChildren(recycler: RecyclerView.Recycler) {
        val scrapList = recycler.scrapList
        Log.d("recycleChildren", "scrapList.indices :${scrapList.indices} :size:${scrapList.size}")
        for (i in scrapList.indices.reversed()) {
            Log.d("recycleChildren", "size:$i scrapList.size:${scrapList.size}")
            val holder = scrapList[i]
            removeView(holder.itemView)
            recycler.recycleView(holder.itemView)
        }
    }
    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        detachAndScrapAttachedViews(recycler)
        updateOffsetX(dx)
        relayoutChildren(recycler, state)
        return dx
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        detachAndScrapAttachedViews(recycler)
        updateOffsetY(dy)
        relayoutChildren(recycler, state)
        return dy
    }

    private fun updateOffsetX(dx: Int) {
        mOffset += dx
        if(mOffset > angleOffset * mPerimeter / 360 * itemCount  ){
            mOffset = mOffset - angleOffset * mPerimeter / 360 * itemCount
        }

        if(mOffset < 0){
            mOffset += angleOffset * mPerimeter / 360 * itemCount
        }
    }

    private fun updateOffsetY(dy: Int) {
        mOffset += dy
    }

    fun visibleItemCount() : Int{
        return (360 / angleOffset).toInt()
    }

    override fun canScrollHorizontally(): Boolean {
        return normal.x.absoluteValue < normal.y.absoluteValue
    }

    override fun canScrollVertically(): Boolean {
        return normal.x.absoluteValue >= normal.y.absoluteValue
    }

    companion object{
        const val TAG = "CircleLayoutManager"
    }
}