/*
 * Copyright (c) 2010 - 2017, Nordic Semiconductor ASA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form, except as embedded into a Nordic
 *    Semiconductor ASA integrated circuit in a product or a software update for
 *    such product, must reproduce the above copyright notice, this list of
 *    conditions and the following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. Neither the name of Nordic Semiconductor ASA nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * 4. This software, with or without modification, must only be used with a
 *    Nordic Semiconductor ASA integrated circuit.
 *
 * 5. Any software provided in binary form under this license must not be reverse
 *    engineered, decompiled, modified and/or disassembled.
 *
 * THIS SOFTWARE IS PROVIDED BY NORDIC SEMICONDUCTOR ASA "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY, NONINFRINGEMENT, AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NORDIC SEMICONDUCTOR ASA OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package no.nordicsemi.android.nrfthingy;

import android.util.Log;
import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.tabs.TabLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.Toolbar;

import android.os.ParcelUuid;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;

import no.nordicsemi.android.nrfthingy.ClusterHead.ClhAdvertise;
import no.nordicsemi.android.nrfthingy.ClusterHead.ClhAdvertisedData;
import no.nordicsemi.android.nrfthingy.ClusterHead.ClhConst;
import no.nordicsemi.android.nrfthingy.ClusterHead.ClhProcessData;
import no.nordicsemi.android.nrfthingy.ClusterHead.ClhScan;
import no.nordicsemi.android.nrfthingy.ClusterHead.ClusterHead;
import no.nordicsemi.android.nrfthingy.common.DeviceListAdapter;
import no.nordicsemi.android.nrfthingy.common.MessageDialogFragment;
import no.nordicsemi.android.nrfthingy.common.PermissionRationaleDialogFragment;
import no.nordicsemi.android.nrfthingy.common.Utils;
import no.nordicsemi.android.nrfthingy.sound.FrequencyModeFragment;
import no.nordicsemi.android.nrfthingy.sound.PcmModeFragment;
import no.nordicsemi.android.nrfthingy.sound.SampleModeFragment;
import no.nordicsemi.android.nrfthingy.sound.ThingyMicrophoneService;
import no.nordicsemi.android.nrfthingy.thingy.ThingyService;
import no.nordicsemi.android.nrfthingy.widgets.VoiceVisualizer;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;
import no.nordicsemi.android.thingylib.ThingyListener;
import no.nordicsemi.android.thingylib.ThingyListenerHelper;
import no.nordicsemi.android.thingylib.ThingySdkManager;
import no.nordicsemi.android.thingylib.utils.ThingyUtils;
import no.nordicsemi.android.nrfthingy.HelperClass;

public class SoundFragment extends Fragment implements PermissionRationaleDialogFragment.PermissionDialogListener {


    private static final String AUDIO_PLAYING_STATE = "AUDIO_PLAYING_STATE";
    private static final String AUDIO_RECORDING_STATE = "AUDIO_RECORDING_STATE";
    private static final float ALPHA_MAX = 0.60f;
    private static final float ALPHA_MIN = 0.0f;
    private static final int DURATION = 800;

    private ImageView mMicrophone;
    private ImageView mMicrophoneOverlay;
    private ImageView mThingyOverlay;
    private ImageView mThingy;
    private VoiceVisualizer mVoiceVisualizer;

    private BluetoothDevice mDevice;
    private FragmentAdapter mFragmentAdapter;
    private ThingySdkManager mThingySdkManager;
    private boolean mStartRecordingAudio = false;
    private boolean mStartPlayingAudio = false;

    Clap clap = new Clap();

    private ThingyListener mThingyListener = new ThingyListener() {
        private Handler mHandler = new Handler();

        @Override
        public void onDeviceConnected(BluetoothDevice device, int connectionState) {
        }

        @Override
        public void onDeviceDisconnected(BluetoothDevice device, int connectionState) {
            if (device.equals(mDevice)) {
                stopRecording();
                stopMicrophoneOverlayAnimation();
                stopThingyOverlayAnimation();
                mStartPlayingAudio = false;
            }
        }

        @Override
        public void onServiceDiscoveryCompleted(BluetoothDevice device) {
            changeLed(null);
        }

        @Override
        public void onBatteryLevelChanged(final BluetoothDevice bluetoothDevice, final int batteryLevel) {

        }

        @Override
        public void onTemperatureValueChangedEvent(BluetoothDevice bluetoothDevice, String temperature) {
        }

        @Override
        public void onPressureValueChangedEvent(BluetoothDevice bluetoothDevice, final String pressure) {
        }

        @Override
        public void onHumidityValueChangedEvent(BluetoothDevice bluetoothDevice, final String humidity) {
        }

        @Override
        public void onAirQualityValueChangedEvent(BluetoothDevice bluetoothDevice, final int eco2, final int tvoc) {
        }

        @Override
        public void onColorIntensityValueChangedEvent(BluetoothDevice bluetoothDevice, final float red, final float green, final float blue, final float alpha) {
        }

        @Override
        public void onButtonStateChangedEvent(BluetoothDevice bluetoothDevice, int buttonState) {

        }

        @Override
        public void onTapValueChangedEvent(BluetoothDevice bluetoothDevice, int direction, int count) {

        }

        @Override
        public void onOrientationValueChangedEvent(BluetoothDevice bluetoothDevice, int orientation) {

        }

        @Override
        public void onQuaternionValueChangedEvent(BluetoothDevice bluetoothDevice, float w, float x, float y, float z) {

        }

        @Override
        public void onPedometerValueChangedEvent(BluetoothDevice bluetoothDevice, int steps, long duration) {

        }

        @Override
        public void onAccelerometerValueChangedEvent(BluetoothDevice bluetoothDevice, float x, float y, float z) {

        }

        @Override
        public void onGyroscopeValueChangedEvent(BluetoothDevice bluetoothDevice, float x, float y, float z) {

        }

        @Override
        public void onCompassValueChangedEvent(BluetoothDevice bluetoothDevice, float x, float y, float z) {

        }

        @Override
        public void onEulerAngleChangedEvent(BluetoothDevice bluetoothDevice, float roll, float pitch, float yaw) {

        }

        @Override
        public void onRotationMatrixValueChangedEvent(BluetoothDevice bluetoothDevice, byte[] matrix) {

        }

        @Override
        public void onHeadingValueChangedEvent(BluetoothDevice bluetoothDevice, float heading) {

        }

        @Override
        public void onGravityVectorChangedEvent(BluetoothDevice bluetoothDevice, float x, float y, float z) {

        }

        @Override
        public void onSpeakerStatusValueChangedEvent(BluetoothDevice bluetoothDevice, int status) {

        }

        @Override
        public void onMicrophoneValueChangedEvent(BluetoothDevice bluetoothDevice, final byte[] data) {
            if (data != null) {
                if (data.length != 0) {



                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {

                            mVoiceVisualizer.draw(data);


                        }
                    });
                    double maximaal = clap.insert_data(data);
                    if (maximaal > 0) {
                        changeLed(null);
                        Log.d("Banter", "max = " + Double.toString(maximaal));
                    }



                }
            }
        }

        private void setupLedColor(int ledBlue) {
        }
    };

    private BroadcastReceiver mAudioRecordBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.startsWith(Utils.EXTRA_DATA_AUDIO_RECORD)) {
                final byte[] tempPcmData = intent.getExtras().getByteArray(ThingyUtils.EXTRA_DATA_PCM);
                final int length = intent.getExtras().getInt(ThingyUtils.EXTRA_DATA);
                if (tempPcmData != null) {
                    if (length != 0) {
                        mVoiceVisualizer.draw(tempPcmData);
                    }
                }
            } else if (action.equals(Utils.ERROR_AUDIO_RECORD)) {
                final String error = intent.getExtras().getString(Utils.EXTRA_DATA);
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
            }
        }
    };

    public static SoundFragment newInstance(final BluetoothDevice device) {
        SoundFragment fragment = new SoundFragment();
        final Bundle args = new Bundle();
        args.putParcelable(Utils.CURRENT_DEVICE, device);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDevice = getArguments().getParcelable(Utils.CURRENT_DEVICE);
        }
        mThingySdkManager = ThingySdkManager.getInstance();
    }


    //PSG edit No.2---------
    //var declare and init

    private Button mAdvertiseButton;
    private EditText mClhIDInput;
    private TextView mClhLog;
    private final String LOG_TAG="CLH Sound";

    private ClhAdvertisedData mClhData=new ClhAdvertisedData();
    private boolean mIsSink=false;
    private byte mClhID=2;
    private byte mClhDestID=0;
    private byte mClhHops=0;
    private byte mClhThingyID=1;
    private byte mClhThingyType=1;
    private int mClhThingySoundPower=100;
    ClusterHead mClh;
    ClhAdvertise mClhAdvertiser;
    ClhScan mClhScanner;
    ClhProcessData mClhProcessor;
    private int recvCount = 0;
    private byte currentAckNumber = 0;
    private ArrayList<Byte> ackedNumbers = new ArrayList<Byte>();
    private ArrayList<Byte> receivedPackets = new ArrayList<Byte>();
    private HelperClass helperClass = new HelperClass();
    private int cycles = 0;
    private int firstRSSICycle = 0;



//End PSG edit No.2----------------------------

    public ClhAdvertisedData createNewPacket(byte dest, byte soundPow, byte thingytype, byte thingyid, byte packetType, byte data0, byte data1)
    {
        ClhAdvertisedData ret = new ClhAdvertisedData();

        ret.setSourceID(mClhID);
        ret.setPacketID((byte) 1);
        ret.setHopCount((byte) 0);
        ret.setDestId(dest);
        ret.setSoundPower(soundPow);
        ret.setThingyDataType(thingytype);
        ret.setThingyId(thingyid);
        ret.setAckNumber(currentAckNumber);
        ret.setIsAckPacket(false);
        ret.setPacketType(packetType);
        ret.setData0(data0);
        ret.setData1(data1);


        return ret;
    }

    public void startTransmit(ClhAdvertisedData ret)
    {
        final ClhAdvertisedData copyOfPacket = ret;

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ackedNumbers.contains(copyOfPacket.getAckNumber())) return;
                handler.postDelayed(this, 5000);

                if (mClhAdvertiser.getAdvertiseList().size() > 10) return; // To prevent filling up the buffer too quickly with only retransmissions
                Log.d("YEET", "RETRANSMIT " + copyOfPacket.getAckNumber());
                mClhAdvertiser.addAdvPacketToBuffer(copyOfPacket, true);
            }
        }, 5000);

        currentAckNumber++;

    }

    public ClhAdvertisedData createAckPacket(ClhAdvertisedData packetToAck)
    {
        ClhAdvertisedData ret = new ClhAdvertisedData();
        ret.setSourceID(packetToAck.getDestinationID());
        ret.setDestId(packetToAck.getSourceID());
        ret.setAckNumber(packetToAck.getAckNumber());
        ret.setIsAckPacket(true);
        return ret;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_sound, container, false);

        final Toolbar speakerToolbar = rootView.findViewById(R.id.speaker_toolbar);
        speakerToolbar.inflateMenu(R.menu.audio_warning);
        speakerToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final int id = item.getItemId();
                switch (id) {
                    case R.id.action_audio_warning:
                        MessageDialogFragment fragment = MessageDialogFragment.newInstance(getString(R.string.info), getString(R.string.mtu_warning));
                        fragment.show(getChildFragmentManager(), null);
                        break;
                }
                return false;
            }
        });

        final Toolbar microphoneToolbar = rootView.findViewById(R.id.microphone_toolbar);
        microphoneToolbar.inflateMenu(R.menu.audio_warning);
        microphoneToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final int id = item.getItemId();
                switch (id) {
                    case R.id.action_audio_warning:
                        MessageDialogFragment fragment = MessageDialogFragment.newInstance(getString(R.string.info), getString(R.string.mtu_warning));
                        fragment.show(getChildFragmentManager(), null);
                        break;
                }
                return false;
            }
        });

        mMicrophone = rootView.findViewById(R.id.microphone);
        mMicrophoneOverlay = rootView.findViewById(R.id.microphoneOverlay);
        mThingy = rootView.findViewById(R.id.thingy);
        mThingyOverlay = rootView.findViewById(R.id.thingyOverlay);
        mVoiceVisualizer = rootView.findViewById(R.id.voice_visualizer);

        // Prepare the sliding tab layout and the view pager
        final TabLayout mTabLayout = rootView.findViewById(R.id.sliding_tabs);
        final ViewPager pager = rootView.findViewById(R.id.view_pager);
        mFragmentAdapter = new FragmentAdapter(getChildFragmentManager());
        pager.setAdapter(mFragmentAdapter);
        mTabLayout.setupWithViewPager(pager);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(final int position) {
                switch (position) {
                    case 1:
                        mFragmentAdapter.setSelectedFragment(position);
                        break;
                    default:
                        mFragmentAdapter.setSelectedFragment(position);
                        break;
                }
            }

            @Override
            public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(final int state) {
            }
        });

        mMicrophone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mThingySdkManager.isConnected(mDevice)) {
                    if (!mStartRecordingAudio) {
                        checkMicrophonePermissions();
                    } else {
                        stopRecording();
                    }
                }
            }
        });


         mThingy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mThingySdkManager.isConnected(mDevice)) {
                    if (!mStartPlayingAudio) {
                        mStartPlayingAudio = true;
                        startThingyOverlayAnimation();

                        mThingySdkManager.enableThingyMicrophone(mDevice, true);
                    } else {
                        mThingySdkManager.enableThingyMicrophone(mDevice, false);
                        stopThingyOverlayAnimation();
                        mStartPlayingAudio = false;
                    }
                }
            }
        });

        if (savedInstanceState != null) {
            mStartPlayingAudio = savedInstanceState.getBoolean(AUDIO_PLAYING_STATE);
            mStartRecordingAudio = savedInstanceState.getBoolean(AUDIO_RECORDING_STATE);

            if (mStartPlayingAudio) {
                startThingyOverlayAnimation();
            }

            if (mStartRecordingAudio) {
                if (mThingySdkManager.isConnected(mDevice)) {
                    startMicrophoneOverlayAnimation();
                    sendAudiRecordingBroadcast();
                }
            }
        }

        loadFeatureDiscoverySequence();


        //PSG edit No.3----------------------------
        mAdvertiseButton = rootView.findViewById(R.id.startClh_btn);
        mClhIDInput= rootView.findViewById(R.id.clhIDInput_text);
        mClhLog= rootView.findViewById(R.id.logClh_text);

        //initial Clusterhead: advertiser, scanner, processor
        mClh=new ClusterHead(mClhID);
        mClh.initClhBLE(ClhConst.ADVERTISING_INTERVAL);
        mClhAdvertiser=mClh.getClhAdvertiser();
        mClhScanner=mClh.getClhScanner();
        mClhProcessor=mClh.getClhProcessor();
        final int maxHopCount = 10;

        //timer 1000 ms for SINK to process receive data(display data to text box)

        final Handler handler=new Handler();
        handler. postDelayed(new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 1000); //loop every cycle

                ArrayList<ClhAdvertisedData> procList = mClhProcessor.getProcessDataList();
                for(int i=0; i<procList.size();i++)
                {
                    if (!procList.get(0).isAckPacket())
                    {
                        ClhAdvertisedData data = createAckPacket(procList.get(0));
                        mClhAdvertiser.addAdvPacketToBuffer(data, true);
                        mClhAdvertiser.nextAdvertisingPacket(); //start advertising

                        if (!receivedPackets.contains(procList.get(0).getAckNumber())) {
                            // CODE COMES HERE YOU IDIOTSSSS
                            if (procList.get(0).getPacketType() == 0 && mClhID == 0) // Is thingy distpacket and we are sink
                            {
                                final int[] RSSIIDlocation = new int[]{11, 4, 6, 13, 15, 17, 19};
                                final int[] RSSIPowerlocation = new int[]{12, 5, 7, 14, 16, 18, 20};
                                for (int ii = 0; ii < 7; ++ii)
                                {
                                    int RSSI = procList.get(0).getData(RSSIPowerlocation[ii]);
                                    int ID = procList.get(0).getData(RSSIIDlocation[ii]);
                                    if (RSSI != 0 && ID != 0)
                                    {
                                        helperClass.insertRSSI(procList.get(0).getSourceID()+"", ID+"", RSSI);
                                    }
                                    else
                                    {
                                        break;
                                    }
                                }
                                //helperClass.insertRSSI(procList.get(0).getSourceID()+"", procList.get(0).getData0() + "", (int) procList.get(0).getData1());
                                Log.d("YEET", "helperClass.insertRSSI("+procList.get(0).getSourceID()+","+procList.get(0).getData0()+","+procList.get(0).getData1()+");");
                                if (firstRSSICycle == 0)
                                {
                                    firstRSSICycle = cycles;
                                }
                            }
                            if (procList.get(0).getPacketType() == 1 && mClhID != 0) // Is a connect list
                            {
                                final int[] RSSIIDlocation = new int[]{11, 4, 6, 13, 15, 17, 19};
                                final int[] RSSIPowerlocation = new int[]{12, 5, 7, 14, 16, 18, 20};
                                for (int ii = 0; ii < 7; ++ii)
                                {
                                    int RSSI = procList.get(0).getData(RSSIPowerlocation[ii]);
                                    int ID = procList.get(0).getData(RSSIIDlocation[ii]);
                                    if (RSSI == -1 && ID != 0)
                                    {
                                        // TODO: PETER CONNECT TO THINGY WITH ID ID.
                                        BluetoothDevice dev = null;
                                        for (int x = 0 ; x < knownThingys.size(); i++) {
                                            if (knownThingys.get(x).ID == ID) {
                                                dev = knownThingys.get(x).bluetoothDevice;
                                            }
                                        }
                                        mThingySdkManager.connectToThingy(getActivity().getApplicationContext(), dev, ThingyService.class);
                                        mThingySdkManager.setSelectedDevice(dev);
                                        // mThingySdkManager.setOneShotLedMode(dev, ThingyUtils.LED_RED, 255);
                                        Log.d("YEET", "HAS TO CONNECT TO: " + ID);
                                    }
                                    else
                                    {
                                        break;
                                    }
                                }
                            }

                            receivedPackets.add(procList.get(0).getAckNumber());
                            if (receivedPackets.size() > 100)
                            {
                                receivedPackets.remove(0);
                            }
                            recvCount++;
                            Log.d("YEET", "" + recvCount);
                            Log.d("YEET", "GOT NORMAL PACK " + procList.get(0).getAckNumber());
                        }
                    }
                    else
                    {

                        ackedNumbers.add(procList.get(0).getAckNumber());
                        if (ackedNumbers.size() > 100)
                        {
                            ackedNumbers.remove(0);
                        }
                        Log.d("YEET", "GOT ACK PACK" + Arrays.toString(procList.get(0).getParcelClhData()));
                        Log.d("YEET", "GOT ACK PACK" + procList.get(0).getAckNumber());
                    }


                    procList.remove(0);
                }
                if (firstRSSICycle != 0 && cycles - firstRSSICycle == 120)
                {
                    HashMap<String, HashSet> thingyDiv = helperClass.getThingyDivision();
                    Log.d("YEET", "DIVIDING!");
                    for (String key: thingyDiv.keySet())
                    {
                        boolean isFirst = true;
                        ClhAdvertisedData newPack = createNewPacket(Byte.parseByte(key), (byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 0, (byte) 0); // Initial invalid packet
                        for (String thingyId: (HashSet<String>)thingyDiv.get(key))
                        {
                            //byte dest, byte soundPow, byte thingytype, byte thingyid, byte packetType, byte data0, byte data1
                            if (isFirst) {
                                newPack = createNewPacket(Byte.parseByte(key), (byte) 0, (byte) 0, (byte) 0, (byte) 1, Byte.parseByte(thingyId), (byte) -1); // really initialize the packet
                                isFirst = false;
                                continue;
                            }
                            newPack.setNewRSSI(Byte.parseByte(thingyId), (byte) -1);
                        }
                        startTransmit(newPack);
                        mClhAdvertiser.addAdvPacketToBuffer(newPack, true);
                    }
                }
                cycles++;
            }
        }, 1000); //the time you want to delay in milliseconds

        //"Start" button Click Handler
        // get Cluster Head ID (0-127) in text box to initialize advertiser
        //Then Start advertising
        //ID=0: Sink
        //ID=1..126: normal Cluster head, get sound data from Thingy and advertise
        //ID=127: test cluster Head, send dummy data for testing purpose
        mAdvertiseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Resources res = getResources();

                Log.i(LOG_TAG, mAdvertiseButton.getText().toString());
                if (mAdvertiseButton.getText().toString().equals("Start")) {
                    mAdvertiseButton.setText("Stop");
                    mClhIDInput.setEnabled(false);

                    mClh.clearClhAdvList(); //empty list before starting

                    //startScan();
                    //check input text must in rang 0..127
                    String strEnteredVal = mClhIDInput.getText().toString();
                    if ((strEnteredVal.compareTo("") == 0) || (strEnteredVal == null)) {
                        mClhIDInput.setText(String.format( "%d", mClhID));
                        Log.i(LOG_TAG, "error: ClhID must be in 0-127");
                        Log.i(LOG_TAG, "set ClhID default:"+mClhID);

                    } else {
                        int num = Integer.valueOf(strEnteredVal);
                        if (num>127) num=mClhID;
                        mClhID = (byte) num;
                        mIsSink = mClh.setClhID(mClhID);
                        Log.i(LOG_TAG, "set ClhID:"+mClhID);
                    }

                    //ID=127, set dummy data include 100 elements for testing purpose
                    if(mClhID==126) {
                        //mClhID = 1;
                        byte clhPacketID=1;
                        mClhThingySoundPower = 100;
                        /*
                                String[] phones = {"iPhone1", "iPhone2", "iPhone3"};
        String[] thingyNumbers = {"25", "25", "25",         "3", "3", "3",          "53", "53", "53",           "8", "8", "8",              "7", "7", "7",              "69", "69", "69"};
        Integer[] RSSItest = {-65, -68, -24,             -56, -57, -32,             -28, -35, -47,               -43, -64, -77,             -44, -62, -82,              -85, -95, -49};

                         */

                        onCreateScanner();
                        startScan();
                      }

                    mClhAdvertiser.nextAdvertisingPacket(); //start advertising
                }
                else
                {//stop advertising
                    mAdvertiseButton.setText("Start");
                    mClhIDInput.setEnabled(true);
                    mClhAdvertiser.stopAdvertiseClhData();
                }
            }
        });
        mClhIDInput.setText(Integer.toString((int)mClhID));
        //End PSG edit No.3----------------------------



        return rootView;
    }

    //-------------------------------------------------------BEGIN YORAN ZIJN TROEP-------------------------------------------------------
    private final static String TAGY = "Scanner";

    private final static long SCAN_DURATION = 8000;
    /* package */static final int NO_RSSI = -1000;

    private final static int REQUEST_PERMISSION_REQ_CODE = 76; // any 8-bit number

    private LinearLayout troubleshootView;
    private DeviceListAdapter mAdapter;
    private Handler mHandler = new Handler();

    private ParcelUuid mUuid;
    private boolean mIsScanning = false;


    // LIST WITH ALL KNOWN THINGY'S
    List<ThingyBLDev> knownThingys = new ArrayList<ThingyBLDev>();
    private boolean thingyListAvailable = false;
    ThingyBLDev thingyBuf = new ThingyBLDev();


    private void onCreateScanner() {
        mAdapter = new DeviceListAdapter();
    }

    private void startScan() {
        Log.d(TAGY, "Starting Scan");

        // Set mUuid to the one from the Thingy's
        mUuid = ParcelUuid.fromString(ThingyUtils.THINGY_BASE_UUID.toString());

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                return;
            }

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_REQ_CODE);
            return;
        }
        if(mAdapter.isEmpty()) {
            mAdapter.clearDevices();
        }

        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(750).setUseHardwareBatchingIfSupported(false).setUseHardwareFilteringIfSupported(false).build();
        final List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(mUuid).build());
        scanner.startScan(filters, settings, scanCallback);

        mIsScanning = true;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mIsScanning) {
                    stopScan();
                }
            }
        }, SCAN_DURATION);
    }

    private void changeLed(View view) {
        BluetoothDevice dev = knownThingys.get(0).bluetoothDevice;
        mThingySdkManager.setConstantLedMode(dev, 255, 0, 0);
    }


    private void stopScan() {
        Log.d(TAGY, "Stopping Scan");
        thingyListAvailable = true;
        if (mIsScanning) {
            final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
            scanner.stopScan(scanCallback);
            mIsScanning = false;
        }

        boolean isFirst = true;
        ClhAdvertisedData newPack = createNewPacket(mClhDestID, (byte)mClhThingySoundPower, mClhThingyType, mClhThingyID, (byte)0, (byte)0, (byte)0);
        for(int i = 0; i < knownThingys.size(); i++)
        {
            Log.d("scanner", "Found a thingy, ID = " + knownThingys.get(i).ID +
                    "MAC = " + knownThingys.get(i).bluetoothDevice.getAddress() + " RSSI = " + knownThingys.get(i).rssi);
            BluetoothDevice dev = knownThingys.get(i).bluetoothDevice;
            mThingySdkManager.connectToThingy(getActivity().getApplicationContext(), dev, ThingyService.class);
            mThingySdkManager.setSelectedDevice(dev);
            mThingySdkManager.setOneShotLedMode(dev, ThingyUtils.LED_RED, 255);

            //byte dest, byte soundPow, byte thingytype, byte thingyid, byte packetType, byte data0, byte data1
            if (isFirst) {
                newPack = createNewPacket(mClhDestID, (byte)mClhThingySoundPower, mClhThingyType, mClhThingyID, (byte)0,
                        (byte)knownThingys.get(i).ID, (byte)knownThingys.get(i).rssi);
                isFirst = false;
                continue;
            }
            newPack.setNewRSSI((byte)knownThingys.get(i).ID, (byte) knownThingys.get(i).rssi);
        }
        if(!knownThingys.isEmpty()) {
            startTransmit(newPack);
            mClhAdvertiser.addAdvPacketToBuffer(newPack, true);
        }
    }


    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, @NonNull final ScanResult result) {
            // do nothing
        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            mAdapter.update(results);
            for (int i = 0; i < results.size(); i++) {
                thingyBuf.bluetoothDevice = results.get(i).getDevice();
                thingyBuf.rssi = results.get(i).getRssi();
                thingyBuf.ID = Integer.parseInt(thingyBuf.bluetoothDevice.getAddress().substring(15), 16);

                if(!knownThingys.contains(thingyBuf)) {
                    knownThingys.add(thingyBuf);
                }

            }
        }

        @Override
        public void onScanFailed(final int errorCode) {
            // should never be called
        }
    };
    //-------------------------------------------------------EIND YORAN ZIJN TROEP-------------------------------------------------------
    //------------------------------------YORAN ZIJN TROEP IS MEDEMOGELIJK GEMAAKT DOOR PERVASIVE SYSTEEM--------------------------------


    private void sendAudiRecordingBroadcast() {
        Intent startAudioRecording = new Intent(getActivity(), ThingyMicrophoneService.class);
        startAudioRecording.setAction(Utils.START_RECORDING);
        startAudioRecording.putExtra(Utils.EXTRA_DEVICE, mDevice);
        getActivity().startService(startAudioRecording);
    }

    private void stop() {
        final Intent s = new Intent(Utils.STOP_RECORDING);
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(s);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUDIO_PLAYING_STATE, mStartPlayingAudio);
        outState.putBoolean(AUDIO_RECORDING_STATE, mStartRecordingAudio);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mDevice != null) {
            ThingyListenerHelper.registerThingyListener(getContext(), mThingyListener, mDevice);
            LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mAudioRecordBroadcastReceiver, createAudioRecordIntentFilter(mDevice.getAddress()));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ThingyListenerHelper.unregisterThingyListener(getContext(), mThingyListener);
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mAudioRecordBroadcastReceiver);
        mVoiceVisualizer.stopDrawing();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopRecording();
        stopThingyOverlayAnimation();
    }

    @Override
    public void onRequestPermission(final String permission, final int requestCode) {
        // Since the nested child fragment (activity > fragment > fragment) wasn't getting called
        // the exact fragment index has to be used to get the fragment.
        // Also super.onRequestPermissionResult had to be used in both the main activity, fragment
        // in order to propagate the request permission callback to the nested fragment
        requestPermissions(new String[]{permission}, requestCode);
    }

    @Override
    public void onCancellingPermissionRationale() {
        Utils.showToast(getActivity(), getString(R.string.requested_permission_not_granted_rationale));
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Utils.REQ_PERMISSION_RECORD_AUDIO:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Utils.showToast(getActivity(), getString(R.string.rationale_permission_denied));
                } else {
                    startRecording();
                }
        }
    }

    private void checkMicrophonePermissions() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else {
            final PermissionRationaleDialogFragment dialog = PermissionRationaleDialogFragment.getInstance(Manifest.permission.RECORD_AUDIO,
                    Utils.REQ_PERMISSION_RECORD_AUDIO, getString(R.string.microphone_permission_text));
            dialog.show(getChildFragmentManager(), null);
        }
    }

    private void startRecording() {
        startMicrophoneOverlayAnimation();
        sendAudiRecordingBroadcast();
        mStartRecordingAudio = true;
    }

    private void stopRecording() {
        stopMicrophoneOverlayAnimation();
        stop();
        mStartRecordingAudio = false;
    }

    private void startMicrophoneOverlayAnimation() {
        mThingy.setEnabled(false);
        mMicrophone.setImageResource(R.drawable.ic_mic_white_off);
        mMicrophone.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_device_bg_red));
        mMicrophoneOverlay.animate().alpha(ALPHA_MAX).setDuration(DURATION).withEndAction(new Runnable() {
            @Override
            public void run() {
                if (mMicrophoneOverlay.getAlpha() == ALPHA_MAX) {
                    mMicrophoneOverlay.animate().alpha(ALPHA_MIN).setDuration(DURATION).withEndAction(this).start();
                } else {
                    mMicrophoneOverlay.animate().alpha(ALPHA_MAX).setDuration(DURATION).withEndAction(this).start();
                }
            }
        }).start();
    }

    private void stopMicrophoneOverlayAnimation() {
        mThingy.setEnabled(true);
        mStartRecordingAudio = false;
        mMicrophoneOverlay.animate().cancel();
        mMicrophoneOverlay.setAlpha(ALPHA_MIN);
        mMicrophone.setImageResource(R.drawable.ic_mic_white);
        mMicrophone.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_device_bg_blue));
    }

    private void startThingyOverlayAnimation() {
        mMicrophone.setEnabled(false);
        mThingy.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_device_bg_red));
        mThingyOverlay.animate().alpha(ALPHA_MAX).setDuration(DURATION).withEndAction(new Runnable() {
            @Override
            public void run() {
                if (mThingyOverlay.getAlpha() == ALPHA_MAX) {
                    mThingyOverlay.animate().alpha(ALPHA_MIN).setDuration(DURATION).withEndAction(this).start();
                } else {
                    mThingyOverlay.animate().alpha(ALPHA_MAX).setDuration(DURATION).withEndAction(this).start();
                }
            }
        }).start();
    }

    private void stopThingyOverlayAnimation() {
        mMicrophone.setEnabled(true);
        mThingyOverlay.animate().cancel();
        mThingyOverlay.setAlpha(ALPHA_MIN);
        mThingy.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_device_bg_blue));
        mStartPlayingAudio = false;
    }

    private class FragmentAdapter extends FragmentPagerAdapter {
        private int mSelectedFragmentTab = 0;

        FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return FrequencyModeFragment.newInstance(mDevice);
                case 1:
                    return PcmModeFragment.newInstance(mDevice);
                default:
                case 2:
                    return SampleModeFragment.newInstance(mDevice);
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getResources().getStringArray(R.array.sound_tab_title)[position];
        }

        void setSelectedFragment(final int selectedTab) {
            mSelectedFragmentTab = selectedTab;
        }

        public int getSelectedFragment() {
            return mSelectedFragmentTab;
        }
    }

    private static IntentFilter createAudioRecordIntentFilter(final String address) {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(Utils.EXTRA_DATA_AUDIO_RECORD + address);
        intentFilter.addAction(Utils.ERROR_AUDIO_RECORD);
        return intentFilter;
    }

    private void displayStreamingInformationDialog() {
        final SharedPreferences sp = requireActivity().getSharedPreferences(Utils.PREFS_INITIAL_SETUP, Context.MODE_PRIVATE);
        final boolean showStreamingDialog = sp.getBoolean(Utils.INITIAL_AUDIO_STREAMING_INFO, true);
        if (showStreamingDialog) {
            MessageDialogFragment fragment = MessageDialogFragment.newInstance(getString(R.string.info), getString(R.string.mtu_warning));
            fragment.show(getChildFragmentManager(), null);

            final SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(Utils.INITIAL_AUDIO_STREAMING_INFO, false);
            editor.apply();
        }
    }

    private void loadFeatureDiscoverySequence() {
        if (!Utils.checkIfSequenceIsCompleted(requireContext(), Utils.INITIAL_SOUND_TUTORIAL)) {

            final SpannableString microphone = new SpannableString(getString(R.string.start_talking_to_thingy));
            final SpannableString thingy = new SpannableString(getString(R.string.start_talking_from_thingy));

            final TapTargetSequence sequence = new TapTargetSequence(requireActivity());
            sequence.continueOnCancel(true);
            sequence.targets(
                    TapTarget.forView(mMicrophone, microphone).
                            transparentTarget(true).
                            dimColor(R.color.grey).
                            outerCircleColor(R.color.accent).id(0),
                    TapTarget.forView(mThingy, thingy).
                            transparentTarget(true).
                            dimColor(R.color.grey).
                            outerCircleColor(R.color.accent).id(1)
            ).listener(new TapTargetSequence.Listener() {
                @Override
                public void onSequenceFinish() {
                    Utils.saveSequenceCompletion(requireContext(), Utils.INITIAL_SOUND_TUTORIAL);
                    displayStreamingInformationDialog();
                }

                @Override
                public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {

                }

                @Override
                public void onSequenceCanceled(TapTarget lastTarget) {

                }
            }).start();
        }
    }
}

;