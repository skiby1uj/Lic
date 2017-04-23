package com.example.grzegorz.streammusic;

/**
 * Created by grzegorz on 22.04.17.
 */

public class ListMusicRow {
    private String title;
    private String path;

    public ListMusicRow(){};

    public ListMusicRow(String title, String path){
        this.title = title;
        this.path = path;
    }

    public String getTitle(){
        return this.title;
    }

    public String getPath(){
        return this.path;
    }
}
