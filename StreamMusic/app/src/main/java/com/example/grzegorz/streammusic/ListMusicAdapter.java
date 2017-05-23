package com.example.grzegorz.streammusic;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.LinkedList;

/**
 * Created by grzegorz on 22.04.17.
 */

public class ListMusicAdapter extends ArrayAdapter<ListMusicRow> {
    Context context;
    int layoutResourceId;
    LinkedList<ListMusicRow> data = null;

    public ListMusicAdapter(Context context, int layoutResourceId, LinkedList<ListMusicRow> data) {
        super(context, layoutResourceId, data);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.data = data;
    }

    public ListMusicRow getItem(int index){
        return this.data.get(index);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ListMusicHolder holder = null;

            if(row == null){
                LayoutInflater inflater = ((Activity)context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new ListMusicHolder();
                holder.title = (TextView)row.findViewById(R.id.titleMusic);
                holder.path = (TextView)row.findViewById(R.id.pathMusic);
                holder.photo = (ImageView)row.findViewById(R.id.imageVplay);

                row.setTag(holder);
            }else{
                holder = (ListMusicHolder)row.getTag();
            }

            ListMusicRow object = data.get(position);
            holder.title.setText(object.getTitle());
            holder.path.setText(object.getPath());
            holder.photo.setImageResource(object.getRaw());

            return  row;
    }


    static class ListMusicHolder{
        TextView title;
        TextView path;
        ImageView photo;
    }
}
