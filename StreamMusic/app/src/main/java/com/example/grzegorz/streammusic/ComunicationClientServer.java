package com.example.grzegorz.streammusic;

import android.util.Log;

/**
 * Created by grzegorz on 18.04.17.
 */

public class ComunicationClientServer {
    static final int sizeOfResponse = 2;
    static final byte[] nextPack = {0, 0};// prosze wyslac kolejna paczke danych
    static final byte[] pasue = {0, 1};// klient wcisnal pause czekamy co dalej
    static final byte[] resetSong = {1, 0};// klient chce piosenke od poczatku
    static final byte[] changeSong = {1, 1};// zmiana piosenki, nie wysylaj wiecej obecnej

    public static boolean checkResponse(byte[] expected, byte [] response){
        if(expected.length != response.length){
            return false;
        }
        for(int i = 0; i < expected.length; i++){
            Log.e("Check", i + "");
            if (expected[i] != response[i]){
                return false;
            }
        }
        return true;
    }
}
