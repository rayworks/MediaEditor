package com.rayworks.mediaeditor.di;

import android.content.Context;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.rayworks.mediaeditor.App;
import com.rayworks.mediaeditor.MainActivity;
import com.rayworks.mediaeditor.util.MediaFileEditor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(injects = {MainActivity.class, App.class})
public class AppModule {
    private final Context context;

    public AppModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    MediaFileEditor provideMediaFileEditor() {
        return new MediaFileEditor(context, FFmpeg.getInstance(context.getApplicationContext()));
    }
}
