package org.hisrc.aufzugswaechter;

import android.os.AsyncTask;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class AbstractRetrieveJSONTask<J> extends AsyncTask<String, Void, J> {

    private static final String LOG_TAG = "AufzugswaechterApp";

    private IOException exception;

    public IOException getException() {
        return exception;
    }

    @Override
    protected J doInBackground(String... params) {
        final String serviceURL = params[0];
        HttpURLConnection conn = null;
        final StringBuilder json = new StringBuilder();
        try {
            // Connect to the web service
            URL url = new URL(serviceURL);
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Read the JSON data into the StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                json.append(buff, 0, read);
            }
            return parseJSON(json.toString());
        } catch (IOException ioex) {
            this.exception = new IOException("Error connecting to service", ioex);
            return null;
        } catch (JSONException jsonex) {
            this.exception = new IOException("Error connecting to service", jsonex);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    protected abstract J parseJSON(String jsonString) throws JSONException;
}
