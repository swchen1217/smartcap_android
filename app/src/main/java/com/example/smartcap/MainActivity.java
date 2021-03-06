package com.example.smartcap;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

//RequiresApi(api = Build.VERSION_CODES.ECLAIR)
public class MainActivity extends AppCompatActivity {

    //private Button openBT,findBT,connectBT,disconnectBT;
    int tmp_ngsecond=0;
    private long ng;
    private float ng_sec;
    private long starttime;
    private String starttime2;
    private int ngcount;
    private TextView starttimelbl;
    private TextView ngcountlbl;
    private TextView status;
    private TextView deviceName;
    private BluetoothDevice myDevice;
    private String deviceStatus = "";
    private BluetoothSocket mySocket;
    private BluetoothAdapter myBT = BluetoothAdapter.getDefaultAdapter();

    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

    Handler mHandler = new Handler() {
        @Override    public void handleMessage(Message msg) {
            switch(msg.what)
            {
                case 49:
                case -2:
                    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                    if(deviceStatus.equals("0")) {
                        ngcount++;
                        ngcountlbl.setText("警告次數："+ngcount);
                        ngsecond(1);
                    }
                    status.setText("狀態：你是不是低頭了呢！");
                    deviceStatus = "1";
                    break;
                case 48:
                case -1:
                    toneG.stopTone();
                    status.setText("狀態：你的姿勢很正確哦！");
                    if(deviceStatus.equals("1")){
                        ngsecond(0);
                    }
                    deviceStatus = "0";
            }
            //statusToServer();
            Date now = new Date();
            long p = (now.getTime() - starttime)/1000;
            long m = p/60;
            long s = p%60;
            long h = m/60;
            m = m%60;
            starttimelbl.setText("練習時間："+h+"時"+m+"分"+s+"秒\nngs:"+ng_sec/1000);
            super.handleMessage(msg);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = (TextView)this.findViewById(R.id.status);
        deviceName = (TextView)this.findViewById(R.id.deviceName);
        ngcountlbl = (TextView)this.findViewById(R.id.ngcounts);
        starttimelbl = (TextView)this.findViewById(R.id.practicetime);
    }

    @Override
    protected void onDestroy(){
        closeBT();
        super.onDestroy();
    }


    //@RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    public void connectBTClick(View v)
    {
        Set<BluetoothDevice> pairedDevices = myBT.getBondedDevices();
        Log.d("debug","BTConnect paired size:"+pairedDevices.size());
        if(pairedDevices.size() > 0)
        {
            String s = "找不到任何正確連結的iLaurel";
            for(BluetoothDevice device:pairedDevices)
            {
                boolean success = false;
                Log.d("debug","BTConnect paired name:"+device.getName());
                //if(device.getName().substring(0,8).equals("SmartCap"))
                if(device.getName().equals(com.example.smartcap.PrefsActivity.getDevice(MainActivity.this)))
                {
                    myDevice = device;
                    deviceName.setText("配對到的iLaurel："+device.getName());
                    try{
                        openBT();
                        success = true;
                        s = "設備配對成功";
                        ngcount = 0;
                        Date now = new Date();
                        starttime = now.getTime();
                        SimpleDateFormat sdf=new SimpleDateFormat();
                        sdf.applyPattern("yyyy-MM-dd*HH:mm:ss");
                        starttime2=sdf.format(now);
                        Log.d("debug","Time:"+starttime2);
                        Log.d("debug","BTConnect paired success:"+device.getName());
                    }catch(Exception e){
                        s = "藍牙錯誤，請重開iLaurel及設定手機的藍牙連接";
                        Log.d("debug","BTConnect paired fail:"+device.getName());
                    }
                }
                if(success)
                    break;
            }
            status.setText(s);
        }
    }

    public void disconnectBTClick(View v)
    {
        closeBT();
    }

    void closeBT()
    {
        try {
            mySocket.close();
            mySocket = null;
            myDevice = null;
            status.setText("");
            deviceName.setText("");
            starttimelbl.setText("");
            ngcountlbl.setText("");
            ng_sec=0;
            logToServer();
        }catch(Exception e)
        {
            status.setText("關閉失敗！");
        }
    }

    //@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    //@RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    void openBT() throws Exception
    {
        UUID uuid=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        Log.d("debug","BTConnect uuid ok");
        Log.d("debug","BTConnect myDevice:"+myDevice.getName());
        mySocket=myDevice.createRfcommSocketToServiceRecord(uuid);

        Log.d("debug","BTConnect create socket ok");
        Log.d("debug","BTConnect connect:"+mySocket.isConnected());
        mySocket.connect();
        Log.d("debug","BTConnect connect ok");
        //myOutputStream=mySocket.getOutputStream();
        //myInputStream=mySocket.getInputStream();
        new ConnectedThread(mySocket).start();
        Log.d("debug","BTConnect thread start ok");
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //@TargetApi(Build.VERSION_CODES.ECLAIR)
        //@RequiresApi(api = Build.VERSION_CODES.ECLAIR)
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                status.setText("藍芽通訊失敗");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            Log.d("debug","BTThread run:"+mySocket.toString());
            while (mySocket != null) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI Activity
                    Log.d("debug","BTThread:"+buffer[0]);
                    mHandler.obtainMessage(buffer[0])
                            .sendToTarget();
                    Thread.sleep(200);
                } catch (Exception e) {
                    Log.d("error","BTThread:"+e.toString());
                }
            }
            Log.d("debug","BTThread end");
        }

        /* Call this from the main Activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main Activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    public void queryLogClick(View v)
    {
        if(!PrefsActivity.getUser(MainActivity.this).equals(""))
            queryLog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, PrefsActivity.class));
                return true;
            case R.id.exit:
                closeBT();
                finish();
        }
        return false;
    }

    void logToServer(){
        new Thread()
        {
            public void run()
            {
                try
                {
                    Date now = new Date();
                    long use = (now.getTime() - starttime)/1000;
                    Log.d("debug","url(m):http://"+PrefsActivity.getServer(MainActivity.this)+"/smartcap/index.php?user="+PrefsActivity.getUser(MainActivity.this)+"&starttime="+starttime2+"&usetime="+use+"&ngtimes="+ngcount);
                    URL url = new URL("http://"+PrefsActivity.getServer(MainActivity.this)+"/smartcap/index.php?user="+PrefsActivity.getUser(MainActivity.this)+"&starttime="+starttime2+"&usetime="+use+"&ngtimes="+ngcount);
                    url.openStream();
                }
                catch(Exception e)
                {
                }
            }
        }.start();

    }

    void queryLog(){
        new Thread()
        {
            public void run()
            {
                try
                {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("http://"+PrefsActivity.getServer(MainActivity.this)+"/smartcap/index.php?user="+PrefsActivity.getUser(MainActivity.this)));
                    startActivity(intent);
                }
                catch(Exception e)
                {
                }
            }
        }.start();

    }

    public void ngsecond(int input){
        Log.d("debug","ngs_input:"+input);
        //Log.d("debug","ngs_tmp:"+tmp_ngsecond);
        if(input==1){
            Date now = new Date();
            ng = now.getTime();
        }else{
            new Thread()
            {
                public void run()
                {
                    try
                    {
                        Date now = new Date();
                        ng_sec = now.getTime()-ng;
                        SimpleDateFormat sdf2=new SimpleDateFormat();
                        sdf2.applyPattern("yyyy-MM-dd*HH:mm:ss");
                        Log.d("debug","ngs:"+ng_sec);
                        Log.d("debug","ngt:"+sdf2.format(now));
                        Log.d("debug","url(s):http://"+PrefsActivity.getServer(MainActivity.this)+"/smartcap/index.php?user="+PrefsActivity.getUser(MainActivity.this)+"&starttime="+starttime2+"&ngtime="+sdf2.format(now)+"&ngsecond="+ng_sec);
                        URL url = new URL("http://"+PrefsActivity.getServer(MainActivity.this)+"/smartcap/index.php?user="+PrefsActivity.getUser(MainActivity.this)+"&starttime="+starttime2+"&ngtime="+sdf2.format(now)+"&ngsecond="+ng_sec);
                        url.openStream();
                    }
                    catch(Exception e)
                    {
                    }
                }
            }.start();
        }
    }
}
