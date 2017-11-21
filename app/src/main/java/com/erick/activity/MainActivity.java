package com.erick.activity;

import java.io.File;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.erick.listener.DownloadProgressListener;
import com.erick.network.FileDownloader;
import com.erick.service.FileService;

public class MainActivity extends Activity {
    private static String TAG = "MainActivity";

    private EditText downloadpathText;
    private TextView resultView;
    private ProgressBar progressBar;
    private FileService.DownloadBinder mService;

    /**
     * 当Handler被创建会关联到创建它的当前线程的消息队列，该类用于往消息队列发送消息
     * 消息队列中的消息由当前线程内部进行处理
     */
    private Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    progressBar.setProgress(msg.getData().getInt("size"));
                    float num = (float)progressBar.getProgress()/(float)progressBar.getMax();
                    int result = (int)(num*100);
                    resultView.setText(result+ "%");

                    if(progressBar.getProgress()==progressBar.getMax()){
                        Toast.makeText(MainActivity.this, R.string.success, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case -1:
                    Toast.makeText(MainActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        downloadpathText = (EditText) this.findViewById(R.id.path);
        progressBar = (ProgressBar) this.findViewById(R.id.downloadbar);
        resultView = (TextView) this.findViewById(R.id.resultView);
        Button button = (Button) this.findViewById(R.id.button);

        Intent intent = new Intent(this,FileService.class);
        bindService(intent,mConn, Context.BIND_AUTO_CREATE);

        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String path = downloadpathText.getText().toString();
                System.out.println(Environment.getExternalStorageState()+"------"+Environment.MEDIA_MOUNTED);
                Log.d(TAG, "onClick: " + Environment.getExternalStorageState()+"------"+Environment.MEDIA_MOUNTED);

                if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                    download(path, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
                    Log.d(TAG, "onClick: 文件存储位置：" +  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
                }else{
                    Toast.makeText(MainActivity.this, R.string.sdcarderror, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onClick: " + getString(R.string.sdcarderror));
                }
            }
        });
    }

    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = (FileService.DownloadBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    /**
     * 主线程(UI线程)
     * 对于显示控件的界面更新只是由UI线程负责，如果是在非UI线程更新控件的属性值，更新后的显示界面不会反映到屏幕上
     * @param path
     * @param savedir
     */
    private void download(final String path, final File savedir) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileDownloader loader = new FileDownloader(MainActivity.this, mService,path, savedir, 3);
                progressBar.setMax(loader.getFileSize());//设置进度条的最大刻度为文件的长度

                try {
                    loader.download(new DownloadProgressListener() {
                        @Override
                        public void onDownloadSize(int size) {//实时获知文件已经下载的数据长度
                            Message msg = new Message();
                            msg.what = 1;
                            msg.getData().putInt("size", size);
                            handler.sendMessage(msg);//发送消息
                        }
                    });
                } catch (Exception e) {
                    handler.obtainMessage(-1).sendToTarget();
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConn);
    }
}