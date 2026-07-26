package com.myapp.taximeter.data

import com.myapp.taximeter.data.model.CurrencyInfo
import com.myapp.taximeter.data.model.FareConfig

/**
 * Static in‑app global fare database.
 *
 * NOTE: Values are indicative, simplified averages – not official tariffs.
 * The structure is designed so you can later plug in a remote API or update
 * this list without touching the calculation engine or UI.
 */
object GlobalFareDatabase {

    /** Night hours are fixed here but could be made per‑city in the future. */
    const val NIGHT_START_HOUR = 22  // 22:00
    const val NIGHT_END_HOUR = 6     // 06:00

    val fares: List<FareConfig> = listOf(
        // ── Africa / Middle East ─────────────────────────────────────────────
        FareConfig(
            countryCode = "MA",
            countryName = "Morocco",
            majorCities = listOf("Casablanca", "Rabat", "Marrakech", "Tangier"),
            currency = CurrencyInfo("MAD", "DH", "Moroccan Dirham", 2),
            baseFare = 7.0,
            farePerKm = 3.5,
            farePerMinute = 0.75,
            minimumFare = 15.0,
            waitingTimeRatePerMinute = 0.60,
            nightSurchargePercent = 0.25,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 20.0
        ),
        FareConfig(
            countryCode = "EG",
            countryName = "Egypt",
            majorCities = listOf("Cairo", "Alexandria", "Giza"),
            currency = CurrencyInfo("EGP", "E£", "Egyptian Pound", 2),
            baseFare = 8.0,
            farePerKm = 3.0,
            farePerMinute = 0.70,
            minimumFare = 12.0,
            waitingTimeRatePerMinute = 0.40,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.05,
            airportSurcharge = 25.0
        ),
        FareConfig(
            countryCode = "SA",
            countryName = "Saudi Arabia",
            majorCities = listOf("Riyadh", "Jeddah", "Dammam"),
            currency = CurrencyInfo("SAR", "﷼", "Saudi Riyal", 2),
            baseFare = 10.0,
            farePerKm = 3.75,
            farePerMinute = 0.90,
            minimumFare = 18.0,
            waitingTimeRatePerMinute = 0.75,
            nightSurchargePercent = 0.25,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 30.0
        ),
        FareConfig(
            countryCode = "AE",
            countryName = "United Arab Emirates",
            majorCities = listOf("Dubai", "Abu Dhabi", "Sharjah"),
            currency = CurrencyInfo("AED", "د.إ", "UAE Dirham", 2),
            baseFare = 12.0,
            farePerKm = 4.0,
            farePerMinute = 1.0,
            minimumFare = 20.0,
            waitingTimeRatePerMinute = 0.80,
            nightSurchargePercent = 0.25,
            weekendSurchargePercent = 0.15,
            airportSurcharge = 25.0
        ),
        FareConfig(
            countryCode = "NG",
            countryName = "Nigeria",
            majorCities = listOf("Lagos", "Abuja"),
            currency = CurrencyInfo("NGN", "₦", "Nigerian Naira", 2),
            baseFare = 500.0,
            farePerKm = 250.0,
            farePerMinute = 60.0,
            minimumFare = 800.0,
            waitingTimeRatePerMinute = 40.0,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 500.0
        ),
        FareConfig(
            countryCode = "ZA",
            countryName = "South Africa",
            majorCities = listOf("Johannesburg", "Cape Town", "Durban"),
            currency = CurrencyInfo("ZAR", "R", "South African Rand", 2),
            baseFare = 12.0,
            farePerKm = 11.0,
            farePerMinute = 1.8,
            minimumFare = 30.0,
            waitingTimeRatePerMinute = 1.0,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 40.0
        ),
        FareConfig(
            countryCode = "KE",
            countryName = "Kenya",
            majorCities = listOf("Nairobi", "Mombasa"),
            currency = CurrencyInfo("KES", "KSh", "Kenyan Shilling", 2),
            baseFare = 150.0,
            farePerKm = 80.0,
            farePerMinute = 20.0,
            minimumFare = 250.0,
            waitingTimeRatePerMinute = 15.0,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 300.0
        ),
        FareConfig(
            countryCode = "QA",
            countryName = "Qatar",
            majorCities = listOf("Doha"),
            currency = CurrencyInfo("QAR", "QR", "Qatari Riyal", 2),
            baseFare = 10.0,
            farePerKm = 4.0,
            farePerMinute = 1.0,
            minimumFare = 18.0,
            waitingTimeRatePerMinute = 0.80,
            nightSurchargePercent = 0.25,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 25.0
        ),

        // ── Europe ─────────────────────────────────────────────────────────────
        FareConfig(
            countryCode = "FR",
            countryName = "France",
            majorCities = listOf("Paris", "Lyon", "Marseille"),
            currency = CurrencyInfo("EUR", "€", "Euro", 2),
            baseFare = 2.8,
            farePerKm = 1.3,
            farePerMinute = 0.5,
            minimumFare = 7.0,
            waitingTimeRatePerMinute = 0.40,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 5.0
        ),
        FareConfig(
            countryCode = "DE",
            countryName = "Germany",
            majorCities = listOf("Berlin", "Munich", "Frankfurt", "Hamburg"),
            currency = CurrencyInfo("EUR", "€", "Euro", 2),
            baseFare = 3.5,
            farePerKm = 2.0,
            farePerMinute = 0.6,
            minimumFare = 8.0,
            waitingTimeRatePerMinute = 0.50,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.15,
            airportSurcharge = 7.0
        ),
        FareConfig(
            countryCode = "GB",
            countryName = "United Kingdom",
            majorCities = listOf("London", "Manchester", "Birmingham"),
            currency = CurrencyInfo("GBP", "£", "British Pound", 2),
            baseFare = 3.0,
            farePerKm = 2.4,
            farePerMinute = 0.7,
            minimumFare = 9.0,
            waitingTimeRatePerMinute = 0.60,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 5.0
        ),
        FareConfig(
            countryCode = "ES",
            countryName = "Spain",
            majorCities = listOf("Madrid", "Barcelona", "Valencia"),
            currency = CurrencyInfo("EUR", "€", "Euro", 2),
            baseFare = 2.5,
            farePerKm = 1.2,
            farePerMinute = 0.45,
            minimumFare = 6.0,
            waitingTimeRatePerMinute = 0.35,
            nightSurchargePercent = 0.15,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 5.0
        ),
        FareConfig(
            countryCode = "IT",
            countryName = "Italy",
            majorCities = listOf("Rome", "Milan", "Naples"),
            currency = CurrencyInfo("EUR", "€", "Euro", 2),
            baseFare = 3.0,
            farePerKm = 1.4,
            farePerMinute = 0.50,
            minimumFare = 7.0,
            waitingTimeRatePerMinute = 0.40,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 6.0
        ),
        FareConfig(
            countryCode = "PT",
            countryName = "Portugal",
            majorCities = listOf("Lisbon", "Porto"),
            currency = CurrencyInfo("EUR", "€", "Euro", 2),
            baseFare = 3.0,
            farePerKm = 0.9,
            farePerMinute = 0.40,
            minimumFare = 4.5,
            waitingTimeRatePerMinute = 0.30,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 5.0
        ),
        FareConfig(
            countryCode = "NL",
            countryName = "Netherlands",
            majorCities = listOf("Amsterdam", "Rotterdam", "The Hague"),
            currency = CurrencyInfo("EUR", "€", "Euro", 2),
            baseFare = 3.2,
            farePerKm = 2.4,
            farePerMinute = 0.6,
            minimumFare = 8.0,
            waitingTimeRatePerMinute = 0.55,
            nightSurchargePercent = 0.25,
            weekendSurchargePercent = 0.15,
            airportSurcharge = 7.0
        ),
        FareConfig(
            countryCode = "SE",
            countryName = "Sweden",
            majorCities = listOf("Stockholm", "Gothenburg"),
            currency = CurrencyInfo("SEK", "kr", "Swedish Krona", 2),
            baseFare = 45.0,
            farePerKm = 14.0,
            farePerMinute = 5.0,
            minimumFare = 80.0,
            waitingTimeRatePerMinute = 3.5,
            nightSurchargePercent = 0.25,
            weekendSurchargePercent = 0.15,
            airportSurcharge = 60.0
        ),
        FareConfig(
            countryCode = "NO",
            countryName = "Norway",
            majorCities = listOf("Oslo", "Bergen"),
            currency = CurrencyInfo("NOK", "kr", "Norwegian Krone", 2),
            baseFare = 60.0,
            farePerKm = 18.0,
            farePerMinute = 6.0,
            minimumFare = 120.0,
            waitingTimeRatePerMinute = 4.5,
            nightSurchargePercent = 0.25,
            weekendSurchargePercent = 0.20,
            airportSurcharge = 80.0
        ),
        FareConfig(
            countryCode = "CH",
            countryName = "Switzerland",
            majorCities = listOf("Zurich", "Geneva", "Basel"),
            currency = CurrencyInfo("CHF", "CHF", "Swiss Franc", 2),
            baseFare = 6.0,
            farePerKm = 3.8,
            farePerMinute = 0.9,
            minimumFare = 12.0,
            waitingTimeRatePerMinute = 0.80,
            nightSurchargePercent = 0.25,
            weekendSurchargePercent = 0.15,
            airportSurcharge = 8.0
        ),

        // ── North America ──────────────────────────────────────────────────────
        FareConfig(
            countryCode = "US",
            countryName = "United States",
            majorCities = listOf("New York", "Los Angeles", "Chicago", "San Francisco"),
            currency = CurrencyInfo("USD", "$", "US Dollar", 2),
            baseFare = 3.0,
            farePerKm = 1.5,
            farePerMinute = 0.4,
            minimumFare = 7.0,
            waitingTimeRatePerMinute = 0.35,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 4.0
        ),
        FareConfig(
            countryCode = "CA",
            countryName = "Canada",
            majorCities = listOf("Toronto", "Vancouver", "Montreal"),
            currency = CurrencyInfo("CAD", "C$", "Canadian Dollar", 2),
            baseFare = 3.5,
            farePerKm = 1.7,
            farePerMinute = 0.45,
            minimumFare = 8.0,
            waitingTimeRatePerMinute = 0.40,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 5.0
        ),
        FareConfig(
            countryCode = "MX",
            countryName = "Mexico",
            majorCities = listOf("Mexico City", "Guadalajara", "Monterrey"),
            currency = CurrencyInfo("MXN", "$", "Mexican Peso", 2),
            baseFare = 10.0,
            farePerKm = 6.0,
            farePerMinute = 1.5,
            minimumFare = 25.0,
            waitingTimeRatePerMinute = 1.0,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 20.0
        ),

        // ── South America ──────────────────────────────────────────────────────
        FareConfig(
            countryCode = "BR",
            countryName = "Brazil",
            majorCities = listOf("São Paulo", "Rio de Janeiro", "Brasília"),
            currency = CurrencyInfo("BRL", "R$", "Brazilian Real", 2),
            baseFare = 5.5,
            farePerKm = 3.2,
            farePerMinute = 0.75,
            minimumFare = 12.0,
            waitingTimeRatePerMinute = 0.60,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.15,
            airportSurcharge = 8.0
        ),
        FareConfig(
            countryCode = "AR",
            countryName = "Argentina",
            majorCities = listOf("Buenos Aires", "Cordoba"),
            currency = CurrencyInfo("ARS", "$", "Argentine Peso", 2),
            baseFare = 350.0,
            farePerKm = 150.0,
            farePerMinute = 40.0,
            minimumFare = 500.0,
            waitingTimeRatePerMinute = 30.0,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.15,
            airportSurcharge = 300.0
        ),
        FareConfig(
            countryCode = "CL",
            countryName = "Chile",
            majorCities = listOf("Santiago", "Valparaiso"),
            currency = CurrencyInfo("CLP", "$", "Chilean Peso", 0),
            baseFare = 600.0,
            farePerKm = 500.0,
            farePerMinute = 120.0,
            minimumFare = 1200.0,
            waitingTimeRatePerMinute = 80.0,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 800.0
        ),

        // ── Asia ───────────────────────────────────────────────────────────────
        FareConfig(
            countryCode = "IN",
            countryName = "India",
            majorCities = listOf("Mumbai", "Delhi", "Bengaluru", "Kolkata"),
            currency = CurrencyInfo("INR", "₹", "Indian Rupee", 2),
            baseFare = 40.0,
            farePerKm = 22.0,
            farePerMinute = 4.0,
            minimumFare = 60.0,
            waitingTimeRatePerMinute = 2.5,
            nightSurchargePercent = 0.25,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 80.0
        ),
        FareConfig(
            countryCode = "CN",
            countryName = "China",
            majorCities = listOf("Beijing", "Shanghai", "Guangzhou", "Shenzhen"),
            currency = CurrencyInfo("CNY", "¥", "Chinese Yuan", 2),
            baseFare = 13.0,
            farePerKm = 2.4,
            farePerMinute = 0.5,
            minimumFare = 14.0,
            waitingTimeRatePerMinute = 0.40,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 10.0
        ),
        FareConfig(
            countryCode = "JP",
            countryName = "Japan",
            majorCities = listOf("Tokyo", "Osaka", "Kyoto"),
            currency = CurrencyInfo("JPY", "¥", "Japanese Yen", 0),
            baseFare = 450.0,
            farePerKm = 280.0,
            farePerMinute = 70.0,
            minimumFare = 730.0,
            waitingTimeRatePerMinute = 60.0,
            nightSurchargePercent = 0.25,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 500.0
        ),
        FareConfig(
            countryCode = "SG",
            countryName = "Singapore",
            majorCities = listOf("Singapore"),
            currency = CurrencyInfo("SGD", "S$", "Singapore Dollar", 2),
            baseFare = 3.9,
            farePerKm = 1.1,
            farePerMinute = 0.3,
            minimumFare = 6.0,
            waitingTimeRatePerMinute = 0.25,
            nightSurchargePercent = 0.25,
            weekendSurchargePercent = 0.15,
            airportSurcharge = 8.0
        ),
        FareConfig(
            countryCode = "TH",
            countryName = "Thailand",
            majorCities = listOf("Bangkok", "Chiang Mai"),
            currency = CurrencyInfo("THB", "฿", "Thai Baht", 2),
            baseFare = 40.0,
            farePerKm = 6.5,
            farePerMinute = 1.2,
            minimumFare = 40.0,
            waitingTimeRatePerMinute = 0.80,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 50.0
        ),
        FareConfig(
            countryCode = "MY",
            countryName = "Malaysia",
            majorCities = listOf("Kuala Lumpur", "Penang"),
            currency = CurrencyInfo("MYR", "RM", "Malaysian Ringgit", 2),
            baseFare = 3.0,
            farePerKm = 1.4,
            farePerMinute = 0.4,
            minimumFare = 5.0,
            waitingTimeRatePerMinute = 0.30,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 5.0
        ),
        FareConfig(
            countryCode = "PH",
            countryName = "Philippines",
            majorCities = listOf("Manila", "Cebu"),
            currency = CurrencyInfo("PHP", "₱", "Philippine Peso", 2),
            baseFare = 45.0,
            farePerKm = 15.0,
            farePerMinute = 3.5,
            minimumFare = 45.0,
            waitingTimeRatePerMinute = 2.5,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 60.0
        ),
        FareConfig(
            countryCode = "KR",
            countryName = "South Korea",
            majorCities = listOf("Seoul", "Busan"),
            currency = CurrencyInfo("KRW", "₩", "South Korean Won", 0),
            baseFare = 3800.0,
            farePerKm = 1000.0,
            farePerMinute = 250.0,
            minimumFare = 3800.0,
            waitingTimeRatePerMinute = 200.0,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 6000.0
        ),
        FareConfig(
            countryCode = "ID",
            countryName = "Indonesia",
            majorCities = listOf("Jakarta", "Surabaya", "Bali"),
            currency = CurrencyInfo("IDR", "Rp", "Indonesian Rupiah", 0),
            baseFare = 7000.0,
            farePerKm = 4500.0,
            farePerMinute = 900.0,
            minimumFare = 10000.0,
            waitingTimeRatePerMinute = 700.0,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 8000.0
        ),
        FareConfig(
            countryCode = "VN",
            countryName = "Vietnam",
            majorCities = listOf("Ho Chi Minh City", "Hanoi"),
            currency = CurrencyInfo("VND", "₫", "Vietnamese Dong", 0),
            baseFare = 12000.0,
            farePerKm = 9000.0,
            farePerMinute = 2000.0,
            minimumFare = 20000.0,
            waitingTimeRatePerMinute = 1500.0,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.10,
            airportSurcharge = 15000.0
        ),

        // ── Oceania ────────────────────────────────────────────────────────────
        FareConfig(
            countryCode = "AU",
            countryName = "Australia",
            majorCities = listOf("Sydney", "Melbourne", "Brisbane"),
            currency = CurrencyInfo("AUD", "A$", "Australian Dollar", 2),
            baseFare = 3.6,
            farePerKm = 2.2,
            farePerMinute = 0.6,
            minimumFare = 7.0,
            waitingTimeRatePerMinute = 0.45,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.15,
            airportSurcharge = 6.0
        ),
        FareConfig(
            countryCode = "NZ",
            countryName = "New Zealand",
            majorCities = listOf("Auckland", "Wellington", "Christchurch"),
            currency = CurrencyInfo("NZD", "NZ$", "New Zealand Dollar", 2),
            baseFare = 3.0,
            farePerKm = 2.0,
            farePerMinute = 0.55,
            minimumFare = 6.0,
            waitingTimeRatePerMinute = 0.45,
            nightSurchargePercent = 0.20,
            weekendSurchargePercent = 0.15,
            airportSurcharge = 5.0
        )
        // You can easily extend this list with additional countries/cities.
    )

    fun findByCountryCode(code: String): FareConfig? =
        fares.firstOrNull { it.countryCode.equals(code, ignoreCase = true) }

    fun findByCountryOrCityName(name: String): FareConfig? {
        val lower = name.lowercase()
        return fares.firstOrNull { config ->
            config.countryName.lowercase().contains(lower) ||
                config.majorCities.any { it.lowercase().contains(lower) }
        }
    }
}

