package com.anedma.nightspot.exception;

/**
 * Created by a-edu on 24/10/2017.
 */

public class FingerprintInsertException extends Exception {

    public FingerprintInsertException() {
        super("Ha habido un error al insertar el fingerprint en la base de datos");
    }
}
