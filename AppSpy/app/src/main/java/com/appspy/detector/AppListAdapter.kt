package com.appspy.detector

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppListAdapter(
    private var items: List<AppInfo>,
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textAppName)
        val pkg: TextView = view.findViewById(R.id.textPackageName)
        val install: TextView = view.findViewById(R.id.textInstallDate)
        val notif: TextView = view.findViewById(R.id.textNotifCount)
        val risk: TextView = view.findViewById(R.id.textRisk)
    }

    fun updateItems(newItems: List<AppInfo>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = items[position]
        holder.name.text = app.appName
        holder.pkg.text = app.packageName
        holder.install.text = "Instalado em: ${dateFormat.format(Date(app.installTime))}"
        holder.notif.text = "Notificações (24h): ${app.notificationCount24h}"
        holder.risk.text = app.riskLabel()

        val color = when (app.riskLabel()) {
            "ALTO RISCO" -> Color.parseColor("#D32F2F")
            "SUSPEITO" -> Color.parseColor("#F57C00")
            else -> Color.parseColor("#388E3C")
        }
        holder.risk.setTextColor(color)

        holder.itemView.setOnClickListener { onClick(app) }
    }

    override fun getItemCount(): Int = items.size
}
