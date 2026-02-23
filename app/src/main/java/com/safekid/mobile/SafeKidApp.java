package com.safekid.mobile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

/**
 * Hilt Application class.
 * WorkManager'ı manuel başlatarak HiltWorkerFactory'yi kullanmasını sağlar.
 * Bu sayede @HiltWorker ile işaretli Worker sınıfları Hilt injection'ı alabilir.
 */
@HiltAndroidApp
public class SafeKidApp extends Application implements Configuration.Provider {

    @Inject
    HiltWorkerFactory workerFactory;

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build();
    }
}
