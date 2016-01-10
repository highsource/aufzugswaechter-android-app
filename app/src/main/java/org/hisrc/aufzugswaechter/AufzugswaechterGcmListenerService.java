package org.hisrc.aufzugswaechter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GcmListenerService;

import org.hisrc.aufzugswaechter.gms.gcm.UnsubscribeFromTopicTask;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Set;

/**
 * Created by Alexey Valikov on 1/8/2016.
 */
public class AufzugswaechterGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String facilityEquipmentnumberAsString = data.getString("facilityEquipmentnumber", null);
        Long facilityEquipmentnumber = facilityEquipmentnumberAsString == null ? null : Long.valueOf(facilityEquipmentnumberAsString);
        String stationName = data.getString("stationname", "<unknown station>");
        String facilityType = data.getString("facilityType", "<unknown facility type>");
        String facilityDescription = data.getString("facilityDescription", facilityType + " " + facilityEquipmentnumber);
        String newFacilityState = data.getString("newFacilityState", "<unknown state>");

        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        final Set<String> subscribedTopics = sharedPreferences.getStringSet(AufzugswaechterPreferences.SUBSCRIBED_TOPICS, Collections.<String>emptySet());

        if (from.startsWith("/topics/")) {
            final String topic = from;
            if (!subscribedTopics.contains(topic)) {
                final String obsoleteTopic = topic;
                new UnsubscribeFromTopicTask(this).execute(from);
            } else {
                if (facilityEquipmentnumber != null) {
                    sendNotification(facilityEquipmentnumber, stationName, facilityDescription, newFacilityState);
                }
            }
        }
    }

    private void sendNotification(
            Long facilityEquipmentnumber,
            String stationName,
            String facilityDescription,
            String facilityState) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("facilityEquipmentnumber", facilityEquipmentnumber);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, facilityEquipmentnumber.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final String contentText = MessageFormat.format(getResources().getString(R.string.notification_content_text_format),
                facilityEquipmentnumber,
                stationName,
                facilityDescription,
                facilityState);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle(stationName)
                .setContentText(contentText)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent).setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_VIBRATE);
//        if (Build.VERSION.SDK_INT >= 21) notificationBuilder.setVibrate(new long[0]);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(facilityEquipmentnumber.hashCode(), notificationBuilder.build());
    }
}
