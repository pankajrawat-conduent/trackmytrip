package com.example.trackmytrip.model

enum class TripType(val displayName: String) {
    Vacation("Vacation"),
    RoadTrip("Road Trip"),
    Business("Business"),
    Adventure("Adventure"),
    Other("Other")
}

enum class TripStatus(val displayName: String) {
    Active("Active"),
    Upcoming("Upcoming"),
    Completed("Completed"),
    Archived("Archived")
}

enum class TransportMode(val displayName: String) {
    Car("Car"),
    Motorcycle("Motorcycle"),
    CampervanRv("Campervan/RV"),
    OtherSelfDriven("Other Self-driven Vehicle"),
    Bus("Bus"),
    Train("Train"),
    MetroTram("Metro/Tram"),
    Flight("Flight"),
    Ferry("Ferry"),
    TaxiRideHail("Taxi/Ride-hail"),
    Bicycle("Bicycle"),
    Walk("Walk"),
    Other("Other")
}

enum class ExpenseCategory(val displayName: String) {
    Fuel("Fuel"),
    Food("Food"),
    Stay("Stay"),
    Tickets("Tickets"),
    Other("Other")
}

enum class LocationSource(val displayName: String) {
    Manual("Manual"),
    CurrentLocation("Current Location"),
    MapSelection("Map Selection")
}

enum class DistanceUnit(val displayName: String) {
    Km("km"),
    Mi("mi")
}

enum class DistanceSource(val displayName: String) {
    Odometer("Odometer"),
    Manual("Manual"),
    NotSet("Not set")
}

data class Trip(
    val id: Long,
    val title: String,
    val type: TripType,
    val destination: String,
    val startDate: String,
    val endDate: String?,
    val description: String,
    val status: TripStatus,
    val coverPhotoRef: String?,
    val vehicle: String?,
    val startOdometerKm: Int?,
    val endOdometerKm: Int?,
    val odometerUnit: DistanceUnit = DistanceUnit.Km,
    val manualDistance: Double? = null,
    val distanceSource: DistanceSource = DistanceSource.Odometer,
    val startOdometerPhotoRef: String? = null,
    val endOdometerPhotoRef: String? = null,
    val updates: List<JourneyUpdate>
) {
    val calculatedDistanceKm: Int?
        get() = if (startOdometerKm != null && endOdometerKm != null) {
            (endOdometerKm - startOdometerKm).coerceAtLeast(0)
        } else {
            null
        }

    val totalExpense: Double
        get() = updates.sumOf { it.expenseAmount ?: 0.0 }

    val displayDistance: String
        get() = when {
            manualDistance != null -> "${manualDistance.toInt()} ${odometerUnit.displayName} manual"
            calculatedDistanceKm != null -> "$calculatedDistanceKm ${odometerUnit.displayName} odometer"
            else -> "Distance not finalized"
        }
}

data class JourneyUpdate(
    val id: Long,
    val title: String,
    val timestamp: String,
    val placeName: String,
    val locationSource: LocationSource,
    val coordinateText: String?,
    val note: String,
    val photoRef: String?,
    val transportMode: TransportMode,
    val expenseAmount: Double?,
    val expenseCategory: ExpenseCategory?,
    val receiptRef: String?,
    val locationAccuracyMeters: Float? = null,
    val expenseCurrency: String = "INR",
    val expenseDate: String = timestamp,
    val expenseDescription: String? = null
)
