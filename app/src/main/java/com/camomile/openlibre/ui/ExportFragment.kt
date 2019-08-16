package com.camomile.openlibre.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.camomile.openlibre.R
import com.camomile.openlibre.service.ExportTask
import com.camomile.openlibre.service.ExportTask.exportDataAsync
import kotlinx.android.synthetic.main.fragment_export.*
import java.text.DateFormat
import java.util.*

class ExportFragment : DialogFragment() {

    enum class DataTypes(val localizedStringId: Int) {
        GLUCOSE(R.string.export_data_type_glucose),
        READING(R.string.export_data_type_reading),
        RAW(R.string.export_data_type_raw);

        lateinit var localizedString: String

        // return localized string
        override fun toString(): String {
            return localizedString
        }

        companion object {
            // resolve resource ids to localized strings when context is available
            fun init(context: Context) {
                DataTypes.values().map { it.localizedString = context.getString(it.localizedStringId) }
            }
        }
    }

    enum class OutputFormats {
        JSON, CSV
    }

    companion object {

        fun newInstance(): ExportFragment {
            val fragment = ExportFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }

    }

    private val dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    private lateinit var dialogView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        DataTypes.init(context!!)
        if (showsDialog) {
            return super.onCreateView(inflater, container, savedInstanceState)
        } else {
            return inflater.inflate(R.layout.fragment_export, container, false)
        }
    }

    // this is needed so the kotlin synthetic classes for the ui item work, because they rely on getView() returning the fragment's view
    override fun getView(): View?
    {
        if (super.getView() != null) {
            return super.getView()
        }
        return dialogView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView = activity!!.layoutInflater.inflate(R.layout.fragment_export, null)

        spinner_data_type.adapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, DataTypes.values())
        spinner_output_format.adapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, OutputFormats.values())

        button_export_or_cancel.setOnClickListener {
            if (ExportTask.isRunning) {

                ExportTask.isCancelled = true
                button_export_or_cancel.isEnabled = false

            } else {

                button_export_or_cancel.text = getString(R.string.pause)
                exportDataAsync(spinner_data_type.selectedItem as DataTypes, spinner_output_format.selectedItem as OutputFormats)

            }
        }

        ExportTask.registerCallbacks(this::finished, this::updateProgress)

        return AlertDialog.Builder(activity!!)
                .setTitle(R.string.export_data)
                .setView(dialogView)
                .create()
    }

    override fun onDestroyView() {
        ExportTask.unregisterCallbacks(this::finished, this::updateProgress)
        super.onDestroyView()
    }

    fun updateProgress(progress: Double, currentDate: Date?) {
        activity?.runOnUiThread {
            if (ExportTask.isRunning) {
                button_export_or_cancel.text = getString(R.string.cancel)
                pb_export.visibility = View.VISIBLE
                tv_export_status.visibility = View.VISIBLE
                pb_export.progress = (100 * progress).toInt()
                tv_export_status.text =
                        if (currentDate != null) String.format(getString(R.string.export_until), dateTimeFormat.format(currentDate))
                        else getString(R.string.exporting)
            } else {
                button_export_or_cancel.text = getString(R.string.export)
                pb_export.visibility = View.INVISIBLE
                if (tv_export_status.text == getString(R.string.exporting)) {
                    tv_export_status.visibility = View.INVISIBLE
                }
            }
        }
    }

    fun finished() {
        activity?.runOnUiThread {
            button_export_or_cancel.text = getString(R.string.export)
            button_export_or_cancel.isEnabled = true
            pb_export.visibility = View.INVISIBLE
            if (tv_export_status.text == getString(R.string.exporting)) {
                tv_export_status.visibility = View.INVISIBLE
            }
        }
    }

}
