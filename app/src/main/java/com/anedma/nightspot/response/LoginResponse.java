package com.anedma.nightspot.response;

import com.anedma.nightspot.async.AsyncResponse;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 04/02/2018.
 */

public interface LoginResponse extends AsyncResponse {

    void loginResult(boolean error, boolean isPub, boolean alreadyRegistered);

}
