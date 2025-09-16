package com.ozapps.geowake.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ozapps.geowake.R
import com.ozapps.geowake.databinding.AlarmRowBinding
import com.ozapps.geowake.roomdb.LocationAlarm
import com.ozapps.geowake.views.MapsActivity

class AlarmAdapter(
    private val context: Context
): Adapter<AlarmAdapter.AlarmHolder>() {
    class AlarmHolder(val binding: AlarmRowBinding): ViewHolder(binding.root)

    private val settingsPref = PreferenceManager.getDefaultSharedPreferences(context)
    private val trackingPref = context.getSharedPreferences("com.ozapps.geowake", MODE_PRIVATE)

    private val diffUtil = object: DiffUtil.ItemCallback<LocationAlarm>() {
        override fun areItemsTheSame(
            oldItem: LocationAlarm,
            newItem: LocationAlarm
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: LocationAlarm,
            newItem: LocationAlarm
        ): Boolean {
            return oldItem == newItem
        }

    }
    private val recyclerListDiffer = AsyncListDiffer(this,diffUtil)

    var alarms: List<LocationAlarm>
        get() = recyclerListDiffer.currentList
        set(value) = recyclerListDiffer.submitList(value)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmHolder {
        val binding = AlarmRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return AlarmHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AlarmHolder, position: Int) {
        val alarm = alarms[position]
        val defaultDistance = settingsPref.getString("default_distance","500")!!.toInt()
        holder.binding.alarmNameTv.text = alarm.locationName
        holder.binding.alarmDistanceTv.text = (alarm.distance ?: defaultDistance).toString() + "m"

        var new = 1

        val trackingAlarmID = trackingPref.getInt("tracking_alarm_id",0)
        if (trackingAlarmID == alarm.id) {
            holder.binding.iconIv.setImageResource(R.drawable.distance_icon)
            holder.binding.trackingTv.visibility = View.VISIBLE
            holder.itemView.setBackgroundResource(R.drawable.active_alarm_row_bg)
            new = 2
        }

        holder.itemView.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(context,R.anim.button_click))
            val chooseAlarm = Intent(it.context, MapsActivity::class.java).apply {
                putExtra("alarm_id",alarm.id)
                putExtra("new",new)
            }
            it.context.startActivity(chooseAlarm)
        }
    }

    override fun getItemCount(): Int { return alarms.size }
}