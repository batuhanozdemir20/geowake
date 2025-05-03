package com.ozapps.geowake.adapter

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ozapps.geowake.R
import com.ozapps.geowake.databinding.AlarmRowBinding
import com.ozapps.geowake.roomdb.LocationAlarm
import com.ozapps.geowake.viewmodel.GeoWakeViewModel
import com.ozapps.geowake.views.MapsActivity

class AlarmAdapter(
    private var alarmList: ArrayList<LocationAlarm>,
    application: Application,
    private val context: Context
): Adapter<AlarmAdapter.AlarmHolder>() {
    class AlarmHolder(val binding: AlarmRowBinding): ViewHolder(binding.root)

    private val viewModel = GeoWakeViewModel(application)
    private val settingsPref = PreferenceManager.getDefaultSharedPreferences(context)

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<LocationAlarm>) {
        alarmList.clear()
        alarmList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmHolder {
        val binding = AlarmRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return AlarmHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AlarmHolder, position: Int) {
        val alarm = alarmList[position]
        val defaultDistance = settingsPref.getString("default_distance","100")!!.toInt()
        holder.binding.alarmNameTv.text = alarm.locationName
        holder.binding.alarmDistanceTv.text = (alarm.distance ?: defaultDistance).toString() + "m"

        holder.itemView.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(context,R.anim.button_click))
            val chooseAlarm = Intent(it.context, MapsActivity::class.java).apply {
                putExtra("alarm_id",alarm.id)
                putExtra("new",1)
            }
            it.context.startActivity(chooseAlarm)
        }

        holder.itemView.setOnLongClickListener {
            AlertDialog.Builder(context,R.style.alert_dialog_theme)
                .setTitle(R.string.delete_alarm_title)
                .setMessage(R.string.are_you_sure)
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.deleteAlarm(alarm)
                    notifyItemRemoved(position)
                    alarmList.removeAt(alarmList.size - 1)
                }
                .setNegativeButton(R.string.no,null)
                .show()
            true
        }
    }

    override fun getItemCount(): Int { return alarmList.size }
}