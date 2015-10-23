package com.chickenkiller.upods2.controllers.app;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.chickenkiller.upods2.controllers.internet.DownloadMaster;
import com.chickenkiller.upods2.controllers.player.UniversalPlayer;

/**
 * Created by alonzilberman on 7/31/15.
 */
public class MainBroadcastRecivier extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("MainBroadcastRecivier", intent.getAction());
        if (intent.getAction() == UniversalPlayer.INTENT_ACTION_PLAY) {
            if (UniversalPlayer.getInstance().isPrepaired) {
                UniversalPlayer.getInstance().start();
            }
        } else if (intent.getAction() == UniversalPlayer.INTENT_ACTION_PAUSE) {
            if (UniversalPlayer.getInstance().isPrepaired) {
                UniversalPlayer.getInstance().pause();
            }
        } else if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            DownloadMaster.getInstance().markDownloadTaskFinished(downloadId);
        }
    }
}