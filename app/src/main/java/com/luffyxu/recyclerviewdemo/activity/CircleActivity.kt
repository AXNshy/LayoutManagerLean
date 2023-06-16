package com.luffyxu.recyclerviewdemo.activity

import android.graphics.Color
import android.graphics.Path
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.luffyxu.recyclerviewdemo.R
import com.luffyxu.recyclerviewdemo.databinding.ActivityRecyclerViewBinding
import com.luffyxu.recyclerviewdemo.layoutmanager.CircleLayoutManager

class CircleActivity  : AppCompatActivity() {

    private lateinit var binding: ActivityRecyclerViewBinding

    private val data : MutableList<String> = mutableListOf()

    private val colorSet = arrayOf(Color.RED,Color.BLUE,Color.GREEN,Color.GRAY,Color.BLACK)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecyclerViewBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        val adapter : RecyclerView.Adapter<VH> = object : RecyclerView.Adapter<VH>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
                return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_image,parent,false))
            }

            override fun onBindViewHolder(holder: VH, position: Int) {
                val vh : VH = holder
                val colorIndex = position%colorSet.size
                vh.image.background = ColorDrawable(colorSet[colorIndex])
                vh.text.text = position.toString()
            }

            override fun getItemCount(): Int {
                return data.size
            }
        }
        binding.rv.adapter = adapter

        val circleLayoutManager =  CircleLayoutManager(500f, CircleLayoutManager.Position3D(500f,500f,-500f),CircleLayoutManager.Position3D(0f,1f,0f),45f)
        binding.rv.layoutManager = circleLayoutManager
        for(i in 1..20){
            data.add(i.toString())
        }
        adapter.notifyItemRangeChanged(0,data.size)

    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image = itemView.findViewById<ImageView>(R.id.image)
        val text = itemView.findViewById<TextView>(R.id.text)
    }

}