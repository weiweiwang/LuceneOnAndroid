package com.dianxinos;

import android.util.Log;

/**
 * 对android的log的简单封装，通过设置LEVEL可以控制日志的输出
 * @see Logger#LEVEL
 * @author  wangweiwei
 */
public class Logger {
    /**
     * 日志级别，不小于这个级别的日志才会被输出
     */
    public static int LEVEL = Log.WARN;

    static public void v(String tag, String msg) {
        if (LEVEL <= Log.VERBOSE) {
            android.util.Log.v(tag, msg);
        }
    }

    static public void v(String tag, String msg, Throwable t) {
        if (LEVEL <= android.util.Log.VERBOSE) {
            android.util.Log.v(tag, msg, t);
        }
    }

    static public void d(String tag, String msg) {
        if (LEVEL <= android.util.Log.DEBUG) {
            android.util.Log.d(tag, msg);
        }
    }

    static public void d(String tag, String msg, Throwable t) {
        if (LEVEL <= android.util.Log.DEBUG) {
            android.util.Log.d(tag, msg, t);
        }
    }
    static public void i(String tag, String msg) {
        if (LEVEL <= android.util.Log.INFO) {
            android.util.Log.i(tag, msg);
        }
    }

    static public void i(String tag, String msg, Throwable t) {
        if (LEVEL <= android.util.Log.INFO) {
            android.util.Log.i(tag, msg, t);
        }
    }


    static public void w(String tag, String msg) {
        if (LEVEL <= android.util.Log.WARN) {
            android.util.Log.w(tag, msg);
        }
    }

    static public void w(String tag, String msg, Throwable t) {
        if (LEVEL <= android.util.Log.WARN) {
            android.util.Log.w(tag, msg, t);
        }
    }
}