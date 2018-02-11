package com.anedma.nightspot.async.response;

import com.anedma.nightspot.dto.Pub;

import java.util.HashMap;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 10/02/2018.
 */

public interface UserResponse extends ApiResponse {

    void userPubsResponse(HashMap<Integer, Pub> pubs);
    void userResponse(int id, String name, String lastName, boolean isPub, int tracks);
    void calculateAffinityDone();

}
