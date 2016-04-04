package famoco.com.pingpongavailability.connections;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TabHost;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import famoco.com.pingpongavailability.R;
import famoco.com.pingpongavailability.activities.MainActivity;
import famoco.com.pingpongavailability.helpers.Constants;
import famoco.com.pingpongavailability.helpers.Utils;

public class ClientConnection {

    private MainActivity mMainActivity;

    private String mServerIp;
    private int mServerPort;

    private Socket mSocket;
    private PrintWriter mPrintWriter;
    private BufferedReader mBufferedReader;

    private Thread mReadingThread;

    private Handler mHeartBeatHandler;
    private Runnable mHeartBeatRunnable;

    private boolean connected;
    private boolean waitingForResponse;

    public ClientConnection(final MainActivity mainActivity) {
        mMainActivity = mainActivity;

        mServerIp = Utils.getSharedPreferencesString(Constants.IP_KEY, mMainActivity);
        mServerPort = Utils.getSharedPreferencesInt(Constants.PORT_KEY, mMainActivity);

        mHeartBeatHandler = new Handler();
        mHeartBeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (!waitingForResponse) {
                    Log.d(Constants.TAG, "beat");
                    sendMessage("heartbeat");
                    waitingForResponse = true;
                    mHeartBeatHandler.postDelayed(mHeartBeatRunnable,
                            Constants.HEARTBEATS_INTERVALS);
                } else {
                    try {
                        if(mSocket != null) {
                            closeSocket();
                        }
                        showIpDialog();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        if (mServerIp == null || mServerPort == -1) {
            mMainActivity.showIpDialog(mMainActivity.getResources()
                    .getString(R.string.dialog_title_error));
        } else {
            startServerCommunication();
        }
    }

    public void startServerCommunication() {
        if (!connected) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        connect();
                        startHeartBeat();
                        sendMessage(mMainActivity.getImei());
                        startReading();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(Constants.TAG, e.getMessage());
                        if (!connected) {
                            showIpDialog();
                        }
                    }
                }
            }).start();
        }
    }

    private void connect() throws IOException {
        mSocket = new Socket(mServerIp, mServerPort);
        connected = true;
        mPrintWriter = new PrintWriter(new BufferedWriter
                (new OutputStreamWriter(mSocket.getOutputStream())), true);
        mBufferedReader = new BufferedReader(
                new InputStreamReader(mSocket.getInputStream()));
    }

    public void disconnect() {
        if (mSocket != null) {
            if (mSocket.isConnected()) {
                try {
                    sendMessage(Constants.DISCONNECTED);
                    stopHeartBeat();
                    closeSocket();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void closeSocket() throws IOException{
        mReadingThread.interrupt();
        mBufferedReader.close();
        mPrintWriter.close();
        mSocket.close();
        connected = false;
        mSocket = null;
    }

    private void showIpDialog() {
        mMainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMainActivity.showIpDialog(mMainActivity.getResources()
                        .getString(R.string.dialog_title_error));
            }
        });
    }

    public void changeIp(String ip, int port) {
        Utils.addSharedPreferencesString(ip, Constants.IP_KEY, mMainActivity);
        Utils.addSharedPreferencesInt(port, Constants.PORT_KEY, mMainActivity);
        mServerIp = ip;
        mServerPort = port;
    }

    public void sendMessage(String message) {
        mPrintWriter.write(message);
        mPrintWriter.flush();
    }

    private void startReading() {
        mReadingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    readMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        mReadingThread.start();
    }

    private void readMessage() throws IOException {
        String data;

        while ((data = mBufferedReader.readLine()) != null) {
            if (data.startsWith("ping")) {
                sendMessage(data);
            } else if (data.startsWith("offline")) {
                disconnect();
                showIpDialog();
            } else if (data.startsWith("position")) {
                String[] splitData = data.split("/");
                mMainActivity.setDevicePosition(Integer.parseInt(splitData[1]));
            } else if (data.startsWith("heartbeat")){
                Log.d(Constants.TAG, "received");
                waitingForResponse = false;
            } else {
                String[] splitData = data.split("/");
                int devices = Integer.parseInt(splitData[0]);
                final String color = splitData[1];
                int delay = Integer.parseInt(splitData[2]);
                final int sleepTime = Integer.parseInt(splitData[3]);
                String message = splitData[4].toUpperCase();

                if (message.length() >= devices) {
                    message += " ";
                } else {
                    int difference = devices - message.length();
                    for (int i = 0; i < difference; i++) {
                        message += " ";
                    }
                }

                final String formattedMessage = message;
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mMainActivity.printMessage(formattedMessage, color, sleepTime);
                    }
                }, delay);
            }
        }
    }

    private void startHeartBeat() {
        mHeartBeatHandler.postDelayed(mHeartBeatRunnable, Constants.HEARTBEATS_INTERVALS);
    }

    private void stopHeartBeat() {
        mHeartBeatHandler.removeCallbacks(mHeartBeatRunnable);
    }
}
