package org.hisrc.aufzugswaechter;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.hisrc.aufzugswaechter.gms.gcm.SubscribeToTopicTask;
import org.hisrc.aufzugswaechter.gms.gcm.UnsubscribeFromTopicTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapActivity extends
        //FragmentActivity
        AppCompatActivity

        implements OnMapReadyCallback, GoogleMap.OnInfoWindowLongClickListener {

    private static final String LOG_TAG = "AufzugswaechterApp";

    private Toolbar toolbar;
    private GoogleMap mMap;

    private BroadcastReceiver mRegistrationBroadcastReceiver;

    private Long initialFacilityEquipmentnumber = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final Long fen = extras.getLong("facilityEquipmentnumber", Long.MIN_VALUE);
            if (fen != Long.MIN_VALUE) {
                initialFacilityEquipmentnumber = fen;
            } else {
                initialFacilityEquipmentnumber = null;
            }

        }

        setContentView(R.layout.activity_map);
        toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(AufzugswaechterPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    // mInformationTextView.setText(getString(R.string.gcm_send_message));
                } else {
                    // mInformationTextView.setText(getString(R.string.token_error_message));
                }
            }
        };
        {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, AufzugswaechterRegistrationIntentService.class);
            startService(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_scan_facility_qr_code:
                scanQRCode();
                return true;
            case R.id.menu_item_subscribe_to_all_updates:
                subscribeToAllUpdates();
                return true;
            case R.id.menu_item_unsubscribe_from_all_updates:
                unsubscribeFromAllUpdates();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void subscribeToAllUpdates() {
        final Set<String> topics = new LinkedHashSet<String>();
        for (Long facilityEquipmentnumber : this.facilityEquipmentnumberToMarkerMap.keySet()) {
            topics.add(createFacilityEquipmentnumberTopic(facilityEquipmentnumber));
        }
        final String[] topicsArray = topics.toArray(new String[topics.size()]);
        subscribe(topicsArray);
    }

    private void unsubscribeFromAllUpdates() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final Set<String> subscribedTopics = preferences.getStringSet(AufzugswaechterPreferences.SUBSCRIBED_TOPICS, Collections.<String>emptySet());
        final String[] topics = subscribedTopics.toArray(new String[subscribedTopics.size()]);
        unsubscribe(topics);
    }

    private static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";

    private void scanQRCode() {
        try {
            //start the scanning activity from the com.google.zxing.client.android.SCAN intent
            Intent intent = new Intent(ACTION_SCAN);
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(intent, 0);
        } catch (Exception anfe) {
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
            startActivityForResult(marketIntent, 0);
//            showDialog(AndroidBarcodeQrExample.this, "No Scanner Found", "Download a scanner code activity?", "Yes", "No").show();
        }
    }

    //on ActivityResult method
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                //get the extras that are returned from the intent
                String contents = intent.getStringExtra("SCAN_RESULT");
                if (contents != null) {
                    Long facilityEquipmentnumber = null;
                    final int positionOfLastHash = contents.lastIndexOf("#");
                    if (positionOfLastHash >= 0) {
                        final String facilityEquipmentnumberAsString = contents.substring(positionOfLastHash + 1);
                        try {
                            facilityEquipmentnumber = Long.valueOf(facilityEquipmentnumberAsString);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    if (facilityEquipmentnumber == null) {
                        Toast toast = Toast.makeText(this, MessageFormat.format("Could not extract facility equipment number from the QR code [{0}].", contents), Toast.LENGTH_LONG);
                        toast.show();
                    } else {
                        activateFacilityByEquipmentnumber(facilityEquipmentnumber);
                    }
                }
            }
        }
    }

    private void activateFacilityByEquipmentnumber(long facilityEquipmentnumber) {
        final Marker marker = this.facilityEquipmentnumberToMarkerMap.get(facilityEquipmentnumber);
        if (marker != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
            marker.showInfoWindow();
        } else {
            Toast toast = Toast.makeText(this, MessageFormat.format("Could not find facility with the equipment number.", facilityEquipmentnumber), Toast.LENGTH_LONG);
            toast.show();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(AufzugswaechterPreferences.REGISTRATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.5, 10.5), 6));
        new RetrieveJSONArrayTask() {
            @Override
            protected void onPostExecute(final JSONArray features) {
                super.onPostExecute(features);
                if (features != null) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                createMarkersFromJson(features);
                                if (MapActivity.this
                                        .initialFacilityEquipmentnumber != null) {
                                    MapActivity.this.activateFacilityByEquipmentnumber(MapActivity.this
                                            .initialFacilityEquipmentnumber);
                                    MapActivity.this
                                            .initialFacilityEquipmentnumber = null;
                                }
                            } catch (JSONException e) {
                                Log.e(LOG_TAG, "Error processing JSON", e);
                            }
                        }
                    });
                }
            }
        }.execute(getResources().getString(R.string.api_url));
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());
        mMap.setOnInfoWindowLongClickListener(this);
    }

    private final Map<String, JSONObject> markerIdToFeatureMap = Collections.synchronizedMap(
            new HashMap<String, JSONObject>()
    );

    private final Map<Long, Marker> facilityEquipmentnumberToMarkerMap = Collections.synchronizedMap(
            new HashMap<Long, Marker>()
    );

    private void createMarkersFromJson(JSONArray features) throws JSONException {
        for (int i = 0; i < features.length(); i++) {
            // Create a marker for each city in the JSON data.
            final JSONObject feature = features.getJSONObject(i);
            final JSONObject properties = feature.optJSONObject("properties");
            final JSONObject geometry = feature.optJSONObject("geometry");
            if (properties != null && geometry != null) {
                final Long facilityEquipmentnumber = properties.getLong("facilityEquipmentnumber");
                if (facilityEquipmentnumber != null) {
                    final String facilityEquipmentnumberAsString = facilityEquipmentnumber.toString();
                    final String facilityStationName = properties.optString("stationname", "<unknown station>");
                    final String facilityType = properties.optString("facilityType", "<unknown facility type>");
                    final String facilityDescription = properties.optString("facilityDescription", MessageFormat.format("{0} {1,number,#}", facilityType, facilityEquipmentnumber));
                    final String facilityState = properties.optString("facilityState", "<unknown facility state>");
                    final Long facilityStateKnownSince = properties.optLong("facilityStateKnownSince");
                    final JSONArray coordinates = geometry.getJSONArray("coordinates");
                    final double lng = coordinates.getDouble(0);
                    final double lat = coordinates.getDouble(1);
                    final String title = MessageFormat.format("{0}: {1}", facilityStationName, facilityDescription);

                    final float hue;
                    if ("ACTIVE".equals(facilityState) || "TEST_ACTIVE".equals(facilityState)) {
                        hue = BitmapDescriptorFactory.HUE_GREEN;
                    } else if ("INACTIVE".equals(facilityState) || "TEST_INACTIVE".equals(facilityState)) {
                        hue = BitmapDescriptorFactory.HUE_RED;
                    } else {
                        hue = BitmapDescriptorFactory.HUE_ORANGE;
                    }

                    final Marker marker = mMap.addMarker(new MarkerOptions()
                                    .title(title)
                                    .snippet("Facility state: " + facilityState + "\n foo bar hold long to subscribe")
                                    .position(new LatLng(lat, lng))
                                    .icon(BitmapDescriptorFactory.defaultMarker(
                                            hue
                                    ))
                    );
                    this.markerIdToFeatureMap.put(marker.getId(), feature);
                    this.facilityEquipmentnumberToMarkerMap.put(facilityEquipmentnumber, marker);
                }
            }
        }
    }

    @Override
    public void onInfoWindowLongClick(Marker marker) {
        final String markerId = marker.getId();
        final JSONObject feature = this.markerIdToFeatureMap.get(markerId);
        if (feature != null) {
            toggleSubscription(feature);
        }
        marker.hideInfoWindow();
    }

    private void toggleSubscription(JSONObject feature) {
        final JSONObject properties = feature.optJSONObject("properties");
        if (properties != null) {
            final Long facilityEquipmentnumber = properties.optLong("facilityEquipmentnumber", Long.MIN_VALUE);
            if (facilityEquipmentnumber != Long.MIN_VALUE) {
                toggleSubscription(facilityEquipmentnumber);
            }
        }
    }

    private void toggleSubscription(Long facilityEquipmentnumber) {
        final String topic = createFacilityEquipmentnumberTopic(facilityEquipmentnumber);
        final boolean subscribedForTopic = isSubscribedForTopic(topic);
        if (subscribedForTopic) {
            unsubscribe(topic);
        } else {
            subscribe(topic);
        }
    }

    private void subscribe(String... topic) {
        new SubscribeToTopicTask(this) {
            @Override
            protected void onPostExecute(String[] topics) {
                super.onPostExecute(topics);
                Toast toast = Toast.makeText(MapActivity.this, "Subscribed", Toast.LENGTH_SHORT);
                toast.show();
            }
        }.execute(topic);
    }

    private void unsubscribe(String... topic) {
        new UnsubscribeFromTopicTask(this) {
            @Override
            protected void onPostExecute(String[] topics) {
                super.onPostExecute(topics);
                Toast toast = Toast.makeText(MapActivity.this, "Unsubscribed", Toast.LENGTH_SHORT);
                toast.show();
            }
        }.execute(topic);
    }

    private boolean isSubscribedForFacilityEquipmentnumberTopic(Long facilityEquipmentnumber) {
        final String topic = createFacilityEquipmentnumberTopic(facilityEquipmentnumber);
        return isSubscribedForTopic(topic);
    }

    private boolean isSubscribedForTopic(String topic) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final Set<String> subscribedTopics = preferences.getStringSet(AufzugswaechterPreferences.SUBSCRIBED_TOPICS, Collections.<String>emptySet());
        return subscribedTopics.contains(topic);
    }

    @NonNull
    private String createFacilityEquipmentnumberTopic(Long facilityEquipmentnumber) {
        final String topicFormat = getResources().getString(R.string.facility_topic_format);
        return MessageFormat.format(topicFormat, facilityEquipmentnumber);
    }


    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        private final View mContents;

        CustomInfoWindowAdapter() {
            mContents = getLayoutInflater().inflate(R.layout.facility_marker_info_contents, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            final String markerId = marker.getId();
            final JSONObject feature = MapActivity.this.markerIdToFeatureMap.get(markerId);
            if (feature != null && feature.optJSONObject("properties") != null &&
                    feature.optJSONObject("properties").optLong("facilityEquipmentnumber",
                            Long.MIN_VALUE) != Long.MIN_VALUE) {
                render(mContents, marker, feature);
            }
            return mContents;
        }

        private void render(View view, Marker marker, JSONObject feature) {
            final JSONObject properties = feature.optJSONObject("properties");
            final Long facilityEquipmentnumber = properties.optLong("facilityEquipmentnumber");
            final String facilityEquipmentnumberAsString = facilityEquipmentnumber.toString();
            final String facilityStationName = properties.optString("stationname", "<unknown station>");
            final String facilityType = properties.optString("facilityType", "<unknown facility type>");
            final String facilityDescription = properties.optString("facilityDescription", MessageFormat.format("{0} {1,number,#}", facilityType, facilityEquipmentnumber));
            final String facilityState = properties.optString("facilityState", "<unknown facility state>");
            final String facilityStateKnownSince;
            {
                final Long facilityStateKnownSinceAsLong = properties.optLong("facilityStateKnownSince");
                if (facilityStateKnownSinceAsLong != null) {
                    final Date facilityStateKnownSinceAsDate = new Date(facilityStateKnownSinceAsLong);
                    final DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
                    final DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
                    facilityStateKnownSince = MessageFormat.format("{0} {1}",
                            dateFormat.format(facilityStateKnownSinceAsDate),
                            timeFormat.format(facilityStateKnownSinceAsDate));
                } else {
                    facilityStateKnownSince = "<unknown>";
                }
            }

            final String toggleFacilitySubscription =
                    isSubscribedForFacilityEquipmentnumberTopic(facilityEquipmentnumber) ?
                            "Click and hold to unsubscribe from updates" :
                            "Click and hold to subscribe for updates";

            final TextView facilityEquipmentnumberTextView = (TextView) mContents.findViewById(R.id.facilityEquipmentnumber_content);
            facilityEquipmentnumberTextView.setText(facilityEquipmentnumberAsString);
            final TextView stationNameTextView = (TextView) mContents.findViewById(R.id.stationName_content);
            stationNameTextView.setText(facilityStationName);
            final TextView facilityDescriptionTextView = (TextView) mContents.findViewById(R.id.facilityDescription_content);
            facilityDescriptionTextView.setText(facilityDescription);
            final TextView facilityTypeTextView = (TextView) mContents.findViewById(R.id.facilityType_content);
            facilityTypeTextView.setText(facilityType);
            final TextView facilityStateTextView = (TextView) mContents.findViewById(R.id.facilityState_content);
            facilityStateTextView.setText(facilityState);
            final TextView facilityStateKnownSinceTextView = (TextView) mContents.findViewById(R.id.facilityStateKnownSince_content);
            facilityStateKnownSinceTextView.setText(facilityStateKnownSince);

            final Button toggleFacilitySubscriptionButton = (Button) mContents.findViewById(R.id.toggleFacilitySubscription_button);
            toggleFacilitySubscriptionButton.setText(toggleFacilitySubscription);

        }
    }
}
