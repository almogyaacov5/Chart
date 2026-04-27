package com.example.chart;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * מחלקת עזר לתזמון בדיקות מחיר תקופתיות עם AlarmManager.
 * קוראים ל-schedule() כשרוצים להפעיל, ול-cancel() כשרוצים לעצור.
 */
public class PriceAlertScheduler {

    // מרווח בדיקה: 15 דקות
    private static final long INTERVAL_MS = 15 * 60 * 1000L;

    private static final String ACTION = "com.example.chart.CHECK_PRICE_ALERTS";
    private static final int REQUEST_CODE = 1001;

    /**
     * מתזמן בדיקת מחיר חוזרת כל 15 דקות.
     * מפעיל את PriceAlertReceiver גם כשהאפליקציה סגורה.
     */
    public static void schedule(Context context) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pendingIntent = buildPendingIntent(context);

        long triggerAtMillis = System.currentTimeMillis() + INTERVAL_MS;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // setExactAndAllowWhileIdle — מדויק גם במצב Doze
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        }
    }

    /**
     * מבטל את כל ה-Alarms הקיימים.
     */
    public static void cancel(Context context) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        alarmManager.cancel(buildPendingIntent(context));
    }

    private static PendingIntent buildPendingIntent(Context context) {
        Intent intent = new Intent(context, PriceAlertReceiver.class);
        intent.setAction(ACTION);
        return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}