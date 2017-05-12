package com.example.grzegorz.streammusic;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.LinkedList;

/**
 * Created by grzegorz on 01.05.17.
 */

public class ListDeviceConnectAdapter extends ArrayAdapter<ListDeviceConnectRow>{
    Context context;
    int layoutResourceId;
    LinkedList<ListDeviceConnectRow> data;


    public ListDeviceConnectAdapter(Context context, int layoutResourceId, LinkedList<ListDeviceConnectRow> data) {
        super(context, layoutResourceId, data);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ListDeviceConnectHolder holder = null;

        if(row == null){
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new ListDeviceConnectAdapter.ListDeviceConnectHolder();
            holder.name = (TextView)row.findViewById(R.id.textNameDevice);
            holder.adress = (TextView)row.findViewById(R.id.textAdressDevice);

            row.setTag(holder);
        }else{
            holder = (ListDeviceConnectAdapter.ListDeviceConnectHolder)row.getTag();
        }

        ListDeviceConnectRow object = data.get(position);
        holder.name.setText(object.getName());
        holder.adress.setText(object.getAdress());

        return  row;
    }

    static class ListDeviceConnectHolder{
        TextView name;
        TextView adress;
    }
}
