package org.hisrc.aufzugswaechter.gms.gcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.google.android.gms.gcm.GcmPubSub;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by Alexey Valikov on 1/8/2016.
 */
public class SubscribeToTopicTask extends AbstractToggleTopicSubscribtionTask {

    public SubscribeToTopicTask(Context context) {
        super(context);
    }

    @Override
    protected void toggleSubscription(GcmPubSub pubSub, String topic, String token) throws IOException {
        pubSub.subscribe(token, topic, null);
    }

    @Override
    protected void toggleTopic(String topic, Set<String> topics) {
        topics.add(topic);
    }
}