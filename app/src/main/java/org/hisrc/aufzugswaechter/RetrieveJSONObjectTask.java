package org.hisrc.aufzugswaechter;

import org.json.JSONException;
import org.json.JSONObject;

public class RetrieveJSONObjectTask extends AbstractRetrieveJSONTask<JSONObject> {

    @Override
    protected JSONObject parseJSON(String jsonString) throws JSONException {
        return new JSONObject(jsonString);
    }
}
