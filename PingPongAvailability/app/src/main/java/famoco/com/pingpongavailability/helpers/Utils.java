package famoco.com.pingpongavailability.helpers;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import famoco.com.pingpongavailability.activities.MainActivity;

public class Utils {

    public static String getSharedPreferencesString(String key, MainActivity activity) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        return sharedPreferences.getString(key, null);
    }

    public static int getSharedPreferencesInt(String key, MainActivity activity) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        return sharedPreferences.getInt(key, -1);
    }

    public static void addSharedPreferencesString(String value, String key, MainActivity activity) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static void addSharedPreferencesInt(int value, String key, MainActivity activity) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static void hideSoftKeyboard(View view, Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void setScreenBrightness(Activity activity, int value) {
        Window window = activity.getWindow();
        ContentResolver cResolver = activity.getContentResolver();

        Settings.System.putInt(cResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

        Settings.System.putInt(cResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE, value);

        WindowManager.LayoutParams lp = window.getAttributes();
        lp.screenBrightness = value / (float) 255;
        window.setAttributes(lp);
    }
}
