package com.myapp.taximeter.domain

import com.myapp.taximeter.data.local.TripEntity
import java.util.Calendar
import java.util.TimeZone

/** Aggregated stats for one earnings period (today / 7 days / 30 days). */
data class PeriodStats(
    val trips: Int,
    val distanceKm: Double,
    val earnings: Double
)

/**
 * Snapshot for the earnings dashboard, computed from the Room trip table.
 * [last7DayEarnings] is ordered oldest → today (7 buckets) and
 * [last7DayStarts] holds the start-of-day timestamp of each bucket so the UI
 * can render weekday labels. [currencySymbol] is null when the saved trips
 * mix more than one currency.
 */
data class EarningsSummary(
    val daily: PeriodStats,
    val weekly: PeriodStats,
    val monthly: PeriodStats,
    val last7DayEarnings: List<Double>,
    val last7DayStarts: List<Long>,
    val currencySymbol: String?
)

/**
 * Pure earnings-period bucketing — deterministic, no Android dependencies.
 *
 * Period definitions (all relative to the local start of "today"):
 * - daily:   trips started since 00:00 today
 * - weekly:  trips started in the last 7 calendar days (today included)
 * - monthly: trips started in the last 30 calendar days (today included)
 */
object EarningsCalculator {

    fun summarize(
        trips: List<TripEntity>,
        now: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): EarningsSummary {
        val startOfToday = Calendar.getInstance(timeZone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayMs = 24L * 60L * 60L * 1000L

        fun statsSince(from: Long): PeriodStats {
            val selected = trips.filter { it.startTime >= from }
            return PeriodStats(
                trips = selected.size,
                distanceKm = selected.sumOf { it.distanceKm },
                earnings = selected.sumOf { it.totalFareLocal }
            )
        }

        val dayStarts = (6 downTo 0).map { startOfToday - it * dayMs }
        val dayEarnings = dayStarts.map { from ->
            trips.filter { it.startTime >= from && it.startTime < from + dayMs }
                .sumOf { it.totalFareLocal }
        }

        return EarningsSummary(
            daily = statsSince(startOfToday),
            weekly = statsSince(startOfToday - 6 * dayMs),
            monthly = statsSince(startOfToday - 29 * dayMs),
            last7DayEarnings = dayEarnings,
            last7DayStarts = dayStarts,
            currencySymbol = trips.map { it.currencySymbol }.distinct().singleOrNull()
        )
    }
}
