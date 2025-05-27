package com.example.asb.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.asb.R
import com.example.asb.db.electricaldatalogtest.ElectricData
import java.util.Date

class AnomaliesAdapter : ListAdapter<ElectricData, AnomaliesAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvType: TextView = view.findViewById(R.id.tvAnomalyType)
        val tvValue: TextView = view.findViewById(R.id.tvAnomalyValue)
        val tvDate: TextView = view.findViewById(R.id.tvAnomalyDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_anomaly, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvType.text = when (item.type) {
            "VOLTAGE_LOW" -> holder.itemView.context.getString(R.string.voltage_low)
            "VOLTAGE_HIGH" -> holder.itemView.context.getString(R.string.voltage_high)
            "PRESSURE_LOW" -> holder.itemView.context.getString(R.string.pressure_low)
            "PRESSURE_HIGH" -> holder.itemView.context.getString(R.string.pressure_high)
            else -> item.type
        }

        // Formatear valor
        holder.tvValue.text = holder.itemView.context.getString(
            R.string.value_format,
            item.value
        )

        // Formatear fecha
        val dateString = Date(item.timestamp).toString()
        holder.tvDate.text = holder.itemView.context.getString(
            R.string.date_format,
            dateString
        )
    }
}


class DiffCallback : DiffUtil.ItemCallback<ElectricData>() {
    override fun areItemsTheSame(oldItem: ElectricData, newItem: ElectricData): Boolean {
        return oldItem.id == newItem.id  // Compara IDs únicos
    }

    override fun areContentsTheSame(oldItem: ElectricData, newItem: ElectricData): Boolean {
        return oldItem == newItem  // Compara todos los campos (usa data class en ElectricData)
    }
}
