package org.hisrc.aufzugswaechter.gms.gcm;

import android.content.Context;

import com.google.android.gms.gcm.GcmPubSub;

import java.io.IOException;
import java.util.Set;

/**
 * Created by Alexey Valikov on 1/8/2016.
 */
public class UnsubscribeFromTopicTask extends AbstractToggleTopicSubscribtionTask {

    public UnsubscribeFromTopicTask(Context context) {
        super(context);
    }

    @Override
    protected void toggleSubscription(GcmPubSub pubSub, String topic, String token) throws IOException {
        pubSub.unsubscribe(token, topic);
    }

    @Override
    protected void toggleTopic(String topic, Set<String> topics) {
        topics.remove(topic);
    }
}