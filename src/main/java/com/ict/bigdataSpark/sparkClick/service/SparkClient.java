package com.ict.bigdataSpark.sparkClick.service;

import com.ict.bigdataSpark.sparkClick.constants.SparkConstants;
import com.ict.bigdataSpark.sparkClick.domain.SparkResult;
import org.apache.spark.launcher.SparkAppHandle;
import org.apache.spark.launcher.SparkLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

/**
 * 启动sparkjob的javaAPI方法。原理百度
 */
public class SparkClient {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(SparkClient.class);
    /**
     * 单例
     */
    private static final SparkClient sigle = new SparkClient();

    /**
     * 属性
     */
    private static ResourceBundle resourceBundle = ResourceBundle.getBundle("spark-client");

    /**
     * 私有化构造器
     */
    private SparkClient() {
    }

    /**
     * 获取实例
     */
    public static SparkClient getInstance() {
        return sigle;
    }

    /**
     * 启动SparkApp
     */
    public static String startSparkApp(String appName, String json, Function<SparkResult, Void> callback) throws IOException, InterruptedException {
        //日志输出
        logger.info("startSparkApp.Appname", appName);
        HashMap env = new HashMap();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        //这里调用setJavaHome()方法后，JAVA_HOME is not set 错误依然存在
        //SparkAppHandle handle = new SparkLauncher(env)
        SparkLauncher launcher = new SparkLauncher()
                .setSparkHome(resourceBundle.getString("sparkHome"))
                .setAppResource(resourceBundle.getString("appResource"))
                .setMainClass(resourceBundle.getString("mainClass"))
                .setMaster(resourceBundle.getString("master"))
                .setDeployMode(resourceBundle.getString("deployMode"))
                .setPropertiesFile(resourceBundle.getString("propertiesFile"))
                .setVerbose(Boolean.parseBoolean(resourceBundle.getString("verbose")))
                .addAppArgs(new String[]{json});


        SparkAppHandle handle = launcher.startApplication();
        handle.addListener(new SparkAppHandle.Listener() {
            //这里监听任务状态，当任务结束时（不管是什么原因结束）,isFinal（）方法会返回true,否则返回false
            @Override
            public void stateChanged(SparkAppHandle sparkAppHandle) {
                SparkResult sparkResult = new SparkResult(handle.getAppId());
                if (SparkAppHandle.State.FINISHED.equals(handle.getState())) {
                    sparkResult.setState(SparkConstants.App_SUCCESS);
                }

                if (sparkAppHandle.getState().isFinal()) {
                    countDownLatch.countDown();
                }
                System.out.println("state:" + sparkAppHandle.getState().toString());
            }

            @Override
            public void infoChanged(SparkAppHandle sparkAppHandle) {
                System.out.println("Info:" + sparkAppHandle.getState().toString());
            }

        });

        System.out.println("The task is executing, please wait ....");
        //线程等待任务结束
        countDownLatch.await();
        System.out.println("The task is finished!");
        return handle.getAppId();
    }
}
