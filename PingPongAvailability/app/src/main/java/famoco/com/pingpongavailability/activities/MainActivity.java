package famoco.com.pingpongavailability.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import famoco.com.pingpongavailability.connections.ClientConnection;
import famoco.com.pingpongavailability.helpers.Utils;
import famoco.com.pingpongavailability.viewhandlers.MessageDisplayHandler;
import famoco.com.pingpongavailability.R;
import famoco.com.pingpongavailability.helpers.Constants;

public class MainActivity extends Activity {

    private ClientConnection mClientConnection;
    private MessageDisplayHandler mMessageDisplayHandler;

    private GestureDetector mGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(Constants.TAG, getImei());
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            public void onLongPress(MotionEvent e) {
                showIpDialog(MainActivity.this.getResources().getString(R.string.dialog_title_default));
            }
        });
        mClientConnection = new ClientConnection(this);
        mMessageDisplayHandler = (MessageDisplayHandler) findViewById(R.id.displayer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setScreenBrightness(this, Constants.BRIGHTNESS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMessageDisplayHandler.stopRefreshHandler();
        mClientConnection.disconnect();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    public void showIpDialog(String title) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.ip_dialog);

        TextView titleText = (TextView) dialog.findViewById(R.id.dialog_title);
        titleText.setText(title);

        final EditText ipEdit = (EditText) dialog.findViewById(R.id.dialog_edit_ip);
        ipEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    Utils.hideSoftKeyboard(v, MainActivity.this);
                }
            }
        });
        ipEdit.setText(Utils.getSharedPreferencesString("ip", this));

        final EditText portEdit = (EditText) dialog.findViewById(R.id.dialog_edit_port);
        portEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    Utils.hideSoftKeyboard(v, MainActivity.this);
                }
            }
        });
        portEdit.setText("" + Utils.getSharedPreferencesInt("port", this));

        final TextView errorText = (TextView) dialog.findViewById(R.id.dialog_port_error);

        final Button connect = (Button) dialog.findViewById(R.id.dialog_confirm);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = ipEdit.getText().toString();
                String port = portEdit.getText().toString();
                if (!isInformationCorrect(ip, port)) {
                    errorText.setVisibility(View.VISIBLE);
                } else {
                    errorText.setVisibility(View.INVISIBLE);
                    int portInt = Integer.parseInt(port);
                    mClientConnection.changeIp(ip, portInt);
                    mClientConnection.startServerCommunication();
                    dialog.cancel();
                }
            }

            private boolean isInformationCorrect(String ip, String port) {
                if (ip.isEmpty() || port.isEmpty()) {
                    return false;
                }
                if (!ip.matches("[0-9.]*")) {
                    return false;
                }
                if (ip.endsWith(".")) {
                    return false;
                }
                return true;
            }
        });

        final Button cancel = (Button) dialog.findViewById(R.id.dialog_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    public void setDevicePosition(int position) {
        mMessageDisplayHandler.setDevicePosition(position);
    }

    public void printMessage(String message, String color, int sleepTime) {
        mMessageDisplayHandler.printMessage(message, color, sleepTime, this);
    }

    public String getImei() {
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }
}
