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
     * @return Version
     */
    public static Version checkUpdate() throws IOException {
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

                //判断是否需要更新
                if (getCurrentVersionInfo().isNewestVersion(version)) {
                    //如果是最新版则不需要更新
                    mCurrentVersion = version;
                    return null;
                } else {
                    //否则需要更新
                    return version;
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
            //下载子节点
            if (mData.getNodeList() != null && mData.getNodeList().size() > 0) {
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
                                    //当进度条走满时，检测其他
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
                                                //window.callBack();
                                            }
                                        }
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

            //下载主程序
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
         * 判断是否是最新版本
         *
         * @param v
         * @return
         */
        public boolean isNewestVersion(Version v) {
            return getVersion().compareTo(v.getVersion()) < 0 ? false : true;
        }
    }
}
