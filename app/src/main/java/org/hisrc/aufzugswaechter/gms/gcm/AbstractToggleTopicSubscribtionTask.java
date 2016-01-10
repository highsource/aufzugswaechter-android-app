package org.hisrc.aufzugswaechter.gms.gcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.google.android.gms.gcm.GcmPubSub;

import org.hisrc.aufzugswaechter.AufzugswaechterPreferences;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by Alexey Valikov on 1/8/2016.
 */
public abstract class AbstractToggleTopicSubscribtionTask extends AsyncTask<String, Void, String[]> {
    protected final Context context;

    public AbstractToggleTopicSubscribtionTask(Context context) {
        this.context = context;
    }

    protected Context getContext() {
        return context;
    }

    @Override
    protected String[] doInBackground(String... topics) {
        final GcmPubSub pubSub = GcmPubSub.getInstance(getContext());
        try {
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getContext());
            final String token = sharedPreferences.getString(AufzugswaechterPreferences.TOKEN, null);
            if (token != null) {
                for (String topic : topics) {
                    toggleSubscription(pubSub, topic, token);
                }
                return topics;
            } else {
                return null;
            }
        } catch (IOException ioex) {
            // TODO error reporting
            ioex.printStackTrace();
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    protected abstract void toggleSubscription(GcmPubSub pubSub, String topic, String token) throws IOException;

    @Override
    protected void onPostExecute(String[] topics) {
        super.onPostExecute(topics);
        if (topics != null) {
            SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(getContext());
            final Set<String> subscribedTopics = new TreeSet<String>(preferences.getStringSet(AufzugswaechterPreferences.SUBSCRIBED_TOPICS, Collections.<String>emptySet()));
            for (String topic : topics)
            {
                toggleTopic(topic, subscribedTopics);
            }
            preferences.edit().putStringSet(AufzugswaechterPreferences.SUBSCRIBED_TOPICS, subscribedTopics).apply();
        }

    }

    protected abstract void toggleTopic(String topic, Set<String> topics);
}
