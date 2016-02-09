package floschlo.screencast.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.io.File;
import java.util.List;

/**
 * Created by Florian on 06.02.2016.
 */
public class IntentUtils {

    public final static String TAG = IntentUtils.class.getSimpleName();

    public static void playVideo (Context context, Uri videoUri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(videoUri, "video/*");
        // Verify it resolves
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        boolean isIntentSafe = activities.size() > 0;

        // Start an activity if it's safe
        if (isIntentSafe) {
            context.startActivity(intent);
        }
    }

    public static void shareViedeo (Context context, Uri videoUri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setDataAndType(videoUri, "video/*");

        Intent chooser = Intent.createChooser(intent, "Share with");
        context.startActivity(chooser);
    }

}
