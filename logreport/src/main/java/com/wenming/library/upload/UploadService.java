package com.wenming.library.upload;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

import com.wenming.library.LogReport;
import com.wenming.library.util.CompressUtil;
import com.wenming.library.util.FileUtil;
import com.wenming.library.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by wenmingvs on 2016/7/9.
 */
public class UploadService extends Service {

    public static final String TAG = "UploadService";

    public static final String logdir = LogReport.getInstance().LOGDIR;


    /**
     * 压缩包名称的一部分：时间戳
     */
    public final static SimpleDateFormat ZIP_FOLDER_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS", Locale.getDefault());

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.d("wenming", "logdir = " + LogReport.getInstance().LOGDIR + "Log/");
        File logdir = new File(LogReport.getInstance().LOGDIR + "Log/");
        File zipdir = new File(LogReport.getInstance().LOGDIR + "AlreadyUploadLog/");
        File zipfile = new File(zipdir, "UploadOn" + ZIP_FOLDER_TIME_FORMAT.format(System.currentTimeMillis()) + ".zip");
        StringBuilder content = new StringBuilder();

        LogUtil.d("wenming", "压缩路径 Logdir = " + logdir.getAbsolutePath());
        LogUtil.d("wenming", "保存zip的 Logdir = " + zipfile.getAbsolutePath());

        if (logdir.exists() && logdir.listFiles().length > 0) {
            if (!zipdir.exists()) {
                zipdir.mkdirs();
            }
            if (!zipfile.exists()) {
                try {
                    zipfile.createNewFile();
                    if (CompressUtil.zipFileAtPath(logdir.getAbsolutePath(), zipfile.getAbsolutePath())) {

                        ArrayList<File> crashFileList = FileUtil.getCrashList(logdir);
                        for (File crash : crashFileList) {
                            content.append(FileUtil.getText(crash));
                            content.append("\n");
                        }
                        if (TextUtils.isEmpty(content)) {
                            content.append("No crash content");
                        }
                        FileUtil.deleteDir(logdir);
                        LogReport.getInstance().getUpload().sendFile(zipfile, this, content.toString());
                    }

                } catch (IOException e) {
                    LogUtil.e("wenming", e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            LogUtil.d("wenming", "本地没有崩溃日志，无需上传");
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 检查文件夹是否超出缓存大小
     *
     * @param dir
     * @return
     */

    //TODO 调用的司机，应该是发送完成后再删除
    public boolean checkCacheSize(File dir) {
        long dirSize = FileUtil.folderSize(dir);
        if (dirSize > LogReport.getInstance().getCacheSize()) {
            LogUtil.d("wenming", "超出大小，删除所以缓存文件");
            return FileUtil.deleteDir(dir);
        } else {
            return false;
        }
    }


}
