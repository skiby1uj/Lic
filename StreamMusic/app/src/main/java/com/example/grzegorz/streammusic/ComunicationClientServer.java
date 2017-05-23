package com.example.grzegorz.streammusic;

import android.util.Log;

/**
 * Created by grzegorz on 18.04.17.
 */

public class ComunicationClientServer{
    static final int sizeOfResponse = 3;
    static final byte[] nextPack = {0, 0, 0};// prosze wyslac kolejna paczke danych
    static final byte[] pasue = {0, 0, 1};// pause czekamy co dalej
//    static final byte[] finishPlaying = {0, 1, 0};// klient chce piosenke od poczatku
    static final byte[] changeSong = {0, 1, 1};// zmiana piosenki, nie wysylaj wiecej obecnej
    static final byte[] play = {1, 0, 0};//muzyka gra
    static final byte[] lastPackage = {1, 0, 1};//ostatnia paczka z piosenka
    static final byte[] musicFinish = {1, 1, 0};//piosenka przestała grac na kliencie
    static final int sizeOfPackage = 500;

    public static boolean checkResponse(byte[] expected, byte [] response){
        if (response == null){
            return false;
        }
        if(expected.length != response.length){
            return false;
        }
        for(int i = 0; i < expected.length; i++){
            if (expected[i] != response[i]){
                return false;
            }
        }
        return true;
    }
}
