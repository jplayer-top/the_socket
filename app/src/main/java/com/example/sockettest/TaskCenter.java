package com.example.sockettest;

import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by shensky on 2018/1/15.
 */

public class TaskCenter {
    private static TaskCenter instance;
    private static final String TAG = "TaskCenter";
    //    Socket
    private Socket socket;
    //    IP地址
    public String ipAddress;
    //    端口号
    public int port;
    public Thread thread;
    //    Socket输出流
    public OutputStream outputStream;
    //    Socket输入流
    public InputStream inputStream;
    //    连接回调
    public OnServerConnectedCallbackBlock connectedCallback;
    //    断开连接回调(连接失败)
    public OnServerDisconnectedCallbackBlock disconnectedCallback;
    //    接收信息回调
    public OnReceiveCallbackBlock receivedCallback;

    //    构造函数私有化
    private TaskCenter() {
        super();
    }

    //    提供一个全局的静态方法
    public static TaskCenter sharedCenter() {
        if (instance == null) {
            synchronized (TaskCenter.class) {
                if (instance == null) {
                    instance = new TaskCenter();
                }
            }
        }
        return instance;
    }

    /**
     * 通过IP地址(域名)和端口进行连接
     *
     * @param ipAddress IP地址(域名)
     * @param port      端口
     */
    public void connect(final String ipAddress, final int port) {

        thread = new Thread(() -> {
            try {
                socket = new Socket(ipAddress, port);
                if (isConnected()) {
                    TaskCenter.sharedCenter().ipAddress = ipAddress;
                    TaskCenter.sharedCenter().port = port;
                    if (connectedCallback != null) {
                        connectedCallback.callback();
                    }
                    outputStream = socket.getOutputStream();
                    inputStream = socket.getInputStream();
                    receive();
                    Log.i(TAG, "连接成功");
                } else {
                    Log.i(TAG, "连接失败");
                    if (disconnectedCallback != null) {
                        disconnectedCallback.callback(new IOException("连接失败"));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();

                Log.e(TAG, "连接异常");
                if (disconnectedCallback != null) {
                    disconnectedCallback.callback(e);
                }
            }
        });
        thread.start();
    }

    /**
     * 判断是否连接
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    /**
     * 连接
     */
    public void connect() {
        connect(ipAddress, port);
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (isConnected()) {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                socket.close();
                if (socket.isClosed()) {
                    if (disconnectedCallback != null) {
                        disconnectedCallback.callback(new IOException("断开连接"));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 接收数据
     */
    public void receive() {
        while (isConnected()) {
            try {
                /**得到的是16进制数，需要进行解析*/
                byte[] bt = new byte[1024];
//                获取接收到的字节和字节数
                int length = inputStream.read(bt);
//                获取正确的字节
                if (length > -1) {
                    byte[] bs = new byte[length];
                    System.arraycopy(bt, 0, bs, 0, length);

                    String str = new String(bs, "GBK");
                    if (str != null) {
                        if (receivedCallback != null) {
                            receivedCallback.callback(str);
                        }
                    }
                    Log.i(TAG, "接收成功");
                    isReset = false;
                }
            } catch (Exception e) {
                Log.i(TAG, "接收失败" + e.getMessage() + isReset);
                SystemClock.sleep(3000);
                if (!isReset && e.getMessage().contains("Connection reset")) {
                    if (disconnectedCallback != null) {
                        disconnectedCallback.callback(e);
                    }
                    if (socket != null) {
                        try {
                            socket.close();
                            connect();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                    isReset = true;
                }
            }
        }
    }
    public volatile boolean isReset = false;
    /**
     * 发送数据
     *
     * @param data 数据
     */
    public void send(final byte[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (socket != null) {
                    try {
                        outputStream.write(data);
                        outputStream.flush();
                        Log.i(TAG, "发送成功");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i(TAG, "发送失败");
                    }
                } else {
                    connect();
                }
            }
        }).start();

    }

    /**
     * 回调声明
     */
    public interface OnServerConnectedCallbackBlock {
        void callback();
    }

    public interface OnServerDisconnectedCallbackBlock {
        void callback(Exception e);
    }

    public interface OnReceiveCallbackBlock {
        void callback(String receicedMessage);
    }

    public void setConnectedCallback(OnServerConnectedCallbackBlock connectedCallback) {
        this.connectedCallback = connectedCallback;
    }

    public void setDisconnectedCallback(OnServerDisconnectedCallbackBlock disconnectedCallback) {
        this.disconnectedCallback = disconnectedCallback;
    }

    public void setReceivedCallback(OnReceiveCallbackBlock receivedCallback) {
        this.receivedCallback = receivedCallback;
    }

    /**
     * 移除回调
     */
    public void removeCallback() {
        connectedCallback = null;
        disconnectedCallback = null;
        receivedCallback = null;
    }
}