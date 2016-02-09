package floschlo.screencast.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import floschlo.screencast.R;
import floschlo.screencast.activity.MainActivity;

/**
 * Created by Florian on 19.12.2015.
 */
public class RecordingNotification {

    public final static String TAG = RecordingNotification.class.getSimpleName();

    public final static int ID = 1;

    public final static void createNotification(Context context) {
        Intent discardIntent = new Intent(context, MainActivity.class);
        discardIntent.setAction(MainActivity.ACTION_REC_DISCARD);
        discardIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Intent stopIntent = new Intent(context, MainActivity.class);
        stopIntent.setAction(MainActivity.ACTION_REC_STOP);
        stopIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);


        PendingIntent discardPendingIntent = PendingIntent.getActivity(context, 1, discardIntent, PendingIntent.FLAG_ONE_SHOT);
        PendingIntent stopPendingIntent = PendingIntent.getActivity(context, 1, stopIntent, PendingIntent.FLAG_ONE_SHOT);

        Notification notification = new Notification.Builder(context)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle(context.getResources().getString(R.string.notification_recording_title))
                .setOngoing(true)
                .setUsesChronometer(true)
                .setContentText(context.getResources().getString(R.string.notification_recording_content))
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setSmallIcon(R.drawable.ic_stat_recording)
                .addAction(R.drawable.ic_not_discard, context.getResources().getString(R.string.action_discard), discardPendingIntent)
                .addAction(R.drawable.ic_not_stop, context.getResources().getString(R.string.action_stop), stopPendingIntent).build();


        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(ID, notification);
    }

    public static void cancel(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(ID);
    }

}
