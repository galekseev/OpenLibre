package com.camomile.openlibre.ui;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.camomile.openlibre.model.AlgorithmUtil;
import com.camomile.openlibre.model.PredictionData;
import com.camomile.openlibre.model.ReadingData;

import java.util.Date;

import com.camomile.openlibre.R;
import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;

import static com.camomile.openlibre.OpenLibre.GLUCOSE_UNIT_IS_MMOL;
import static java.lang.Math.max;
import static java.lang.Math.min;

class LogRecyclerViewAdapter
        extends RealmRecyclerViewAdapter<ReadingData, LogRecyclerViewAdapter.LogRowViewHolder>
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String LOG_ID = "OpenLibre::" + LogRecyclerViewAdapter.class.getSimpleName();

    private final LogFragment fragment;

    LogRecyclerViewAdapter(LogFragment fragment, OrderedRealmCollection<ReadingData> data) {
        super(data, true);
        this.fragment = fragment;
        PreferenceManager.getDefaultSharedPreferences(fragment.getContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public LogRowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_log_row, parent, false);
        return new LogRowViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(LogRowViewHolder holder, int position) {
        ReadingData readingData;
        try {
            readingData = getData().get(position);
        } catch (NullPointerException e) {
            Log.e(LOG_ID, "Null pointer at position: " + position);
            holder.itemView.setVisibility(View.GONE);
            return;
        }
        if (readingData.getTrend().size() == 0) {
            Log.e(LOG_ID, "No trend data at position: " + position);
            holder.itemView.setVisibility(View.GONE);
            return;
        }
        holder.itemView.setVisibility(View.VISIBLE);
        PredictionData predictedGlucose = new PredictionData(readingData.getTrend());
        holder.readingData = readingData;
        holder.tv_date.setText(AlgorithmUtil.mFormatDate.format(new Date(readingData.getDate())));
        holder.tv_time.setText(AlgorithmUtil.mFormatTimeShort.format(new Date(readingData.getDate())));
        holder.tv_glucose.setText(predictedGlucose.glucoseData.glucoseString());
        if (GLUCOSE_UNIT_IS_MMOL) {
            holder.iv_unit.setImageResource(R.drawable.ic_unit_mmoll);
        } else {
            holder.iv_unit.setImageResource(R.drawable.ic_unit_mgdl);
        }
        //holder.iv_predictionArrow.setImageResource(trendArrowMap.get(getTrendArrow(predictedGlucose)));
        float rotationDegrees = -90f * max(-1f, min(1f, (float) (predictedGlucose.glucoseSlopeRaw / AlgorithmUtil.TREND_UP_DOWN_LIMIT)));
        holder.iv_predictionArrow.setRotation(rotationDegrees);
        // reduce trend arrow visibility according to prediction confidence
        holder.iv_predictionArrow.setAlpha((float) min(1, 0.1 + predictedGlucose.confidence()));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_glucose_unit_is_mmol")) {
            notifyDataSetChanged();
        }
    }

    class LogRowViewHolder
            extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener  {
        TextView tv_date;
        TextView tv_time;
        TextView tv_glucose;
        ImageView iv_unit;
        ImageView iv_predictionArrow;
        ReadingData readingData;

        LogRowViewHolder(View view) {
            super(view);
            tv_date = (TextView) view.findViewById(R.id.tv_log_date);
            tv_time = (TextView) view.findViewById(R.id.tv_log_time);
            tv_glucose = (TextView) view.findViewById(R.id.tv_log_glucose);
            iv_unit = (ImageView) view.findViewById(R.id.iv_log_unit);
            iv_predictionArrow = (ImageView) view.findViewById(R.id.iv_log_prediction);
            view.setOnClickListener(this);

            // enable context menu only in developer mode
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(fragment.getContext());
            boolean developerMode = settings.getBoolean("pref_developer_mode", false);
            if (developerMode) {
                view.setOnCreateContextMenuListener(this);
            }
        }

        @Override
        public void onClick(View view) {
            fragment.showScanData(readingData);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
            MenuInflater inflater = new MenuInflater(itemView.getContext());
            inflater.inflate(R.menu.menu_log_item_context, menu);
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setOnMenuItemClickListener(this);
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            int id = item.getItemId();
            switch (id) {
                case R.id.action_show_scan:
                    fragment.showScanData(readingData);
                    return true;
                case R.id.action_delete_scan:
                    fragment.deleteScanData(readingData);
                    return true;
            }
            return false;
        }
    }
}
