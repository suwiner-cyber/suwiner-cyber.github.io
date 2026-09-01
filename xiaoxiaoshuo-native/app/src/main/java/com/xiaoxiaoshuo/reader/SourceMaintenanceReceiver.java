package com.xiaoxiaoshuo.reader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SourceMaintenanceReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? "" : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            SourceMaintenance.schedule(context);
            SourceMaintenance.runIfDue(context);
            return;
        }
        if (SourceMaintenance.ACTION_DAILY.equals(action)) {
            final PendingResult pending = goAsync();
            SourceMaintenance.runAsync(context, true, () -> pending.finish());
        }
    }
}
