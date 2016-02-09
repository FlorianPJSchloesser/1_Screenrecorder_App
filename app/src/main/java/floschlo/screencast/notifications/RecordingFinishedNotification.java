package floschlo.screencast.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.io.File;
import java.util.List;

import floschlo.screencast.R;

/**
 * Created by Florian on 21.12.2015.
 */
public class RecordingFinishedNotification {

    public final static String TAG = RecordingFinishedNotification.class.getSimpleName();

    public final static int ID = 2;

    public final static void createNotification(Context context, File videoFile) {
        Intent shareCommmonIntent = new Intent(Intent.ACTION_SEND);
        shareCommmonIntent.setDataAndType(Uri.fromFile(videoFile), "video/mpeg");
        Intent shareIntent = Intent.createChooser(shareCommmonIntent, "Share with");

        Intent playIntent = new Intent(Intent.ACTION_VIEW);
        playIntent.setDataAndType(Uri.fromFile(videoFile), "video/mpeg");
        // Verify it resolves
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(playIntent, 0);
        boolean isPlayIntentSafe = activities.size() > 0;

        PendingIntent playPendingIntent = null;
        PendingIntent sharePendingIntent = PendingIntent.getActivity(context, 1, shareIntent, PendingIntent.FLAG_ONE_SHOT);
        if (isPlayIntentSafe) {
            playPendingIntent = PendingIntent.getActivity(context, 1, playIntent, PendingIntent.FLAG_ONE_SHOT);
        }


        Notification notification = new Notification.Builder(context)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentTitle(context.getResources().getString(R.string.notification_finished_title))
                .setContentText(context.getResources().getString(R.string.notification_finished_content))
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setSmallIcon(R.drawable.ic_stat_recordfinished)
                        .addAction(R.drawable.ic_not_share, context.getResources().getString(R.string.action_share), sharePendingIntent)
                                .addAction(R.drawable.ic_not_play, context.getResources().getString(R.string.action_play), playPendingIntent).build();


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
