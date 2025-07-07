package com.example.asb.db.json

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.asb.R

class ElectricDataAdapter : ListAdapter<ElectricDataResponse.Registro, ElectricDataAdapter.ElectricDataViewHolder>(DiffCallback()) {

    inner class ElectricDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
        private val tvVoltage: TextView = itemView.findViewById(R.id.tv_voltage)
        private val tvCurrent: TextView = itemView.findViewById(R.id.tv_current)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)

        fun bind(registro: ElectricDataResponse.Registro) {
            tvTimestamp.text = registro.timestamp
            tvVoltage.text = itemView.context.getString(R.string.voltage_format, registro.voltaje)
            tvCurrent.text = itemView.context.getString(R.string.current_format, registro.corriente)
            tvStatus.text = registro.estado
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ElectricDataResponse.Registro>() {
        override fun areItemsTheSame(oldItem: ElectricDataResponse.Registro, newItem: ElectricDataResponse.Registro): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ElectricDataResponse.Registro, newItem: ElectricDataResponse.Registro): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ElectricDataViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_electric_data, parent, false)
        return ElectricDataViewHolder(view)
    }

    override fun onBindViewHolder(holder: ElectricDataViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}