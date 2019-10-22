package com.empsun.empsunbleutils;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * @author Licoy
 * @version 2018/7/6/21:09
 */

public class DeviceAdapter
        extends RecyclerView.Adapter<DeviceAdapter.Holder> {
    private Context            mContext;
    private List<BluetoothDevice> mDevices;

    public DeviceAdapter(Context mContext, List<BluetoothDevice> mDevices) {
        this.mContext = mContext;
        this.mDevices = mDevices;
    }


    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View   inflate = LayoutInflater.from(mContext).inflate(R.layout.device_item, parent, false);
        Holder holder  = new Holder(inflate);
        return holder;
    }

    @Override
    public void onBindViewHolder(Holder holder, final int position) {

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClick!=null){
                    itemClick.onItemClick(position);
                }
            }
        });
        if (mDevices.get(position).getName() !=null ) {
            holder.mName.setText(mDevices.get(position).getName());
        }
        holder.mAddress.setText(mDevices.get(position).getAddress());
    }



    @Override
    public int getItemCount() {
        return mDevices.size();
    }
    class Holder extends RecyclerView.ViewHolder{
        TextView mName;
        TextView mAddress;
        public Holder(View itemView) {
            super(itemView);
            mName=itemView.findViewById(R.id.name);
            mAddress = itemView.findViewById(R.id.address);
        }
    }

    private OnItemClick itemClick;
    public interface OnItemClick{

        void onItemClick(int position);
    }

    public void setItemClick(OnItemClick itemClick) {
        this.itemClick = itemClick;
    }
}
