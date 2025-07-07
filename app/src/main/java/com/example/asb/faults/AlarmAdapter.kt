package com.example.asb.faults

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.asb.R

class AlarmAdapter(private var alarms: List<Alarma>) :
    RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCodigo: TextView = itemView.findViewById(R.id.tv_registro)
        val tvMensaje: TextView = itemView.findViewById(R.id.tv_estructura)
        val tvFecha: TextView = itemView.findViewById(R.id.tv_fecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarms[position]
        holder.tvCodigo.text = holder.itemView.context.getString(R.string.id_with_placeholder, alarm.idModbus)
        holder.tvMensaje.text = alarm.mensaje
        holder.tvFecha.text = alarm.fecha
    }

    override fun getItemCount() = alarms.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateAlarms(newAlarms: List<Alarma>) {
        alarms = newAlarms
        notifyDataSetChanged()
    }
}