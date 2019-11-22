package com.camomile.openlibre.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.camomile.openlibre.model.ReadingData;

import com.camomile.openlibre.R;
import com.camomile.openlibre.service.CloudStoreSynchronization;

import java.util.Date;

import io.realm.Realm;
import io.realm.Sort;

import static com.camomile.openlibre.OpenLibre.realmConfigProcessedData;

public class LogFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    OnScanDataListener mCallback;
    private SwipeRefreshLayout mSwipeRefreshLayout;

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

        mSwipeRefreshLayout = view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        // Set the adapter
        Context context = view.getContext();
        RecyclerView recyclerView = view.findViewById(R.id.log_list_recycle_view);
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
//        }
        return view;
    }

    @Override
    public void onRefresh() {
        final CloudStoreSynchronization sync = CloudStoreSynchronization.getInstance();
        sync.registerProgressUpdateCallback(new CloudStoreSynchronization.ProgressCallBack() {
            @Override
            public void updateProgress(float progress, Date currentDate) { }
            @Override
            public void finished() {
                mSwipeRefreshLayout.setRefreshing(false);
                sync.unregisterProgressUpdateCallback();
            }
        });
        sync.startTriggeredDownload(getActivity().getApplicationContext());
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
