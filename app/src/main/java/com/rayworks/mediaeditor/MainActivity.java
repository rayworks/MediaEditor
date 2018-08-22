package com.rayworks.mediaeditor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.rayworks.mediaeditor.util.MediaFileEditor;

import java.io.File;
import java.util.Locale;

import javax.inject.Inject;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    @Inject MediaFileEditor mediaFileEditor;

    private boolean concatEnabled = true;
    private boolean openAutomatically = true;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        App.getInstance().getObjectGraph().inject(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        doMerge();
                    }
                });

        textView = findViewById(R.id.text);

        Switch togge = findViewById(R.id.toggle);
        togge.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        concatEnabled = isChecked;
                    }
                });

        Switch openSwitch = findViewById(R.id.auto_open);
        openSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        openAutomatically = isChecked;
                    }
                });

        checkPermission();
    }

    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0xFF);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void doMerge() {
        File sdcardFolder = Environment.getExternalStorageDirectory();
        final File outfile = new File(sdcardFolder, "output_test.m4a");

        String rootFolder = sdcardFolder.getPath();
        final String[] input =
                new String[] {
                    rootFolder + "/spring.m4a",
                    rootFolder + "/jp.m4a",
                    rootFolder + "/asr_demo0.m4a",
                    rootFolder + "/asr_demo.m4a"
                };

        final long startTm = System.currentTimeMillis();
        mediaFileEditor.mergeAudioFiles(
                concatEnabled,
                input,
                outfile,
                new MediaFileEditor.AudioMergeListener() {
                    @Override
                    public void onStarted() {
                        showDebugToast(">>> Merging Audios started");
                    }

                    @Override
                    public void onFailure(String s) {
                        showDebugToast(">>> Merging Audios failed : " + s);
                    }

                    @Override
                    public void onComplete() {
                        float timeConsumed = ((System.currentTimeMillis() - startTm) * 1.0f / 1000);

                        String msg =
                                String.format(
                                        Locale.ENGLISH,
                                        ">>> Editing Audio takes %.2f (s) for %d input files",
                                        timeConsumed,
                                        input.length);

                        Timber.w(msg);
                        textView.setText(msg);

                        showDebugToast(">>> Merging Audios completed");

                        if (openAutomatically) {
                            openMergedAudio(outfile);
                        }
                    }
                });
    }

    private void openMergedAudio(File outfile) {
        Uri uri = Uri.fromFile(outfile);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri =
                    FileProvider.getUriForFile(
                            MainActivity.this, getApplicationContext().getPackageName(), outfile);
        }
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "audio/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        MainActivity.this.startActivity(intent);
    }

    private void showDebugToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
