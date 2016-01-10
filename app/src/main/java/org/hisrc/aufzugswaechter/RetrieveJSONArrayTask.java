package org.hisrc.aufzugswaechter;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RetrieveJSONArrayTask extends AbstractRetrieveJSONTask<JSONArray> {

    @Override
    protected JSONArray parseJSON(String jsonString) throws JSONException {
        return new JSONArray(jsonString);
    }
}
