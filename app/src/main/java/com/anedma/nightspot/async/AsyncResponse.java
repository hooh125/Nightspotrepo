package com.anedma.nightspot.async;

import org.json.JSONObject;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 24/01/2018.
 */

public interface AsyncResponse {
    void processFinish(JSONObject jsonObject);
}
