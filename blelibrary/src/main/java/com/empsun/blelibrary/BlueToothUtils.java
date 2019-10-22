package com.empsun.blelibrary;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.TextView;

import java.util.UUID;


/**
 * Created by chen on 2018/1/16.
 */
public class BlueToothUtils {

    private String TAG = "BlueToothUtils";
    private final UUID HEARTSERVICEUUID          = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private final UUID HEARTREADCHARACTERISTIC   = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private final UUID BATTERYSERVICEUUID        = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    private final UUID BATTERYREADCHARACTERISTIC = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    private final UUID URINESERVICEUUID          = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private final UUID URINEREADCHARACTERISTIC   = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private final UUID URINEWRITECHARACTERISTIC  = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static Handler           mHandler;//用于回调到主线程
    private static BlueToothUtils    mInstance;//蓝牙工具实例对象
    private final  BluetoothAdapter  mBlueToothAdapter;//系统蓝牙适配器
    private static Context           mContext;
    private        BluetoothGatt     mBluetoothGatt;//GATT
    private        BluetoothDevice   mBluetoothDevice;//蓝牙设备对象
    private        BlueToothCallback callback; //蓝牙回调对象
    private        int               SCAN_TIME = 10 * 1000;//搜索时间
    private boolean connState = false;
    private static int type;//1:尿机 2:心率带
    private static boolean isAutoNotifyDevice = false;


    private BluetoothGattCharacteristic writeCharacteristicl,//尿液分析仪--write特征值
            notifyCharacteristic,//尿液分析仪--read特征值
            batteryCharacteristic,//心率带--电量特征值
            heartCharacteristic;//心率带--心电数据特征值
    private TextView             dialogTV;
    private BluetoothGattService mBluetoothGattService;


    public static BlueToothUtils getInstance(Context context,int deviceType,boolean isAutoNotify) {
        mContext = context;
        isAutoNotifyDevice = isAutoNotify;
        type =deviceType;
        if (mInstance == null) {
            synchronized (BlueToothUtils.class) {
                if (mInstance == null) {
                    mInstance = new BlueToothUtils();
                }
            }
        }
        return mInstance;
    }


    /*
     * 对外暴露蓝牙适配器
     */
    public BluetoothAdapter getmBlueToothAdapter() {
        return mBlueToothAdapter;
    }

    @SuppressLint("HandlerLeak")
    private BlueToothUtils() {
        mBlueToothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = new Handler() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what ) {
                    case 1:
                        //搜索结束
                        mBlueToothAdapter.stopLeScan(mLeScanCallback);
                        if (callback != null) {
                            callback.onFinishFind();
                        }
                        break;
                    case 2:
                        if (batteryCharacteristic != null) {
                            boolean heartNotifyState = enableNotification(true, batteryCharacteristic);
                            if (callback != null) {
                                callback.onNotifyState(heartNotifyState);
                            }
                        }
                        break;
                }
            }
        };
    }

    public void stopScan(){
        if (mBlueToothAdapter !=null && mLeScanCallback != null ){
            mBlueToothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    /*
     * 搜索
     */
    public void scanDevice() {

        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }

        boolean discovering = mBlueToothAdapter.isDiscovering();
        if (discovering) {
            return;
        }
        mBlueToothAdapter.startLeScan(mLeScanCallback);
        mHandler.sendEmptyMessageDelayed(1, SCAN_TIME);
    }


    /*
     * 连接蓝牙
     * */
    public void connDevice(String mac) {
        //通过蓝牙地址获得蓝牙设备
        stopScan();
        mBluetoothDevice = mBlueToothAdapter.getRemoteDevice(mac);
        mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, gattCallback);
    }


    /**
     * 给BLE设备发送指令
     */
    public void sendData(byte[] bytes) {
        if (writeCharacteristicl != null && mBluetoothGatt != null && connState) {
            writeCharacteristicl.setValue(bytes);
            mBluetoothGatt.writeCharacteristic(writeCharacteristicl);
        }

    }


    /**
     * 搜索蓝牙的回调方法
     */

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            //回掉到主线程更新设备列表
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        callback.onFindDevice(device);
                    }
                }
            });
        }
    };

    /*
     * gatt回调
     * */
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {


        /*
         * 连接的时候的两种状态与中途的两种状态
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            //更新UI必须用在主线程
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
                    connState = true;
                    //打开服务
                    mBluetoothGatt.discoverServices();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onStateCallBack(BluetoothGatt.STATE_CONNECTED);
                            }

                        }
                    });
                    break;
                case BluetoothGatt.STATE_CONNECTING:

                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    connState = false;
                    if (mBluetoothGatt != null) {
                        mBluetoothGatt.close();
                        mBluetoothGatt = null;
                    }
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {

                            if (callback != null) {
                                callback.onStateCallBack(BluetoothGatt.STATE_DISCONNECTED);
                            }
                        }
                    });
                    break;
                case BluetoothGatt.STATE_DISCONNECTING:
                    break;
                default:

                    break;
            }
        }

        /*
         * 获取服务,notify通知服务
         * */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (isAutoNotifyDevice){
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    //根据设备服务
                    switch (type){
                        /*尿液设备*/
                        case 1:
                            notifyUrineDevice();
                            break;
                        /*心率带设备*/
                        case 2:
                            notifyHeartDevice();
                            break;
                    }

                }
            }


        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic)
        {
            super.onCharacteristicChanged(gatt, characteristic);
            final byte[] value = characteristic.getValue();
            Log.e(TAG, StrExchangeBytes.bytesToHexString(value));
            Log.e(TAG, characteristic.getUuid().toString());
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        if (characteristic.getUuid().equals(HEARTREADCHARACTERISTIC)) {
                            //心电数据回调
                            callback.heartDataReceived(value);
                        } else if (characteristic.getUuid().equals(BATTERYREADCHARACTERISTIC)) {
                            //电量回调
                            callback.batteryDataReceived(value);
                        }else if (characteristic.getUuid().equals(URINEREADCHARACTERISTIC)){
                            //尿机数据回调
                            callback.onDataCallBack(value);
                        }
                    }
                }
            });
        }
    };

    /**
     * 开启尿液通知服务
     */
    public void notifyUrineDevice() {
        notifyCharacteristic = getServiceCharacteristic(URINESERVICEUUID,URINEREADCHARACTERISTIC);
        writeCharacteristicl = getServiceCharacteristic(URINESERVICEUUID,URINEWRITECHARACTERISTIC);
        if (notifyCharacteristic != null) {
            boolean urineNotifyState = enableNotification(true,notifyCharacteristic);
            if (callback != null) {
                callback.onNotifyState(urineNotifyState);
            }

        }
    }

    /**
     * 开启心率带通知服务
     */
    public void notifyHeartDevice() {
        boolean heartValueNotifyState = false;
        heartCharacteristic= getServiceCharacteristic(HEARTSERVICEUUID,HEARTREADCHARACTERISTIC);
        if (heartCharacteristic!=null) {
            heartValueNotifyState = enableNotification(true,heartCharacteristic);
        }
        if (heartValueNotifyState){
            batteryCharacteristic = getServiceCharacteristic(BATTERYSERVICEUUID,BATTERYREADCHARACTERISTIC);
            if (batteryCharacteristic!=null) {
                //开启电量通知服务
                mHandler.sendEmptyMessageDelayed(2,1000);
            }
        }else{
            //TODO notify fail
            if (callback != null) {
                callback.onNotifyState(heartValueNotifyState);
            }

        }

    }

    private BluetoothGattCharacteristic getServiceCharacteristic(UUID serviceUuid, UUID readCharacteristic)
    {
        if (mBluetoothGatt!=null && serviceUuid!=null) {
            mBluetoothGattService = mBluetoothGatt.getService(serviceUuid);
        }
        BluetoothGattCharacteristic characteristic        = null;
        if (mBluetoothGattService != null) {
            characteristic = mBluetoothGattService.getCharacteristic(readCharacteristic);
        }
        return characteristic;
    }



    /*
     * 打开通知操作
     */
    private boolean enableNotification(boolean enable, BluetoothGattCharacteristic characteristic) {
        if (mBluetoothGatt == null || characteristic == null) { return false; }
        if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enable)) { return false; }
        //这里的UUID 是不变的 不要改动
        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(UUID.fromString(
                "00002902-0000-1000-8000-00805f9b34fb"));
        if (clientConfig == null) { return false; }
        if (enable) {
            clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        boolean b = mBluetoothGatt.writeDescriptor(clientConfig);
        return b;
    }


    /*
     * 断开蓝牙、刷新缓存
     */
    public void stopService() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
        mHandler.removeMessages(1);
        mHandler.removeMessages(2);
    }

    /*
     *  设置回调接口对象
     * */
    public void setCallback(BlueToothCallback callback) {
        this.callback = callback;
    }


    /**
     * 设置搜索时间
     * @param SCAN_TIME
     */
    public void setSCAN_TIME(int SCAN_TIME) {
        this.SCAN_TIME = SCAN_TIME;
    }
}
