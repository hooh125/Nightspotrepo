package com.anedma.nightspot;

import com.anedma.nightspot.async.AsyncResponse;
import com.anedma.nightspot.response.LoginResponse;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 04/02/2018.
 */

public class ApiController implements AsyncResponse {

    private AsyncResponse delegate = null;

    public void insertUser(LoginResponse delegate) {
        this.delegate = delegate;
    }

    @Override
    public void processFinish(JSONObject jsonObject) {
        try {
            String operation = jsonObject.getString("operation");
            switch (operation) {
                case "insertUser":
                    LoginResponse loginResponse = (LoginResponse) delegate;

                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
