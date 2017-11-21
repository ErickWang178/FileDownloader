package com.erick.service;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.erick.database.DBOpenHelper;

import java.util.HashMap;
import java.util.Map;

public class FileService extends Service{
    private DBOpenHelper openHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        openHelper = new DBOpenHelper(getApplication());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private DownloadBinder mBinder = new DownloadBinder();

    public class DownloadBinder extends Binder{
        /**
         * 获取每条线程已经下载的文件长度
         */
        public Map<Integer, Integer> getData(String path){
            SQLiteDatabase db = openHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("select threadid, downlength from filedownlog where downpath=?", new String[]{path});
            Map<Integer, Integer> data = new HashMap<Integer, Integer>();

            while(cursor.moveToNext()){
                data.put(cursor.getInt(0), cursor.getInt(1));
            }
            cursor.close();
            db.close();
            return data;
        }

        /**
         * 保存每条线程已经下载的文件长度
         */
        public void save(String path,  Map<Integer, Integer> map){//int threadid, int position
            SQLiteDatabase db = openHelper.getWritableDatabase();
            db.beginTransaction();
            try{
                for(Map.Entry<Integer, Integer> entry : map.entrySet()){
                    db.execSQL("insert into filedownlog(downpath, threadid, downlength) values(?,?,?)",
                            new Object[]{path, entry.getKey(), entry.getValue()});
                }
                db.setTransactionSuccessful();
            }finally{
                db.endTransaction();
            }
            db.close();
        }

        /**
         * 实时更新每条线程已经下载的文件长度
         */
        public void update(String path, Map<Integer, Integer> map){
            SQLiteDatabase db = openHelper.getWritableDatabase();
            db.beginTransaction();
            try{
                for(Map.Entry<Integer, Integer> entry : map.entrySet()){
                    db.execSQL("update filedownlog set downlength=? where downpath=? and threadid=?",
                            new Object[]{entry.getValue(), path, entry.getKey()});
                }
                db.setTransactionSuccessful();
            }finally{
                db.endTransaction();
            }
            db.close();
        }

        /**
         * 当文件下载完成后，删除对应的下载记录
         */
        public void delete(String path){
            SQLiteDatabase db = openHelper.getWritableDatabase();
            db.execSQL("delete from filedownlog where downpath=?", new Object[]{path});
            db.close();
        }
    };





}
