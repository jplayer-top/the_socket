package com.example.sockettest;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends AppCompatActivity {
    public EditText editText;
    public TextView textView_send;
    public TextView textView_receive;
    private EditText mEditAudio;
    private Button mBtnPlay;
    private Button mBtnStop;
    private EditText mEditIP;
    private EditText mEditPort;
    private Disposable mSubscribe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AndPermission.with(this)
                .permission(Permission.CAMERA,
                        Permission.RECORD_AUDIO,
                        Permission.WRITE_EXTERNAL_STORAGE,
                        Permission.ACCESS_COARSE_LOCATION)
                .onGranted(permissions -> {
                })
                .onDenied(permissions -> AndPermission.hasAlwaysDeniedPermission(this, permissions))
                .start();
        editText = findViewById(R.id.send_editText);
        mEditAudio = findViewById(R.id.editAudio);
        mEditIP = findViewById(R.id.editIP);
        mEditPort = findViewById(R.id.editPort);
        mBtnPlay = findViewById(R.id.btnPlay);
        mBtnStop = findViewById(R.id.btnStop);
        textView_send = findViewById(R.id.send_textView);
        textView_receive = findViewById(R.id.receive_textView);
        textView_send.setMovementMethod(ScrollingMovementMethod.getInstance());
        textView_receive.setMovementMethod(ScrollingMovementMethod.getInstance());

        mEditAudio.setText("/storage/emulated/0/tencent/micromsg/download/iphone.mp3");
        mBtnPlay.setOnClickListener(v -> {
            playAudio();
        });
        mBtnStop.setOnClickListener(v -> {
            MediaManager.release();
        });
        mSubscribe = getSubscribe();
    }
    private Disposable getSubscribe() {
        return Observable.interval(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    System.out.println(TaskCenter.sharedCenter().isConnected() + "---------------");
                    if (!isConnectByMe) {
                        Toast.makeText(this, "正在尝试 connect" + mEditIP.getText().toString() + ":" + mEditPort.getText().toString(),
                                LENGTH_SHORT).show();
                        thisToConnect();
                    }
                });
    }
    private void playAudio() {
        MediaManager.playSound(mEditAudio.getText().toString(), mp -> {

        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSubscribe != null && !mSubscribe.isDisposed()) {
            mSubscribe.dispose();
        }
    }



    public void sendMessage(View view) {
        String msg = editText.getText().toString();
        String text = textView_send.getText().toString() + msg + "\n";
        textView_send.setText(text);
        TaskCenter.sharedCenter().send(msg.getBytes());
    }

    public void connect(View view) {
//        "43.229.152.34"
        thisToConnect();
    }


    @SuppressLint("CheckResult")
    private void thisToConnect() {
        TaskCenter.sharedCenter().connect(mEditIP.getText().toString(), Integer.parseInt(mEditPort.getText().toString()));
        TaskCenter.sharedCenter().setReceivedCallback(receicedMessage -> {
            Observable.just(1)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(integer -> {
                        String text = textView_receive.getText().toString() + receicedMessage + "\n";
                        textView_receive.setText(text);
                        if (text.contains("arning")) {
                            playAudio();
                        }
                    });
        });
        TaskCenter.sharedCenter().setDisconnectedCallback(e -> {
            Observable.just(e.getMessage())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(msg -> {
                        String text = textView_receive.getText().toString() + "断开连接即将重试" + "\n";
                        if (!msg.contains("Connection timed out")) {
                            textView_receive.setText(text);
                        }
                        if (msg.contains("Connection reset")) {
                            isConnectByMe =false;
                            mSubscribe = getSubscribe();
                        }
                    });
        });
        TaskCenter.sharedCenter().setConnectedCallback(() -> {
            Observable.just(1)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(integer -> {
                        String text = textView_receive.getText().toString() + "连接成功" + "\n";
                        textView_receive.setText(text);
                        isConnectByMe = true;
                    });
        });
    }

    public boolean isConnectByMe = false;

    public void disconnect(View view) {
        TaskCenter.sharedCenter().removeCallback();
        TaskCenter.sharedCenter().disconnect();
    }

    public void clear1(View view) {
        textView_send.setText("");
    }

    public void clear2(View view) {
        textView_receive.setText("");
    }


}
