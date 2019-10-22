package com.empsun.empsunbleutils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.empsun.blelibrary.BlueToothCallback;
import com.empsun.blelibrary.BlueToothUtils;
import com.empsun.blelibrary.StrExchangeBytes;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{

    private RxPermissions  rxPermissions;
    private BlueToothUtils blueToothUtils;
    private TextView       conState,myData;
    private Button scan,connect,button,disconnectBLE,auto,sendUIH;
    private RecyclerView          mRecycler;
    private DeviceAdapter         deviceAdapter;
    private BluetoothDevice       bluetoothLeDevice;
    private List<BluetoothDevice> mDevices = new ArrayList<>();
    private TextView              mSeleteName,mSeleteAddress;
    private boolean           flag = true;
    private BluetoothDevice   mCurrentDevice;
    private BlueToothCallback mCallback;
    private boolean           mNotifyState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 动态权限
        showPermission();
        initView();
        initListener();

        /*初始化蓝牙*/
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(OrientationHelper.VERTICAL);
        mRecycler.setLayoutManager(manager);
        deviceAdapter = new DeviceAdapter(this, mDevices);
        mRecycler.setAdapter(deviceAdapter);
        deviceAdapter.setItemClick(new DeviceAdapter.OnItemClick() {
            @Override
            public void onItemClick(int position) {
                bluetoothLeDevice = mDevices.get(position);
                if (bluetoothLeDevice.getName() != null) {
                    mSeleteName.setText(bluetoothLeDevice.getName());
                    mCurrentDevice = bluetoothLeDevice;
                }
                mSeleteAddress.setText(bluetoothLeDevice.getAddress());
            }
        });


    }

    private void initListener() {

        mCallback = new BlueToothCallback() {
            //连接 状态
            @Override
            public void onStateCallBack(int state) {
                switch (state){
                    case BluetoothGatt.STATE_CONNECTED:
                        //成功
                        conState.setText("连接成功");
                        flag=true;
                        break;
                    case BluetoothGatt.STATE_DISCONNECTED:
                        //失败
                        conState.setText("连接失败");
                        flag=true;
                        break;

                }
            }

            //搜索到设备
            @Override
            public void onFindDevice(BluetoothDevice device) {
                if (!mDevices.contains(device) && device!=null) {
                    mDevices.add(device);
                    deviceAdapter.notifyDataSetChanged();
                }
            }

            //打开通知状态回调
            @Override
            public void onNotifyState(boolean onNotifyState) {
                if (!onNotifyState) {
                    AppToast.creat(MainActivity.this,"开启通知服务失败");
                }
                mNotifyState = onNotifyState;
            }

            @Override
            public void onFinishFind() {
                AppToast.creat(MainActivity.this, "搜索完成！！！(*^_^*)");

            }
            @Override
            public void onDataCallBack(byte[] bytes) {
                //尿机数据回调
                myData.setText(StrExchangeBytes.bytesToHexString(bytes));

            }

            //心率带数据回调
            @Override
            public void heartDataReceived(byte[] bytes) {
                myData.setText(StrExchangeBytes.bytesToHexString(bytes));
            }
            @Override
            public void batteryDataReceived(byte[] bytes) {
                myData.setText(StrExchangeBytes.bytesToHexString(bytes));
            }

        };



        blueToothUtils.setCallback(mCallback);
        scan.setOnClickListener(this);
        connect.setOnClickListener(this);
        button.setOnClickListener(this);
        disconnectBLE.setOnClickListener(this);
        auto.setOnClickListener(this);
        sendUIH.setOnClickListener(this);
    }

    private void initView() {
        scan = (Button) findViewById(R.id.mScan);
        connect = (Button) findViewById(R.id.mConnect);
        conState = (TextView)findViewById(R.id.mState);
        myData = (TextView)findViewById(R.id.mData);
        button = (Button) findViewById(R.id.mOrder);
        sendUIH= (Button) findViewById(R.id.uih);
        auto = (Button) findViewById(R.id.autoCheck);
        disconnectBLE = (Button) findViewById(R.id.disconnectBle);
        mSeleteName = (TextView)findViewById(R.id.deviceName);
        mSeleteAddress = (TextView)findViewById(R.id.deviceAddress);
        mRecycler = (RecyclerView)findViewById(R.id.mRecycler);
        blueToothUtils = BlueToothUtils.getInstance(this,1,true);

    }

    private boolean flag_permission=false;

    @SuppressLint("CheckResult")
    private void showPermission() {
        rxPermissions = new RxPermissions(this);
        rxPermissions
                .request(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean granted) throws Exception
                    {
                        if (granted) {
                            flag_permission = true;
                        } else {
                            flag_permission = false;
                        }
                    }
                });

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.mScan:
                mDevices.clear();
                deviceAdapter.notifyDataSetChanged();
                scanBLE();

                break;
            case R.id.mConnect:
                if (mCurrentDevice != null) {
                    connDevice(mCurrentDevice);
                } else {
                    AppToast.creat(MainActivity.this,"未选择蓝牙设备");
                }
                break;
            case R.id.disconnectBle:
                disConnect();
                break;
                //获取尿机最后一条数据指令
            case R.id.mOrder:
                if (conState.getText().toString().trim().equals("连接成功")) {
                    byte[] uih = StrExchangeBytes.hexStringToBytes("938E0400080410");
                    sendData(uih);

                } else {
                    AppToast.creat(MainActivity.this,"当前未连接蓝牙");
                    return;
                }
                break;
                //尿机自动测试
            case R.id.autoCheck:
                if (conState.getText().toString().trim().equals("连接成功")) {
                    byte[] order = StrExchangeBytes.hexStringToBytes("938E0500080B0018");
                    sendData(order);
                } else {
                    AppToast.creat(MainActivity.this,"当前未连接蓝牙");
                    return;
                }
                break;
                //uih
            case R.id.uih:
                if (conState.getText().toString().trim().equals("连接成功")) {
                    byte[] uih = StrExchangeBytes.hexStringToBytes("UIH");
                    sendData(uih);
                } else {
                    AppToast.creat(MainActivity.this,"当前未连接蓝牙");
                    return;
                }
                break;
            default:
                break;

        }
    }

    private void sendData(byte[] data){
        if (mNotifyState){
            blueToothUtils.sendData(data);

        }else {
            AppToast.creat(this,"蓝牙初始化中...");
        }

    }


    private void disConnect() {
        blueToothUtils.stopService();
    }

    private void scanBLE() {
        blueToothUtils.scanDevice();
    }

    private void connDevice(BluetoothDevice device) {

        blueToothUtils.connDevice(device.getAddress());
    }
}

