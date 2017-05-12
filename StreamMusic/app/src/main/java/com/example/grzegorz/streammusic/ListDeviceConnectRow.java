package com.example.grzegorz.streammusic;

/**
 * Created by grzegorz on 01.05.17.
 */

public class ListDeviceConnectRow {
    private String name;
    private String adress;

    ListDeviceConnectRow(){}

    ListDeviceConnectRow(String name, String adress){
        this.name = name;
        this.adress = adress;
    }

    public String getName(){
        return this.name;
    }

    public String getAdress(){
        return this.adress;
    }
}
