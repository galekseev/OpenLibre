package de.dorianscholz.openlibre.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.dorianscholz.openlibre.R;
import de.dorianscholz.openlibre.model.ReadingData;
import io.realm.Realm;
import io.realm.Sort;

import static de.dorianscholz.openlibre.OpenLibre.realmConfigProcessedData;

public class LogFragment extends Fragment {
    OnScanDataListener mCallback;

    // Container Activity must implement this interface
    public interface OnScanDataListener {
        void onShowScanData(ReadingData readingData);
    }

    private Realm mRealmProcessedData;

    public LogFragment() {
    }

    @SuppressWarnings("unused")
    public static LogFragment newInstance() {
        return new LogFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mCallback = (OnScanDataListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnScanDataListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRealmProcessedData = Realm.getInstance(realmConfigProcessedData);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealmProcessedData.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(new LogRecyclerViewAdapter(this,
                    mRealmProcessedData
                            .where(ReadingData.class)
                            .isNotEmpty(ReadingData.TREND)
                            .sort(ReadingData.DATE, Sort.DESCENDING)
                            .findAllAsync()
            ));
            recyclerView.setHasFixedSize(true);
            recyclerView.addItemDecoration(
                    new DividerItemDecoration(this.getContext(), DividerItemDecoration.VERTICAL_LIST)
            );
            registerForContextMenu(recyclerView);
        }
        return view;
    }

    public void showScanData(final ReadingData readingData) {
        mCallback.onShowScanData(readingData);
    }

    public void deleteScanData(final ReadingData readingData) {
        mRealmProcessedData.beginTransaction();
        readingData.getHistory().deleteAllFromRealm();
        readingData.getTrend().deleteAllFromRealm();
        readingData.deleteFromRealm();
        mRealmProcessedData.commitTransaction();
    }

}
