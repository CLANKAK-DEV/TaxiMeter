package com.myapp.taximeter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RideHistoryAdapter(private val rideHistory: List<String>) : RecyclerView.Adapter<RideHistoryAdapter.RideHistoryViewHolder>() {

    inner class RideHistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rideDetailsTextView: TextView = view.findViewById(R.id.rideDetailsTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideHistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ride_history, parent, false)
        return RideHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: RideHistoryViewHolder, position: Int) {
        val rideDetail = rideHistory[position]
        holder.rideDetailsTextView.text = rideDetail
    }

    override fun getItemCount(): Int {
        return rideHistory.size
    }
}
