package famoco.com.pingpongavailability.viewhandlers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import famoco.com.pingpongavailability.R;
import famoco.com.pingpongavailability.activities.MainActivity;
import famoco.com.pingpongavailability.helpers.Constants;

public class MessageDisplayHandler extends View {

    private final static int TABLE_WIDTH = 20; // table indexes
    private final static int TABLE_HEIGHT = 30; // table indexes

    private final static int Y_START = 7; // table indexes
    private final static int X_START = 5; // table indexes
    private final static int GAP_BETWEEN_CHARACTERS = 10; // table indexes

    private int mDevicePosition;

    private static int WIDTH, HEIGHT; // pxl
    private final static int GAP_BETWEEN_LEDS = 1; //pxl
    private static float LED_WIDTH, LED_HEIGHT; //pxl

    private Paint mPaint;
    private RectF mLedRect;

    private String mMessage;
    private String mColor;
    private int mCurrentlyDisplayedCharacterIndex;
    private int mCurrentXStart, mNextXStart;

    private boolean ready;
    private boolean running;

    private byte[][] mLedsTable;

    private MyThread mThread;

    public MessageDisplayHandler(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MessageDisplayHandler(Context context) {
        super(context);
        init();
    }

    public MessageDisplayHandler(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.BLACK);

        mLedRect = new RectF();

        mLedsTable = new byte[TABLE_WIDTH][TABLE_HEIGHT];
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        WIDTH = w;
        HEIGHT = h;

        LED_WIDTH = (WIDTH - TABLE_WIDTH - 2) / (float) TABLE_WIDTH;
        LED_HEIGHT = (HEIGHT - TABLE_HEIGHT - 2) / (float) TABLE_HEIGHT;

        Log.d(Constants.TAG, "" + LED_HEIGHT + " " + LED_WIDTH);
        ready = true;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (ready) {
            for (int col = 0; col < mLedsTable.length; col++) {
                for (int row = 0; row < mLedsTable[0].length; row++) {
                    if (mLedsTable[col][row] == 1) {
                        mPaint.setColor(getCurrentColor());
                    } else {
                        mPaint.setColor(ContextCompat.getColor(getContext(), R.color.screen_led_off));
                    }
                    mLedRect.left = (GAP_BETWEEN_LEDS * (col + 1)) + (LED_WIDTH * col);
                    mLedRect.top = (GAP_BETWEEN_LEDS * (row + 1)) + (LED_HEIGHT * row);
                    mLedRect.right = (GAP_BETWEEN_LEDS * (col + 1)) + (LED_WIDTH * col) + LED_WIDTH;
                    mLedRect.bottom= (GAP_BETWEEN_LEDS * (row + 1)) + (LED_HEIGHT * row) + LED_HEIGHT;
                    canvas.drawRoundRect(mLedRect, 3.0f, 3.0f, mPaint);
                }
            }
        }
    }

    public void printMessage(String message, String color, final int sleepTime,
                             final MainActivity activity) {
        mMessage = message;
        mColor = color;
        mCurrentlyDisplayedCharacterIndex = mDevicePosition;
        mCurrentXStart = X_START;
        mNextXStart = X_START + GAP_BETWEEN_CHARACTERS + CharacterHandler.CHARACTER_WIDTH;

        if (!running) {
            mThread = new MyThread(activity, sleepTime);
            running = true;
            new Thread(mThread).start();
        } else {
            mThread.setSleepTime(sleepTime);
        }
    }

    private void clearLedsTable() {
        for (int i = 0; i < TABLE_WIDTH; i++) {
            for (int j = 0; j < TABLE_HEIGHT; j++) {
                mLedsTable[i][j] = 0;
            }
        }
    }

    private void addCurrentCharacter() {
        byte[][] characterTab = CharacterHandler.printCharacterInTable(getCurrentCharacter());
        for (int column = 0; column < characterTab[0].length; column++) {
            if (mCurrentXStart + column < 0) {
                continue;
            }
            for (int row = 0; row < characterTab.length; row++) {
                mLedsTable[mCurrentXStart + column][Y_START + row] = characterTab[row][column];
            }
        }
    }

    private void addNextCharacter() {
        byte[][] characterTab = CharacterHandler.printCharacterInTable(getNextCharacter());
        for (int column = 0; column < characterTab[0].length; column++) {
            if (mNextXStart + column >= TABLE_WIDTH) {
                break;
            }
            for (int row = 0; row < characterTab.length; row++) {
                mLedsTable[mNextXStart + column][Y_START + row] = characterTab[row][column];
            }
        }
    }

    private void drawMessage(MainActivity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

    private boolean isCurrentCharacterVisible() {
        return mCurrentXStart > -CharacterHandler.CHARACTER_WIDTH;
    }

    private boolean isNextCharacterVisible() {
        return mNextXStart < TABLE_WIDTH;
    }

    private char getCurrentCharacter() {
        return mMessage.charAt(mCurrentlyDisplayedCharacterIndex);
    }

    private char getNextCharacter() {
        return mMessage.length()-1 == mCurrentlyDisplayedCharacterIndex ?
                mMessage.charAt(0) : mMessage.charAt(mCurrentlyDisplayedCharacterIndex+1);
    }

    private int getNextCharacterIndex() {
        return mMessage.length()-1 == mCurrentlyDisplayedCharacterIndex ?
                0 : mCurrentlyDisplayedCharacterIndex+1;
    }

    private int getCurrentColor() {
        return Color.parseColor(mColor);
    }

    public void setDevicePosition(int devicePosition) {
        this.mDevicePosition = devicePosition;
    }

    private class MyThread implements Runnable {

        private MainActivity mActivity;
        private int mSleepTime;

        public MyThread(MainActivity activity, int sleepTime) {
            mActivity = activity;
            mSleepTime = sleepTime;
        }

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()) {
                if (!isCurrentCharacterVisible()) {
                    mCurrentlyDisplayedCharacterIndex = getNextCharacterIndex();
                    mCurrentXStart = mNextXStart;
                    mNextXStart = mCurrentXStart + GAP_BETWEEN_CHARACTERS
                            + CharacterHandler.CHARACTER_WIDTH;
                }
                clearLedsTable();
                addCurrentCharacter();
                if (isNextCharacterVisible()) {
                    addNextCharacter();
                }
                drawMessage(mActivity);
                mCurrentXStart--;
                mNextXStart--;
                try {
                    Thread.sleep(mSleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void setSleepTime(int mSleepTime) {
            this.mSleepTime = mSleepTime;
        }
    }
}
