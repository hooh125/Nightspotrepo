package com.anedma.nightspot.exception;

/**
 * Created by a-edu on 24/10/2017.
 */

public class SQLiteInsertException extends Exception {

    public SQLiteInsertException() {
        super("Ha habido un error al insertar el fingerprint en la base de datos");
    }
}
