package com.example.nsyy.utils;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.nsyy.exception.BluetoothException;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
     * 打印日志
     */
    private static void logD(String msg) {
        Log.d(BLE_TAG, msg);
    }


    @SuppressLint("MissingPermission")
    public String read(String... bluetoothDevicesMac) throws BluetoothException, IOException {
        // 1. 根据蓝牙设备的 mac 地址，来获取蓝牙设备
        BluetoothDevice bluetoothDevice = getBluetoothDeviceByMac(bluetoothDevicesMac);

        // 2. 检查是否已经和蓝牙设备配对
        if (bluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
            throw new BluetoothException("本设备还未和 " + bluetoothDevicesMac[0] + " 进行蓝牙配对，请先进行配对。");
        }

        // 3. 连接蓝牙设备
        BluetoothSocket bluetoothSocket;
        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID);
        } catch (IOException e) {
            throw new BluetoothException("连接蓝牙设备异常：获取Socket失败。" + e.getMessage());
        }

        if (bluetoothSocket == null) {
            throw new BluetoothException("连接蓝牙设备异常：获取Socket失败。");
        }

        try {
            // 等待连接，会阻塞线程
            bluetoothSocket.connect();
            logD("成功连接蓝牙设备：mac address： " + bluetoothDevice.getAddress() +
                    "device name: " + bluetoothDevice.getName());
        } catch (Exception connectException) {
            close(bluetoothSocket, null, null);
            logD("连接失败:" + connectException.getMessage());
            throw new BluetoothException("连接蓝牙设备异常：连接 Socket 失败。");
        }


        InputStream inputStream = null;
        OutputStream outputStream = null;
        // 4. 通过应答模式获取重量（向电子秤发送 R ，电子秤会返回当前重量）
        boolean isRunning = true;
        byte[] sendData = "R".getBytes();
        byte[] result = null;
        try {
            outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(sendData);

            inputStream = bluetoothSocket.getInputStream();
            while (isRunning) {
                // 等待有数据
                byte[] buffer = new byte[1024];
                synchronized (this) {
                    int bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        result = new byte[bytes];
                        System.arraycopy(buffer, 0, result, 0, bytes);
                        logD("接收到电子秤发送的数据：" + new String(result));
                    } else {
                        logD("接收到电子秤发送的数据：null");
                    }
                }

                if (inputStream.available() == 0) {
                    break;
                }
            }
        } catch (Exception e) {
            close(bluetoothSocket, inputStream, outputStream);
            throw new BluetoothException("接收数据失败： " + e.getMessage());
        } finally {
            close(bluetoothSocket, inputStream, outputStream);
        }
        return converterWeight(new String(result));
    }

    /**
     * 关闭资源: BluetoothSocket, InputStream, OutputStream
     */
    public void close(BluetoothSocket socket, InputStream inputStream, OutputStream outputStream)
            throws BluetoothException {
        try {
            if (inputStream != null){
                inputStream.close();
            }
        } catch (IOException e) {
            throw new BluetoothException("资源关闭失败：" + e.getMessage());
        }
        try {
            if (outputStream != null){
                outputStream.close();
            }
        } catch (IOException e) {
            throw new BluetoothException("资源关闭失败：" + e.getMessage());
        }
        try {
            if (socket != null){
                socket.close();
            }
        } catch (IOException e) {
            throw new BluetoothException("资源关闭失败：" + e.getMessage());
        }
    }


    /**
     * 根据蓝牙设备的 mac 地址，来获取蓝牙设备
     * @return
     */
    @SuppressLint("MissingPermission")
    private BluetoothDevice getBluetoothDeviceByMac(String... bluetoothDevicesMac) throws BluetoothException {
        // 1. 校验蓝牙权限（已在MainActivity启动时获取）
        if (!XXPermissions.isGranted(mContext, new String[]{
                Permission.BLUETOOTH_SCAN,
                Permission.BLUETOOTH_CONNECT,
                Permission.BLUETOOTH_ADVERTISE})) {
            throw new BluetoothException("未获取蓝牙权限，请先获取蓝牙权限");
        }

        // 2.检查是否打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mContext.startActivity(intent);
        }

        // 3. 根据 mac 地址获取蓝牙设备
        BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(bluetoothDevicesMac[0]);
        if (remoteDevice == null) {
            throw new BluetoothException("通过 " + bluetoothDevicesMac[0] + " 未获取任何蓝牙设备，请检查输入的 mac 地址是否正确。");
        }

        return remoteDevice;
    }

    /**
     * 将电子秤返回过来的重量数据，转换为正常数据
     * 电子秤返回的数据格式：
     * - "ST,NT,-  66.66kg\r\n"
     * - "US,NT,-  66660 g\r\n"
     * @param weightFromElectronicWeigher
     * @return
     */
    private String converterWeight(String weightFromElectronicWeigher) {
        // 1. 去除多余空格 & \r\n
        weightFromElectronicWeigher = weightFromElectronicWeigher.replaceAll("\r", "");
        weightFromElectronicWeigher = weightFromElectronicWeigher.replaceAll("\n", "");
        weightFromElectronicWeigher = weightFromElectronicWeigher.replace(" ", "");

        // 2. 通过 , 将字符串分段
        String[] split = weightFromElectronicWeigher.split(",");

        return split[2];
    }
}
