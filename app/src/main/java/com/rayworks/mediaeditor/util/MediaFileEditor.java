package com.rayworks.mediaeditor.util;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.google.common.base.Preconditions;

import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import timber.log.Timber;

/** * An Utility for manipulating the media files. */
public class MediaFileEditor {
    private final FFmpeg ffmpeg;
    private final Context context;

    private boolean ffmpegSupported;

    public MediaFileEditor(@NonNull Context context, @NonNull FFmpeg ffmpeg) {
        this.ffmpeg = ffmpeg;
        this.context = context.getApplicationContext();
    }

    /**
     * * Whether this device is FFmpeg supported.
     *
     * @return
     */
    public boolean isFFmpegSupported() {
        return ffmpegSupported;
    }

    public void loadFFmpegBinary() {
        ffmpegSupported = true;

        try {
            getFFmpeg()
                    .loadBinary(
                            new LoadBinaryResponseHandler() {
                                @Override
                                public void onFailure() {
                                    showUnsupportedInfo();
                                }
                            });
        } catch (FFmpegNotSupportedException e) {
            e.printStackTrace();
            showUnsupportedInfo();
        }
    }

    private FFmpeg getFFmpeg() {
        return ffmpeg;
    }

    private void showUnsupportedInfo() {
        Timber.w(">>> FFmpeg not supported on this device.");
        ffmpegSupported = false;
    }

    public void execFFmpegBinary(final String[] command, final AudioMergeListener mergeListener) {
        final String cmdStr = Arrays.toString(command);

        if (!isFFmpegSupported()) {
            if (mergeListener != null) {
                mergeListener.onFailure("Not supported on this device.");
            }

            return;
        }

        try {
            getFFmpeg()
                    .execute(
                            command,
                            new ExecuteBinaryResponseHandler() {
                                @Override
                                public void onFailure(String s) {
                                    Timber.w("FAILED with output : " + s);

                                    if (mergeListener != null) {
                                        mergeListener.onFailure(s);
                                    }
                                }

                                @Override
                                public void onSuccess(String s) {
                                    Timber.i("SUCCESS with output : " + s);
                                }

                                @Override
                                public void onProgress(String s) {
                                    Timber.i("command progress : %s", s);
                                }

                                @Override
                                public void onStart() {
                                    Timber.d("Started command : ffmpeg %s", cmdStr);
                                    if (mergeListener != null) {
                                        mergeListener.onStarted();
                                    }
                                }

                                @Override
                                public void onFinish() {
                                    Timber.d("Finished command : ffmpeg %s", cmdStr);
                                    if (mergeListener != null) {
                                        mergeListener.onComplete();
                                    }
                                }
                            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
        }
    }

    /**
     * * Merges a set of recorded audio files into a single one.
     *
     * <p>NB: If the output file exists, it will be overridden.
     *
     * @see <a href
     *     >https://superuser.com/questions/1298891/ffmpeg-merge-multiple-audio-files-into-single-
     *     audio-file-with-android</a>
     * @param demuxerEnabled whether we just concat all the input files which have the same format.
     * @param inputFilePaths file full paths
     * @param output output file
     * @param listener {@link AudioMergeListener}
     */
    public void mergeAudioFiles(
            boolean demuxerEnabled,
            @NonNull String[] inputFilePaths,
            @NonNull File output,
            @Nullable AudioMergeListener listener) {

        Preconditions.checkNotNull(output);
        Preconditions.checkArgument(inputFilePaths.length > 0, "Input audio files are not valid");

        if (output.exists()) { // delete the old one if any
            output.delete();
        }

        int cnt = inputFilePaths.length;
        StringBuilder cmdBuilder = new StringBuilder();

        if (demuxerEnabled) { // the same codec, same sample rate ...
            // use the script: concat - Virtual concatenation script demuxer.
            // https://ffmpeg.org/ffmpeg-formats.html#Examples

            BufferedWriter bufferedWriter = null;
            File parent = Environment.getExternalStorageDirectory(); // context.getFilesDir();
            File mergingScript = new File(parent, "script.txt");

            try {
                bufferedWriter = new BufferedWriter(new FileWriter(mergingScript));

                for (String path : inputFilePaths) {
                    bufferedWriter.write("file");
                    bufferedWriter.write(" ");

                    bufferedWriter.write("'");
                    bufferedWriter.write(path);
                    bufferedWriter.write("'");

                    bufferedWriter.newLine();
                }
                bufferedWriter.flush();

            } catch (FileNotFoundException e) {
                onHandleException(listener, e);
                return;
            } catch (IOException e) {
                onHandleException(listener, e);
                return;
            } finally {
                IOUtils.closeQuietly(bufferedWriter);
            }

            cmdBuilder
                    .append("-f")
                    .append(" ")
                    .append("concat")
                    .append(" ")
                    .append("-safe")
                    .append(" ")
                    .append(0)
                    .append(" ");

            cmdBuilder.append("-i").append(" ").append(mergingScript.getAbsolutePath());
            cmdBuilder.append(" ").append("-c").append(" ").append("copy").append(" ");

            cmdBuilder.append(output.getAbsolutePath());
        } else {
            for (String str : inputFilePaths) {
                cmdBuilder.append("-i").append(" ").append(str).append(" ");
            }

            cmdBuilder.append("-filter_complex").append(" ");

            for (int i = 0; i < cnt; i++) {
                cmdBuilder.append(String.format(Locale.ENGLISH, "[%d:0]", i));
            }
            cmdBuilder.append(String.format(Locale.ENGLISH, "concat=n=%d:", cnt));

            cmdBuilder.append("v=0:a=1[out]").append(" ").append("-map [out]").append(" ");
            cmdBuilder.append(output.getAbsolutePath());
        }

        String cmdStr = cmdBuilder.toString();
        String[] command = cmdStr.split(" ");

        Timber.w(">>> Command string : %s", cmdStr);

        execFFmpegBinary(command, listener);
    }

    private void onHandleException(@Nullable AudioMergeListener listener, Exception e) {
        e.printStackTrace();

        if (listener != null) {
            listener.onFailure(e.getMessage());
        }
    }

    public interface AudioMergeListener {
        void onStarted();

        void onFailure(String s);

        void onComplete();
    }
}
