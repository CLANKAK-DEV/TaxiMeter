package com.myapp.taximeter.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.myapp.taximeter.R
import com.myapp.taximeter.data.local.TripEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Read-only list of completed trips shown in the Trip History dialog.
 */
class TripHistoryAdapter : RecyclerView.Adapter<TripHistoryAdapter.VH>() {

    private var trips: List<TripEntity> = emptyList()

    private val dateFormat = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.tripDate)
        val meta: TextView = view.findViewById(R.id.tripMeta)
        val fare: TextView = view.findViewById(R.id.tripFare)
    }

    fun submitList(newTrips: List<TripEntity>) {
        trips = newTrips
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_trip_history, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val trip = trips[position]
        holder.date.text = dateFormat.format(Date(trip.startTime))

        val durationMillis = ((trip.endTime - trip.startTime).coerceAtLeast(0L))
        val durationStr = String.format(
            Locale.getDefault(), "%02d:%02d",
            durationMillis / 60000, (durationMillis % 60000) / 1000
        )
        holder.meta.text = String.format(
            Locale.getDefault(), "%.2f km · %s · %s",
            trip.distanceKm, durationStr, trip.countryName
        )
        holder.fare.text = String.format(
            Locale.getDefault(), "%.2f %s",
            trip.totalFareLocal, trip.currencySymbol
        )
    }

    override fun getItemCount(): Int = trips.size
}
