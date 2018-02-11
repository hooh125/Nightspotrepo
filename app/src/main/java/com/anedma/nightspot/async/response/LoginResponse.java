package com.anedma.nightspot.async.response;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 10/02/2018.
 */

public interface LoginResponse extends ApiResponse {

    void loginResponse(boolean alreadyRegistered, boolean isPub);

}
