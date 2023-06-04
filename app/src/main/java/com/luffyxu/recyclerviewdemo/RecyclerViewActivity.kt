package com.luffyxu.recyclerviewdemo

import android.graphics.Path
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luffyxu.recyclerviewdemo.databinding.ActivityMainBinding
import com.luffyxu.recyclerviewdemo.databinding.ActivityRecyclerViewBinding

class RecyclerViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecyclerViewBinding

    private val data : MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecyclerViewBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        val adapter :RecyclerView.Adapter<VH> = object : RecyclerView.Adapter<VH>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
                return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_string,parent,false))
            }

            override fun onBindViewHolder(holder: VH, position: Int) {
                val vh : VH = holder
                vh.tv.text = data[position]
            }

            override fun getItemCount(): Int {
                return data.size
            }
        }
        binding.rv.adapter = adapter
        val pathLayoutManager = PathLayoutManager(200f,RecyclerView.VERTICAL)
        binding.rv.layoutManager = pathLayoutManager
        for(i in 1..20){
            data.add(i.toString())
        }
        adapter.notifyItemRangeChanged(0,data.size)

        val path = Path().apply {
            moveTo(250F,250F)
            rLineTo(600F,300F)
            rLineTo(-600F,300F)
            rLineTo(600F,300F)
            rLineTo(-600F,250F);
        }
        pathLayoutManager.updatePath(path)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tv = itemView.findViewById<TextView>(R.id.tv)
    }
}