package com.anedma.nightspot.async.response;

import com.anedma.nightspot.dto.Pub;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 10/02/2018.
 */

public interface PubResponse extends ApiResponse {

    void pubResponse(Pub pub);

}
