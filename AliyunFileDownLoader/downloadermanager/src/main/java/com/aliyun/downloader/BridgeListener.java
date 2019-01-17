/*
 * Copyright (C) 2010-2017 Alibaba Group Holding Limited.
 */

package com.aliyun.downloader;

import android.os.AsyncTask;
import android.util.Log;

import com.aliyun.common.utils.StringUtils;
import com.aliyun.downloader.zipprocessor.ZIPFileProcessor;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.util.FileDownloadUtils;

import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;

class BridgeListener extends FileDownloadListener {

    private CopyOnWriteArrayList<FileDownloaderCallback> mListenerList;

    private BaseDownloadTask mDownloadTask;
    private FileDownloaderCallback mGlobleDownloadCallback;
    //开始下载时间，用户计算加载速度
    private long mPreviousTime;
    private long mSpeed;//下载速度
    private long mOldSoFarBytes;

    public BridgeListener(){
        mGlobleDownloadCallback = DownloaderManager.getInstance().getGlobalDownloadCallback();
        mListenerList = new CopyOnWriteArrayList<>();
    }

    public void setDownloadTask(BaseDownloadTask task) {
        this.mDownloadTask = task;
    }

    public BaseDownloadTask getDownloadTask() {
        return this.mDownloadTask;
    }

    @Override
    protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
        int preProgress = 0;
        if ( totalBytes != 0 ) {
            preProgress = (int)(soFarBytes / (float)totalBytes * 100);
        }

        for(FileDownloaderCallback listener: mListenerList) {
            if (listener != null) {
                listener.onStart(task.getDownloadId(), soFarBytes, totalBytes, preProgress);
            }
        }

        if (mGlobleDownloadCallback != null) {
            mGlobleDownloadCallback.onStart(task.getDownloadId(), soFarBytes, totalBytes, preProgress);
        }
    }

    @Override
    protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
        super.connected(task, etag, isContinue, soFarBytes, totalBytes);
        int preProgress = 0;
        if ( totalBytes != 0 ) {
            preProgress = (int)(soFarBytes / (float)totalBytes * 100);
        }
        for(FileDownloaderCallback listener: mListenerList) {
            if (listener != null) {
                listener.onStart(task.getDownloadId(), soFarBytes, totalBytes, preProgress);
            }
        }

        if (mGlobleDownloadCallback != null) {
            mGlobleDownloadCallback.onStart(task.getDownloadId(), soFarBytes, totalBytes, preProgress);
        }
        mOldSoFarBytes = soFarBytes;
        mPreviousTime = System.currentTimeMillis();
    }

    @Override
    protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
        int progress = 0;
        if ( totalBytes != 0 ) {
            progress = (int)(soFarBytes / (float)totalBytes * 100);
        }

        //计算下载速度
        long totalTime = (System.currentTimeMillis() - mPreviousTime)/1000;
        if ( totalTime == 0 ) {
            totalTime += 1;
        }
        long speed = (soFarBytes - mOldSoFarBytes) / totalTime;

        for(FileDownloaderCallback listener: mListenerList) {
            if (listener != null) {
                listener.onProgress(task.getDownloadId(), soFarBytes, totalBytes, speed, progress);
            }
        }

        mSpeed = speed;

        if (mGlobleDownloadCallback != null) {
            mGlobleDownloadCallback.onProgress(task.getDownloadId(), soFarBytes, totalBytes, speed, progress);
        }
    }

    @Override
    protected void blockComplete(BaseDownloadTask task) {
    }

    @Override
    protected void completed(final BaseDownloadTask task) {
        for(final FileDownloaderCallback listener: mListenerList) {
            if (listener != null) {
                //解压并且更新解压地址
                final FileDownloaderModel model = DownloaderManager.getInstance().getFileDownloaderModelById(task.getId());
                if(model != null) {
                    new AsyncTask() {
                        @Override
                        protected Object doInBackground(Object[] objects) {
                            File outFile = null;
                            if(model.isunzip() == 1) {
                                model.setIsunzip(0);
                                if (new File(task.getPath()).isDirectory()){
                                    outFile = new File(task.getPath());//添加到数据库
                                    DownloaderManager.getInstance().getDbController().addTask(model);
                                    return outFile;
                                }

                                File sourceFile = new File(task.getPath());
                                File descFile = new File(task.getPath() + "tmp");
                                boolean isRename = sourceFile.renameTo(descFile);
                                //File descFile = new File(task.getPath() + "tmp");
                                //if(sourceFile.exists() && !sourceFile.isDirectory()) {
                                //    try {
                                //        FileUtils.copyFile(sourceFile, descFile, true);
                                //    } catch (Exception e) {
                                //        e.printStackTrace();
                                //    } finally {
                                //        if(sourceFile.exists()) {
                                //            sourceFile.delete();
                                //        }
                                //    }
                                //}

                                File file = new File(FileDownloadUtils.getDefaultSaveRootPath(), StringUtils.subString(task.getPath()));
                                boolean success = file.mkdirs();

                                if(success && descFile.exists() && isRename) {
                                    ZIPFileProcessor zipFileProcessor = new ZIPFileProcessor(file, task.getDownloadId());
                                    outFile = zipFileProcessor.process(descFile);
                                    if(outFile != null) {
                                        DownloaderManager.getInstance().getDbController().addTask(model);
                                    }
                                }else{
                                    Log.e("process","not process file is "+descFile.getAbsolutePath()+" success is " +success + " isRename is " + isRename);
                                }
                            } else {
                                outFile = new File(model.getPath());//添加到数据库
                                DownloaderManager.getInstance().getDbController().addTask(model);
                            }
                            return outFile;
                        }

                        @Override
                        protected void onPostExecute(Object o) {
                            super.onPostExecute(o);
                            listener.onFinish(task.getDownloadId(), task.getPath());
                            removeDownloadListener(listener);
                        }
                    }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                }
            }
        }
        if (mGlobleDownloadCallback != null) {
            mGlobleDownloadCallback.onFinish(task.getDownloadId(), task.getPath());
        }
        nextTask(task.getDownloadId());
    }

    @Override
    protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
        stop(task.getDownloadId(), soFarBytes, totalBytes);
    }

    @Override
    protected void error(BaseDownloadTask task, Throwable e) {
        long totalBytes = task.getLargeFileTotalBytes();
        long soFarBytes = task.getLargeFileSoFarBytes();
        for(FileDownloaderCallback listener: mListenerList) {
            if (listener != null) {
                listener.onError(task, e);
            }
        }

        if (mGlobleDownloadCallback != null) {
            mGlobleDownloadCallback.onError(task, e);
        }
   }

    @Override
    protected void warn(BaseDownloadTask task) {
    }

    protected void stop(int downloadId, long soFarBytes, long totalBytes) {
        int progress = 0;
        if ( totalBytes != 0 ) {
            progress = (int)(soFarBytes / (float)totalBytes * 100);
        }
        for(FileDownloaderCallback listener: mListenerList) {
            if (listener != null) {
                listener.onStop(downloadId, soFarBytes, totalBytes, progress);
            }
        }

        if (mGlobleDownloadCallback != null) {
            mGlobleDownloadCallback.onStop(downloadId, soFarBytes, totalBytes, progress);
        }
        nextTask(downloadId);
    }

    public void wait(int downloadId) {
        for(FileDownloaderCallback listener: mListenerList) {
            if (listener != null) {
                listener.onWait(downloadId);
            }
        }
    }

    public void addDownloadListener(FileDownloaderCallback listener) {
        if ( listener != null && !mListenerList.contains(listener)) {
            mListenerList.add(listener);
        }
    }

    public void removeDownloadListener(FileDownloaderCallback listener) {
        if (listener != null && mListenerList.contains(listener)) {
            mListenerList.remove(listener);
        }
    }

    public void removeAllDownloadListener() {
        mListenerList.clear();
    }

    private void nextTask(int curDownloadId) {
        DownloaderManager.getInstance().removeDownloadingTask(curDownloadId);
        FileDownloaderModel model = DownloaderManager.getInstance().nextTask();
        if (model != null) {
            DownloaderManager.getInstance().startTask(model.getTaskId());
        }
    }

    protected long getSpeed() {
        return mSpeed;
    }

}