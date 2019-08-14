package de.dorianscholz.openlibre.service

import android.util.Log
import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import de.dorianscholz.openlibre.OpenLibre.*
import de.dorianscholz.openlibre.model.GlucoseData
import de.dorianscholz.openlibre.model.RawTagData
import de.dorianscholz.openlibre.model.ReadingData
import de.dorianscholz.openlibre.model.ReadingData.numHistoryValues
import de.dorianscholz.openlibre.model.ReadingData.numTrendValues
import de.dorianscholz.openlibre.ui.ExportFragment
import io.realm.Realm
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
//import kotlinx.coroutines.experimental.CommonPool
//import kotlinx.coroutines.experimental.android.UI
//import kotlinx.coroutines.experimental.async
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.lang.Math.abs
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object ExportTask {

    private val LOG_ID = "OpenLibre::" + ExportTask::class.java.simpleName

    private val finishedCallbacks = ArrayList<() -> Unit>()
    private val progressCallbacks = ArrayList<(Double, Date?) -> Unit>()

    var isRunning = false
    var isCancelled = false

    fun registerCallbacks(finished: () -> Unit, progress: (Double, Date?) -> Unit) {
        finishedCallbacks += finished
        progressCallbacks += progress
    }

    fun unregisterCallbacks(finished: () -> Unit, progress: (Double, Date?) -> Unit) {
        finishedCallbacks -= finished
        progressCallbacks -= progress
    }

    private fun notifyProgress(progress: Double, date: Date?) {
        progressCallbacks.forEach { it(progress, date) }
    }

    private fun notifyFinished() {
        finishedCallbacks.forEach { it() }
    }

    fun exportDataAsync(dataType: ExportFragment.DataTypes, outputFormat: ExportFragment.OutputFormats) = GlobalScope.async ( Dispatchers.Main ) {
        try {
            isRunning = true
            isCancelled = false
            val job = GlobalScope.async (Dispatchers.Default) {
                when (dataType) {
                    ExportFragment.DataTypes.RAW ->
                        Realm.getInstance(realmConfigRawData).use { realm ->
                            exportEntries<RawTagData>("raw-data", outputFormat, realm,
                                    realm.where(RawTagData::class.java)
                                            .findAllSorted(RawTagData.DATE, Sort.ASCENDING))
                        }

                    ExportFragment.DataTypes.READING ->
                        Realm.getInstance(realmConfigProcessedData).use { realm ->
                            exportEntries<ReadingData>("decoded-data", outputFormat, realm,
                                    realm.where(ReadingData::class.java)
                                            .findAllSorted(ReadingData.DATE, Sort.ASCENDING))
                        }

                    ExportFragment.DataTypes.GLUCOSE ->
                        Realm.getInstance(realmConfigProcessedData).use { realm ->
                            exportEntries<GlucoseData>("glucose-data", outputFormat, realm,
                                    realm.where(GlucoseData::class.java)
                                            .equalTo(GlucoseData.IS_TREND_DATA, false)
                                            .findAllSorted(GlucoseData.DATE, Sort.ASCENDING))
                        }
                }
            }
            job.await()
        }
        catch (e: Exception) {
            Log.e(LOG_ID, "Error in exportDataAsync: " + e.toString())
        }
        finally {
            isRunning = false
            notifyFinished()
        }
    }

    inline private suspend fun <reified T: RealmObject> exportEntries(
            dataType: String, outputFormat: ExportFragment.OutputFormats, realm: Realm, realmResults: RealmResults<T>) {
        when (outputFormat) {
            ExportFragment.OutputFormats.JSON -> exportEntriesJson(dataType, realm, realmResults)
            ExportFragment.OutputFormats.CSV -> exportEntriesCsv(dataType, realmResults)
        }
    }

    inline private suspend fun <reified T: RealmObject> exportEntriesJson(dataType: String, realm: Realm, realmResults: RealmResults<T>) {
        notifyProgress(0.0, null)
        try {
            val jsonFile = File(openLibreDataPath, "openlibre-export-%s.json".format(dataType))
            val writer = JsonWriter(FileWriter(jsonFile))
            writer.setIndent("  ")
            writer.beginObject()
            writer.name(T::class.java.simpleName)
            writer.beginArray()

            val gson = Gson()
            var count = 0
            for (data in realmResults) {
                gson.toJson(realm.copyFromRealm(data), T::class.java, writer)

                count++
                if (count % 10 == 0) {
                    val progress = count.toDouble() / realmResults.size
                    // this is an ugly way to resolve date, but there is no common base class, due to missing support in Realm
                    var date: Date? = null
                    if (data is GlucoseData) {
                        date = Date(data.date)
                    } else if (data is ReadingData) {
                        date = Date(data.date)
                    } else if (data is RawTagData) {
                        date = Date(data.date)
                    }
                    notifyProgress(progress, date)
                }
                if (isCancelled) break
            }

            writer.endArray()
            writer.endObject()
            writer.flush()
            writer.close()

        } catch (e: IOException) {
            Log.e(LOG_ID, "exportEntriesJson: error: " + e.toString())
            e.printStackTrace()
        }
    }

    inline private suspend fun <reified T: RealmObject> exportEntriesCsv(dataType: String, realmResults: RealmResults<T>) {
        notifyProgress(0.0, null)
        if (realmResults.size == 0) {
            return
        }
        var csvSeparator = ','
        if (java.text.DecimalFormatSymbols.getInstance().decimalSeparator == csvSeparator) {
            csvSeparator = ';'
        }
        try {
            File(openLibreDataPath, "openlibre-export-%s.csv".format(dataType)).printWriter().use { csvFile ->
                var headerList = listOf("id", "timezone", "date")
                if (realmResults[0] is GlucoseData) {
                    headerList += listOf("glucose [%s]".format(GlucoseData.getDisplayUnit()))

                } else if (realmResults[0] is ReadingData) {
                    headerList += listOf("ageInSensorMinutes")
                    headerList += (1..numTrendValues).map{ "trend %d [%s]".format(it, GlucoseData.getDisplayUnit()) }
                    headerList += (1..numHistoryValues).map{ "history %d [%s]".format(it, GlucoseData.getDisplayUnit()) }

                } else if (realmResults[0] is RawTagData) {
                    headerList += listOf("rawDataHex")
                }
                csvFile.println(headerList.joinToString(csvSeparator.toString()))

                var count = 0
                for (data in realmResults) {

                    var date: Date? = null
                    var dataList = emptyList<Any>()
                    if (data is GlucoseData) {
                        date = Date(data.date)

                        dataList += data.id
                        dataList += getTimezoneName(data.timezoneOffsetInMinutes)
                        dataList += formatDateTimeWithoutTimezone(date, data.timezoneOffsetInMinutes)

                        dataList += data.glucose()

                    } else if (data is ReadingData) {
                        date = Date(data.date)

                        dataList += data.id
                        dataList += getTimezoneName(data.timezoneOffsetInMinutes)
                        dataList += formatDateTimeWithoutTimezone(date, data.timezoneOffsetInMinutes)

                        dataList += data.sensorAgeInMinutes
                        dataList += data.trend.map { it.glucose() }
                        dataList += DoubleArray(numTrendValues - data.trend.size).asList()
                        dataList += data.history.map { it.glucose() }
                        dataList += DoubleArray(numHistoryValues - data.history.size).asList()

                    } else if (data is RawTagData) {
                        date = Date(data.date)

                        dataList += data.id
                        dataList += getTimezoneName(data.timezoneOffsetInMinutes)
                        dataList += formatDateTimeWithoutTimezone(date, data.timezoneOffsetInMinutes)

                        dataList += data.data.map{ "%02x".format(it) }.joinToString("")
                    }
                    csvFile.println(dataList.joinToString(csvSeparator.toString()))

                    count++
                    if (count % 10 == 0) {
                        val progress = count.toDouble() / realmResults.size
                        notifyProgress(progress, date)
                    }
                    if (isCancelled) break
                }
            }
        } catch (e: IOException) {
            Log.e(LOG_ID, "exportEntriesJson: error: " + e.toString())
            e.printStackTrace()
        }
    }

    private fun formatDateTimeWithoutTimezone(date: Date, timezoneOffsetInMinutes: Int): String {
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        df.timeZone = TimeZone.getTimeZone(getTimezoneName(timezoneOffsetInMinutes))
        return df.format(date)
    }

    private fun getTimezoneName(timezoneOffsetInMinutes: Int): String {
        val offsetSign = "%+d".format(timezoneOffsetInMinutes)[0]

        val of = SimpleDateFormat("HH:mm", Locale.US)
        of.timeZone = TimeZone.getTimeZone("UTC")
        val timezoneOffsetString = of.format(Date(TimeUnit.MINUTES.toMillis(abs(timezoneOffsetInMinutes).toLong())))

        return "GMT" + offsetSign + timezoneOffsetString
    }

}
