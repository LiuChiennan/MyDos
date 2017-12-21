package com.miko.zd.wifidirect.NetworkFlowUnity;

import android.content.Context;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;

import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by 刘建南 on 2017/12/13.
 */

public class NetworkSpeedUnity {
    private Context mContex;
    private Handler mHandler;

    private double lastTotalRxBytes = 0;
    private double lastTimeStemp = 0;
    private NumberFormat num;

    TimerTask mTask = new TimerTask() {
        @Override
        public void run() {
            showSpeed();
        }
    };
    public NetworkSpeedUnity(Context context,Handler handler){
        this.mContex = context;
        this.mHandler = handler;
        num = NumberFormat.getInstance();
        num.setMinimumIntegerDigits(4);
        num.setMaximumFractionDigits(2);
    }

    public void startShowNetwork(){
        lastTotalRxBytes = TrafficStats.getTotalRxBytes();
        //lastTimeStemp = System.currentTimeMillis();

        new Timer().schedule(mTask,0,1000);
    }

    private void showSpeed(){
        double nowTotalRxBytes = getTotalRxBytes();
        double nowTimeStemp = System.currentTimeMillis();
        double speed = nowTotalRxBytes - lastTotalRxBytes;
        //long speed2 = ((nowTotalRxBytes - lastTotalRxBytes) * 1000) % (nowTimeStemp-lastTimeStemp); // ;

        lastTotalRxBytes = nowTotalRxBytes;
        //lastTimeStemp = nowTimeStemp;

        Message mess = new Message();
        mess.what = 20;
        mess.obj = String.valueOf(num.format(speed)) + "kb/s";
        mHandler.sendMessage(mess);
    }

    private double getTotalRxBytes(){
        return (double)TrafficStats.getTotalRxBytes()/1024;
    }




}
