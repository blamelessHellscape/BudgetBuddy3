package com.styio.budgetbuddy3

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat

//https://www.geeksforgeeks.org/cardview-using-recyclerview-in-android-with-example/#

class CategoryAdapter(private val context: Context, categoryModelArrayList: ArrayList<CategoryModel>, private val listener: OnItemClickListener?) :
    RecyclerView.Adapter<CategoryAdapter.Viewholder>() {

    private val categoryModelArrayList: ArrayList<CategoryModel>
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryAdapter.Viewholder {
        // to inflate the layout for each item of recycler view.
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.budget_card, parent, false)
        return Viewholder(view, listener)
    }

    override fun onBindViewHolder(holder: CategoryAdapter.Viewholder, position: Int) {
        // to set data to textview and imageview of each card layout
        val model: CategoryModel = categoryModelArrayList[position]
        holder.total.text = NumberFormat.getCurrencyInstance().format(model.total).toString()
        holder.spent.text = NumberFormat.getCurrencyInstance().format(model.spent).toString()
        holder.progress.progress = model.percent
        holder.category.text = model.category

        //insert other values here.
    }

    override fun getItemCount(): Int {
        // this method is used for showing number of card items in recycler view.
        return categoryModelArrayList.size
    }
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    // View holder class for initializing of your views such as TextView and Imageview.

    class Viewholder(itemView: View, private val listener: OnItemClickListener?) : RecyclerView.ViewHolder(itemView) {
        val total: TextView
        val spent: TextView
        val progress: ProgressBar
        val category: TextView
        val btnEdit: Button
        init {
            total = itemView.findViewById(R.id.txt_total)
            spent = itemView.findViewById(R.id.txt_spent)
            progress = itemView.findViewById(R.id.prog_percent)
            category = itemView.findViewById(R.id.title)
            btnEdit = itemView.findViewById(R.id.btn_edit)
            btnEdit.setOnClickListener {
                // Handle button click here
                listener?.onItemClick(adapterPosition)
            }

        }




    }

    // Constructor
    init {
        this.categoryModelArrayList = categoryModelArrayList
    }
}
