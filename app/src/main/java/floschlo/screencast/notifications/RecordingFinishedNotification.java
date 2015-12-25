package floschlo.screencast.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.util.Log;

import java.io.File;
import java.util.List;

import floschlo.screencast.BuildConfig;
import floschlo.screencast.R;
import floschlo.screencast.activity.MainActivity;

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
                .setContentTitle("Record finished")
                .setContentText("Video file saved")
                .setColor(context.getResources().getColor(R.color.colorPrimary))
                .setSmallIcon(R.drawable.ic_stat_recordfinished)
                .addAction(R.drawable.ic_action_share, "Share", sharePendingIntent)
                .addAction(R.drawable.ic_action_play, "Play", playPendingIntent).build();


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
