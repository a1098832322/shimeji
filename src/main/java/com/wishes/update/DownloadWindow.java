package com.wishes.update;

import lombok.Getter;

import javax.swing.*;

/**
 * @Author:郑龙
 * @Date:2018-11-06 15:33
 * @Description:下载窗口
 */
public class DownloadWindow extends JFrame {
    /**
     * 显示控件
     */
    public void callBack() {
        //将控件添加进panel
        mPanel.add(warnText);
        mPanel.add(btnOK);
        //显示控件
        this.warnText.setVisible(true);
        this.btnOK.setVisible(true);

        //显示下载完成
        this.setTitle("下载完成");
    }


    /**
     * 加载控件
     *
     * @param panel
     */
    public void loadComponent(JPanel panel) {
        //重设窗口大小
        this.setSize(this.getWidth(), this.getHeight() + 50);
        //添加panel
        this.mPanel.add(panel);
        //刷新panel
        mPanel.revalidate();
    }

    /**
     * 主窗体面板
     */
    @Getter
    private JPanel mPanel;

    /**
     * 下载完成后的提示语
     */
    private JLabel warnText;

    /**
     * 确定按钮
     */
    private JButton btnOK;


    /**
     * 默认显示的窗口宽高
     */
    public static final int Frame_Height = 80;
    public static final int FRAME_WIDTH = 240;

    public DownloadWindow(String title) {
        //设置标题头
        this.setTitle(title == null ? "下载" : title);
        this.setSize(FRAME_WIDTH, Frame_Height);
        this.setLocationRelativeTo(null);
        this.setResizable(false);
        //this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mPanel = new JPanel();

        warnText = new JLabel("请在关闭程序后重新运行新的程序包！");
        warnText.setVisible(false);

        //设置按钮文字
        btnOK = new JButton("关闭程序");
        //默认隐藏按钮
        btnOK.setVisible(false);
        //点击事件,点击关闭
        btnOK.addActionListener(e -> System.exit(0));

        this.setContentPane(mPanel);

        //显示窗口
        this.setVisible(true);
    }

    /**
     * 自定义的panel
     */
    public static abstract class ProgressPanel extends JPanel {
        private JLabel label;
        private JProgressBar progressBar;
        /**
         * 进度条最小值为0不变
         */
        private static final int MIN = 0;

        /**
         * 最大值根据文件大小的变化而变化,默认100
         */
        private static final int MAX = 100;

        public ProgressPanel() {
            label = new JLabel("下载中...");
            progressBar = new JProgressBar();
            // 绘制百分比文本（进度条中间显示的百分数）
            progressBar.setStringPainted(true);
            progressBar.setMinimum(MIN);
            progressBar.setMaximum(MAX);
            progressBar.setValue(0);//初始化时置零
            //设置监听事件
            progressBar.addChangeListener(e -> setOnChangeListener());

            this.add(label, 0);
            this.add(progressBar, 1);
        }

        /**
         * 设置进度条进度
         *
         * @param value
         */
        public void setValue(int value) {
            this.progressBar.setValue(value);
        }

        /**
         * 是否下载完成
         *
         * @return
         */
        public boolean isComplete() {
            return this.progressBar.getValue() == this.progressBar.getMaximum()
                    && this.progressBar.getPercentComplete() == 1.0;
        }


        /**
         * 设置一些参数
         *
         * @param text
         * @param maxProgressValue
         * @return
         */
        public ProgressPanel build(String text, int maxProgressValue) {
            this.label.setText(text);
            this.progressBar.setMaximum(maxProgressValue);
            return this;
        }

        /**
         * 自定义进度条改变事件
         */
        public abstract void setOnChangeListener();
    }

}
