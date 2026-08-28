package com.example.trackmytrip.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.trackmytrip.model.DistanceSource
import com.example.trackmytrip.model.DistanceUnit
import com.example.trackmytrip.model.ExpenseCategory
import com.example.trackmytrip.model.JourneyUpdate
import com.example.trackmytrip.model.LocationSource
import com.example.trackmytrip.model.TransportMode
import com.example.trackmytrip.model.Trip
import com.example.trackmytrip.model.TripStatus
import com.example.trackmytrip.model.TripType

class TripStore(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    fun loadTrips(): List<Trip> {
        val db = readableDatabase
        val trips = mutableListOf<Trip>()
        db.query(TABLE_TRIPS, null, null, null, null, null, "id DESC").use { cursor ->
            while (cursor.moveToNext()) {
                val tripId = cursor.getLongValue("id")
                trips += Trip(
                    id = tripId,
                    title = cursor.getStringValue("title"),
                    type = enumValueOrDefault(cursor.getStringValue("type"), TripType.Other),
                    destination = cursor.getStringValue("destination"),
                    startDate = cursor.getStringValue("start_date"),
                    endDate = cursor.getNullableString("end_date"),
                    description = cursor.getStringValue("description"),
                    status = enumValueOrDefault(cursor.getStringValue("status"), TripStatus.Active),
                    coverPhotoRef = cursor.getNullableString("cover_photo_ref"),
                    vehicle = cursor.getNullableString("vehicle"),
                    startOdometerKm = cursor.getNullableInt("start_odometer"),
                    endOdometerKm = cursor.getNullableInt("end_odometer"),
                    odometerUnit = enumValueOrDefault(cursor.getStringValue("odometer_unit"), DistanceUnit.Km),
                    manualDistance = cursor.getNullableDouble("manual_distance"),
                    distanceSource = enumValueOrDefault(cursor.getStringValue("distance_source"), DistanceSource.Odometer),
                    startOdometerPhotoRef = cursor.getNullableString("start_odometer_photo_ref"),
                    endOdometerPhotoRef = cursor.getNullableString("end_odometer_photo_ref"),
                    updates = loadUpdates(db, tripId)
                )
            }
        }
        return trips
    }

    fun saveTrips(trips: List<Trip>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_UPDATES, null, null)
            db.delete(TABLE_TRIPS, null, null)
            trips.forEach { trip ->
                db.insertOrThrow(TABLE_TRIPS, null, trip.toContentValues())
                trip.updates.forEach { update ->
                    db.insertOrThrow(TABLE_UPDATES, null, update.toContentValues(trip.id))
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun clear() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_UPDATES, null, null)
            db.delete(TABLE_TRIPS, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_TRIPS (
                id INTEGER PRIMARY KEY,
                title TEXT NOT NULL,
                type TEXT NOT NULL,
                destination TEXT NOT NULL,
                start_date TEXT NOT NULL,
                end_date TEXT,
                description TEXT NOT NULL,
                status TEXT NOT NULL,
                cover_photo_ref TEXT,
                vehicle TEXT,
                start_odometer INTEGER,
                end_odometer INTEGER,
                odometer_unit TEXT NOT NULL,
                manual_distance REAL,
                distance_source TEXT NOT NULL,
                start_odometer_photo_ref TEXT,
                end_odometer_photo_ref TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_UPDATES (
                id INTEGER PRIMARY KEY,
                trip_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                place_name TEXT NOT NULL,
                location_source TEXT NOT NULL,
                coordinate_text TEXT,
                location_accuracy_meters REAL,
                note TEXT NOT NULL,
                photo_ref TEXT,
                transport_mode TEXT NOT NULL,
                expense_amount REAL,
                expense_category TEXT,
                expense_currency TEXT NOT NULL,
                expense_date TEXT NOT NULL,
                expense_description TEXT,
                receipt_ref TEXT,
                FOREIGN KEY(trip_id) REFERENCES $TABLE_TRIPS(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_UPDATES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRIPS")
        onCreate(db)
    }

    private fun loadUpdates(db: SQLiteDatabase, tripId: Long): List<JourneyUpdate> {
        val updates = mutableListOf<JourneyUpdate>()
        db.query(TABLE_UPDATES, null, "trip_id = ?", arrayOf(tripId.toString()), null, null, "id DESC").use { cursor ->
            while (cursor.moveToNext()) {
                updates += JourneyUpdate(
                    id = cursor.getLongValue("id"),
                    title = cursor.getStringValue("title"),
                    timestamp = cursor.getStringValue("timestamp"),
                    placeName = cursor.getStringValue("place_name"),
                    locationSource = enumValueOrDefault(cursor.getStringValue("location_source"), LocationSource.Manual),
                    coordinateText = cursor.getNullableString("coordinate_text"),
                    note = cursor.getStringValue("note"),
                    photoRef = cursor.getNullableString("photo_ref"),
                    transportMode = enumValueOrDefault(cursor.getStringValue("transport_mode"), TransportMode.Other),
                    expenseAmount = cursor.getNullableDouble("expense_amount"),
                    expenseCategory = cursor.getNullableString("expense_category")?.let { enumValueOrDefault(it, ExpenseCategory.Other) },
                    receiptRef = cursor.getNullableString("receipt_ref"),
                    locationAccuracyMeters = cursor.getNullableFloat("location_accuracy_meters"),
                    expenseCurrency = cursor.getStringValue("expense_currency"),
                    expenseDate = cursor.getStringValue("expense_date"),
                    expenseDescription = cursor.getNullableString("expense_description")
                )
            }
        }
        return updates
    }

    private fun Trip.toContentValues() = ContentValues().apply {
        put("id", id)
        put("title", title)
        put("type", type.name)
        put("destination", destination)
        put("start_date", startDate)
        put("end_date", endDate)
        put("description", description)
        put("status", status.name)
        put("cover_photo_ref", coverPhotoRef)
        put("vehicle", vehicle)
        put("start_odometer", startOdometerKm)
        put("end_odometer", endOdometerKm)
        put("odometer_unit", odometerUnit.name)
        put("manual_distance", manualDistance)
        put("distance_source", distanceSource.name)
        put("start_odometer_photo_ref", startOdometerPhotoRef)
        put("end_odometer_photo_ref", endOdometerPhotoRef)
    }

    private fun JourneyUpdate.toContentValues(tripId: Long) = ContentValues().apply {
        put("id", id)
        put("trip_id", tripId)
        put("title", title)
        put("timestamp", timestamp)
        put("place_name", placeName)
        put("location_source", locationSource.name)
        put("coordinate_text", coordinateText)
        put("location_accuracy_meters", locationAccuracyMeters)
        put("note", note)
        put("photo_ref", photoRef)
        put("transport_mode", transportMode.name)
        put("expense_amount", expenseAmount)
        put("expense_category", expenseCategory?.name)
        put("expense_currency", expenseCurrency)
        put("expense_date", expenseDate)
        put("expense_description", expenseDescription)
        put("receipt_ref", receiptRef)
    }

    private fun android.database.Cursor.getStringValue(column: String): String = getString(getColumnIndexOrThrow(column))

    private fun android.database.Cursor.getLongValue(column: String): Long = getLong(getColumnIndexOrThrow(column))

    private fun android.database.Cursor.getNullableString(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }

    private fun android.database.Cursor.getNullableInt(column: String): Int? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getInt(index)
    }

    private fun android.database.Cursor.getNullableDouble(column: String): Double? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getDouble(index)
    }

    private fun android.database.Cursor.getNullableFloat(column: String): Float? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getFloat(index)
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T {
        return enumValues<T>().firstOrNull { it.name == value } ?: default
    }

    private companion object {
        const val DATABASE_NAME = "track_my_trip.db"
        const val DATABASE_VERSION = 1
        const val TABLE_TRIPS = "trips"
        const val TABLE_UPDATES = "journey_updates"
    }
}
