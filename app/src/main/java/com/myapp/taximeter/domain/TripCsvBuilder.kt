package com.myapp.taximeter.domain

import com.myapp.taximeter.data.local.TripEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pure CSV serialization of the trip history — deterministic, no Android
 * dependencies. Text cells are quoted per RFC 4180 (embedded quotes doubled);
 * numeric cells use Locale.US formatting so the file parses the same on every
 * device locale.
 */
object TripCsvBuilder {

    const val HEADER: String =
        "start_time,end_time,distance_km,duration_minutes,waiting_minutes," +
            "fare,currency_code,currency_symbol,country,city,vehicle_type"

    fun build(
        trips: List<TripEntity>,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            this.timeZone = timeZone
        }

        val sb = StringBuilder()
        sb.append(HEADER).append('\n')
        trips.forEach { t ->
            sb.append(df.format(Date(t.startTime))).append(',')
            sb.append(df.format(Date(t.endTime))).append(',')
            sb.append(String.format(Locale.US, "%.3f", t.distanceKm)).append(',')
            sb.append(String.format(Locale.US, "%.1f", t.durationMinutes)).append(',')
            sb.append(String.format(Locale.US, "%.1f", t.waitingMinutes)).append(',')
            sb.append(String.format(Locale.US, "%.2f", t.totalFareLocal)).append(',')
            sb.append(quote(t.currencyCode)).append(',')
            sb.append(quote(t.currencySymbol)).append(',')
            sb.append(quote(t.countryName)).append(',')
            sb.append(quote(t.city)).append(',')
            sb.append(quote(t.vehicleType)).append('\n')
        }
        return sb.toString()
    }

    /** RFC 4180 quoting: wrap in quotes, double any embedded quote. Null → "". */
    fun quote(value: String?): String =
        "\"" + (value ?: "").replace("\"", "\"\"") + "\""
}
