package com.wishes.utils;

import com.alibaba.fastjson.JSONObject;
import com.wishes.constant.Constant;
import com.wishes.fix.OriginEngineFix;
import com.wishes.update.DataAnalysis;
import com.wishes.update.DownloadWindow;
import com.wishes.update.Node;
import lombok.Data;
import okhttp3.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @Author:郑龙
 * @Date:2018-11-06 14:04
 * @Description:更新升级检测
 */
public class UpdateChecker {
    /**
     * logger
     */
    private static Logger logger = Logger.getLogger(UpdateChecker.class);

    private static Version mCurrentVersion = new Version();

    /**
     * 检测更新方式
     */
    public enum CHECK_UPDATE_TYPE {
        /**
         * 程序在启动时自动检测
         */
        AUTO,
        /**
         * 用户手动点击更新
         */
        MANUAL
    }

    /**
     * 更新细节
     */
    private static DataAnalysis mData = null;

    /**
     * 指定下载文件夹
     */
    private static final String DOWNLOAD_PATH = OriginEngineFix.getInstance()
            .getBASE_PATH() + "\\download";

    /**
     * 更新Server地址
     */
    private static final String UPDATE_URL = Constant.UPDATE_JSON_URL;

    /**
     * 获得当前版本信息
     *
     * @return CurrentVersion's information
     */
    public static Version getCurrentVersionInfo() {
        return mCurrentVersion == null ? new Version() : mCurrentVersion;
    }

    /**
     * 检测是否有更新<br>
     * 是：返回版本号和更新内容及下载URL<br>
     * 否：返回null
     *
     * @param checkType 来源
     * @return Version
     */
    public static Version checkUpdate(CHECK_UPDATE_TYPE checkType) throws IOException {
        //网络请求
        OkHttpClient mClient = new OkHttpClient();
        Request request = new Request.Builder().url(UPDATE_URL).get().build();
        Response response = mClient.newCall(request).execute();
        if (response.isSuccessful()) {
            String json = response.body().string();

            //解析json
            try {
                //解析JSON
                DataAnalysis data = JSONObject.parseObject(json, DataAnalysis.class);
                Version version = new Version(data.getVersion()
                        , data.getMessage(), data.getUrl());
                //赋值到本地
                mData = data;

                //判断需要更新的内容，当且仅当仅需要更新资源文件且方式为自动更新时，不显示弹窗，其他时刻按照正常逻辑显示.
                if (Constant.UPDATE_TYPE.CHILDREN.getType().equals(mData.getUpdateType())) {
                    if (CHECK_UPDATE_TYPE.AUTO.equals(checkType)) {
                        //仅更新资源文件时自动更新不提示
                        if (getCurrentVersionInfo().isNewestVersion(version) >= 0) {
                            mCurrentVersion = version;
                        }

                        return null;
                    }
                }

                //手动更新时接收所有更新提示
                if (CHECK_UPDATE_TYPE.MANUAL.equals(checkType)) {
                    //为手动更新模式时，当版本与服务器版本相同时会弹出更新选项框
                    if (getCurrentVersionInfo().isNewestVersion(version) > 0) {
                        //如果是最新版则不需要更新
                        mCurrentVersion = version;
                        return null;
                    } else {
                        //否则需要更新
                        return version;
                    }
                } else if (CHECK_UPDATE_TYPE.AUTO.equals(checkType)) {
                    //自动更新时
                    if (getCurrentVersionInfo().isNewestVersion(version) >= 0) {
                        mCurrentVersion = version;
                        return null;
                    } else {
                        return version;
                    }
                }

            } catch (Exception e) {
                logger.error("更新包json解析失败！更新失败！", e);
            }
        }

        return null;
    }

    /**
     * 下载更新方法
     *
     * @param window
     * @throws IOException
     */
    public static void download(DownloadWindow window) throws IOException {
        if (mData != null) {
            //当有资源文件(子节点)有更新时，才下载子节点
            if (mData.getNodeList() != null && mData.getNodeList().size() > 0
                    && !Constant.UPDATE_TYPE.MAIN.getType().equals(mData.getUpdateType())) {
                for (Node node : mData.getNodeList()) {
                    OkHttpClient mNodeClient = new OkHttpClient();
                    Request nodeRequest = new Request.Builder().url(node.getDownLoadURL()).get().build();
                    mNodeClient.newCall(nodeRequest).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            logger.error("下载子文件更新失败！更新失败！");
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            //得到流
                            InputStream is = response.body().byteStream();
                            //自定义下载进度条panel
                            DownloadWindow.ProgressPanel panel = new DownloadWindow.ProgressPanel() {
                                @Override
                                public void setOnChangeListener() {
                                    //当只有资源文件需要更新的时候，让Node中的资源下载完成后实现回调
                                    if (Constant.UPDATE_TYPE.CHILDREN
                                            .getType().equals(mData.getUpdateType())) {
                                        //当进度条走满时，检测其他
                                        showControlPanel(window);
                                    }
                                }
                            }.build(node.getName(), (int) response.body().contentLength());

                            //加载并重绘panel
                            window.loadComponent(panel);

                            //转换为byte[]存储
                            byte[] buf = new byte[2048];
                            FileOutputStream fos = null;
                            // 储存下载文件的目录
                            int len = 0;
                            File file = new File(DOWNLOAD_PATH, node.getDeployPath() + node.getName());
                            //如果不存在文件夹则新建
                            if (!file.getParentFile().exists()) {
                                boolean result = file.getParentFile().mkdirs();
                                if (!result) {
                                    logger.error("文件夹创建失败！");
                                }
                            }

                            //以覆盖的方式写入文件
                            fos = new FileOutputStream(file, false);
                            long sum = 0;
                            while ((len = is.read(buf)) != -1) {
                                fos.write(buf, 0, len);
                                sum += len;

                                // 下载中
                                panel.setValue((int) sum);
                            }
                            // 下载完成
                            fos.flush();
                        }
                    });


                }
            }

            //判断有无主程序更新
            if (mData.getUrl() == null || Constant.UPDATE_TYPE
                    .CHILDREN.getType().equals(mData.getUpdateType())) {
                //主程序无更新
            } else {
                //有更新时下载主程序
                OkHttpClient mClient = new OkHttpClient();
                Request request = new Request.Builder().url(mData.getUrl()).get().build();
                mClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        logger.error("下载主程序更新包失败！更新失败！");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        //得到流
                        InputStream is = response.body().byteStream();
                        //自定义下载进度条panel
                        DownloadWindow.ProgressPanel panel = new DownloadWindow.ProgressPanel() {
                            @Override
                            public void setOnChangeListener() {
                                if (this.isComplete()) {
                                    //当进度条走满时，检测其他
                                    showControlPanel(window);
                                }
                            }
                        }.build("主程序：", (int) response.body().contentLength());

                        //加载并重绘panel
                        window.loadComponent(panel);


                        //转换为byte[]存储
                        byte[] buf = new byte[2048];
                        FileOutputStream fos = null;
                        // 储存下载文件的目录
                        int len = 0;
                        File file = new File(DOWNLOAD_PATH, getNameFromUrl(mData.getUrl()));
                        //如果不存在文件夹则新建
                        if (!file.getParentFile().exists()) {
                            boolean result = file.getParentFile().mkdirs();
                            if (!result) {
                                logger.error("文件夹创建失败！");
                            }
                        }

                        //以覆盖方式写入文件
                        fos = new FileOutputStream(file, false);
                        long sum = 0;
                        while ((len = is.read(buf)) != -1) {
                            fos.write(buf, 0, len);
                            sum += len;

                            // 下载中
                            panel.setValue((int) sum);
                        }
                        // 下载完成
                        fos.flush();
                    }
                });
            }


        }
    }

    /**
     * 检测并显示下载窗口的控制按钮
     *
     * @param window
     */
    private static void showControlPanel(DownloadWindow window) {
        int componentCount = window.getMPanel().getComponentCount();
        for (int i = 0; i < componentCount; i++) {
            //如果是进度条panel
            Object obj = window.getMPanel().getComponent(i);
            if (obj instanceof DownloadWindow.ProgressPanel) {
                DownloadWindow.ProgressPanel p = (DownloadWindow.ProgressPanel) obj;
                if (!p.isComplete()) {
                    //如果未完成则break掉
                    break;
                }
                if (i == componentCount - 1 && p.isComplete()) {
                    //如果最后一个已完成,则显示其他控件
                    window.callBack();
                }
            }
        }
    }

    /**
     * 从下载连接中解析出文件名
     *
     * @param url
     * @return fileName
     */
    private static String getNameFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    /**
     * 记录当前版本
     */
    @Data
    public static class Version {
        /**
         * 版本
         */
        private String Version = Constant.CURRENT_VERSION;

        /**
         * 新版本特性
         */
        private String whatNew = "公开测试";

        /**
         * 更新包主程序下载地址
         */
        private String downloadURL = Constant.DEFAULT_JAR_DOWNLOAD_URL;

        public Version(String version, String whatNew, String downloadURL) {
            Version = version;
            this.whatNew = whatNew;
            this.downloadURL = downloadURL;
        }

        public Version() {
        }

        /**
         * 判断是否是最新版本<br>
         * -1:否，需要更新<br>
         * 0:是最新版本<br>
         * 1:是最新版本(测试时可能会有，但是一般不会出现)
         *
         * @param v
         * @return
         */
        public int isNewestVersion(Version v) {
            return getVersion().compareTo(v.getVersion());
        }
    }
}
