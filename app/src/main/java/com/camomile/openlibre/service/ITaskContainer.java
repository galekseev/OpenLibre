package com.camomile.openlibre.service;

public interface ITaskContainer {
    void onTaskSuccess(CloudStoreTask task);
    void onTaskError(CloudStoreTask task);
    void onTaskCancelled(CloudStoreTask task);
}
