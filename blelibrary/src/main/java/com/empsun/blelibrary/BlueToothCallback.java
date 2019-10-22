package com.empsun.blelibrary;

import android.bluetooth.BluetoothDevice;

/**
 * Created by chen on 2018/1/16.
 */

public abstract class BlueToothCallback {
    //连接状态
    public  void onStateCallBack(int state){};
    //数据返回
    public  void onDataCallBack(byte[] bytes){};
    //搜索到设备
    public  void onFindDevice(BluetoothDevice device){};
    //notify 回调
    public  void onNotifyState(boolean onNotifyState){};
    //搜索结束回调
    public  void onFinishFind(){};
    //心率数据回调
    public  void heartDataReceived(byte[] bytes){};
    //电量回调
    public  void batteryDataReceived(byte[] bytes){};

}
