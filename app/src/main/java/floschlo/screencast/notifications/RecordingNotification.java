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

    public final static void createNotification (Context context) {
        Intent discardIntent = new Intent(context, MainActivity.class);
        discardIntent.setAction(MainActivity.ACTION_REC_DISCARD);
        discardIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Intent stopIntent = new Intent(context, MainActivity.class);
        stopIntent.setAction(MainActivity.ACTION_REC_STOP);
        discardIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);


        PendingIntent discardPendingIntent = PendingIntent.getActivity(context, 1, discardIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stoptPendingIntent = PendingIntent.getActivity(context, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder(context)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setContentTitle("Screencast")
                .setOngoing(true)
                .setContentText("Floschlo screencast is recording")
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setSmallIcon(R.drawable.ic_stat_recording)
                .addAction(R.drawable.ic_not_discard, "Discard", discardPendingIntent)
                .addAction(R.drawable.ic_not_stop, "Stop", stoptPendingIntent).build();


        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

    public static void cancel (Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(ID);
    }

}
