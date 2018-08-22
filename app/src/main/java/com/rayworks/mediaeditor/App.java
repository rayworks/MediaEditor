package com.rayworks.mediaeditor;

import android.app.Application;

import com.rayworks.mediaeditor.di.AppModule;
import com.rayworks.mediaeditor.util.MediaFileEditor;

import javax.inject.Inject;

import dagger.ObjectGraph;
import timber.log.Timber;

public class App extends Application {

    private static App sApp;

    @Inject MediaFileEditor fileEditor;
    private ObjectGraph objectGraph;

    public static App getInstance() {
        return sApp;
    }

    public ObjectGraph getObjectGraph() {
        return objectGraph;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        objectGraph = ObjectGraph.create(new AppModule(this));
        objectGraph.inject(this);

        sApp = this;

        // init the ffmpeg
        fileEditor.loadFFmpegBinary();

        Timber.plant(new Timber.DebugTree());
    }
}
