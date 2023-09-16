package com.example.nsyy.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.nsyy.exception.BluetoothException;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Formatter;
import java.util.UUID;

public class BlueToothUtil {

    private static boolean mEnableLogOut = true;
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private volatile static BlueToothUtil uniqueInstance;

    // 电子秤终端 uuid
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String BLE_TAG = "BLUETOOTH: ";

    private BlueToothUtil() {
    }

    //采用Double CheckLock(DCL)实现单例
    public static BlueToothUtil getInstance() {
        if (uniqueInstance == null) {
            synchronized (LocationUtil.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new BlueToothUtil();
                }
            }
        }
        return uniqueInstance;
    }


    /**
     * 构造蓝牙工具
     *
     * @param context 上下文
     */
    public void init(Context context) {
        this.mContext = context;
    }


    /**
     * 字节转换为 16 进制字符串
     *
     * @param b 字节
     * @return Hex 字符串
     */
    private static String byte2Hex(byte b) {
        StringBuilder hex = new StringBuilder(Integer.toHexString(b));
        if (hex.length() > 2) {
            hex = new StringBuilder(hex.substring(hex.length() - 2));
        }
        while (hex.length() < 2) {
            hex.insert(0, "0");
        }
        return hex.toString();
    }


    /**
     * 字节数组转换为 16 进制字符串
     *
     * @param bytes 字节数组
     * @return Hex 字符串
     */
    private static String byte2Hex(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        String hash = formatter.toString();
        formatter.close();
        return hash;
    }


    /**
     * 打印日志
     */
    private static void logD(String msg) {
        if (mEnableLogOut) Log.d("BLEUTILS", msg);
    }


    // ================================== 测试方法

    @SuppressLint("MissingPermission")
    public String read(String... bluetoothDevicesMac) throws BluetoothException, IOException {
        BluetoothDevice bluetoothDevice = getBluetoothDeviceByMac(bluetoothDevicesMac);

        // 检查是否已经和蓝牙设备配对
        if (bluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
            throw new BluetoothException("本设备还未和 " + bluetoothDevicesMac[0] + " 进行蓝牙配对，请先进行配对。");
        }

        BluetoothSocket bluetoothSocket;
        try {
//            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                throw new BluetoothException("未获取蓝牙权限，请先获取蓝牙权限");
//            }
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID);
        } catch (IOException e) {
            throw new BluetoothException("链接蓝牙设备异常：获取Socket失败。" + e.getMessage());
        }

        if (bluetoothSocket == null) {
            throw new BluetoothException("链接蓝牙设备异常：获取Socket失败。");
        }


        // 尝试连接
        try {
            // 等待连接，会阻塞线程
            bluetoothSocket.connect();
            logD( "连接成功");
        } catch (Exception connectException) {
            bluetoothSocket.close();
            logD("连接失败:" + connectException.getMessage());
        }


        // 开始监听数据接收
        boolean isRunning = true;
        byte[] result = new byte[0];
        try {
            // 通过向电子秤发送 "R" 来获取重量
            byte[] sendMsg = "R".getBytes();
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(sendMsg);

            InputStream inputStream = bluetoothSocket.getInputStream();
            while (isRunning) {
                // 等待有数据
                byte[] buffer = new byte[102400];
                synchronized (this){
                    int bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        final byte[] data = new byte[bytes];
                        System.arraycopy(buffer, 0, data, 0, bytes);
                        logD("服务端收到客户端发送的数据：" + new String(data));

                    } else {
                        logD("服务端收到客户端发送的数据：null");
                    }
                }

//                if (inputStream.available() == 0) {
//                    break;
//                }
            }
        } catch (Exception e) {
            throw new BluetoothException("接收数据失败： " + e.getMessage());
        }
        return new String(result);
    }


    /**
     * 根据蓝牙设备的 mac 地址，来获取蓝牙设备
     * @return
     */
    private BluetoothDevice getBluetoothDeviceByMac(String... bluetoothDevicesMac) throws BluetoothException {
        // 1. 获取蓝牙权限（已在MainActivity启动时获取）
        if (!XXPermissions.isGranted(mContext, new String[]{
                Permission.BLUETOOTH_SCAN,
                Permission.BLUETOOTH_CONNECT,
                Permission.BLUETOOTH_ADVERTISE})) {
            throw new BluetoothException("未获取蓝牙权限，请先获取蓝牙权限");
        }

        // 2. 打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mContext.startActivity(intent);
            //mContext.startActivityForResult(intent, requestCode);

            // mBluetoothAdapter.isEnabled();
        }

        // 3. 根据 mac 地址获取蓝牙设备
        BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(bluetoothDevicesMac[0]);
        if (remoteDevice == null) {
            throw new BluetoothException("通过 " + bluetoothDevicesMac[0] + " 未获取任何蓝牙设备，请输入正确的 mac 地址。");
        }

        return remoteDevice;
    }

}
