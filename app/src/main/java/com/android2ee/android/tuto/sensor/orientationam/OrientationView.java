/**
 * <ul>
 * <li>SensorAccelerationTuto</li>
 * <li>com.android2ee.android.tuto.sensor.accelation</li>
 * <li>17 nov. 2011</li>
 * <p>
 * <li>======================================================</li>
 * <p>
 * <li>Projet : Mathias Seguy Project</li>
 * <li>Produit par MSE.</li>
 * <p>
 * /**
 * <ul>
 * Android Tutorial, An <strong>Android2EE</strong>'s project.</br>
 * Produced by <strong>Dr. Mathias SEGUY</strong>.</br>
 * Delivered by <strong>http://android2ee.com/</strong></br>
 * Belongs to <strong>Mathias Seguy</strong></br>
 * ***************************************************************************************************************</br>
 * This code is free for any usage but can't be distribute.</br>
 * The distribution is reserved to the site <strong>http://android2ee.com</strong>.</br>
 * The intelectual property belongs to <strong>Mathias Seguy</strong>.</br>
 * <em>http://mathias-seguy.developpez.com/</em></br> </br>
 * <p>
 * *****************************************************************************************************************</br>
 * Ce code est libre de toute utilisation mais n'est pas distribuable.</br>
 * Sa distribution est reservée au site <strong>http://android2ee.com</strong>.</br>
 * Sa propriété intellectuelle appartient a <strong>Mathias Seguy</strong>.</br>
 * <em>http://mathias-seguy.developpez.com/</em></br> </br>
 * *****************************************************************************************************************</br>
 */
package com.android2ee.android.tuto.sensor.orientationam;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Mathias Seguy (Android2EE)
 * @goals This class aims to draw a vector (x,y,z) that represents the sensor value. This vector is
 * represents by a point (x,y) and the change of the background colors. It also draws:
 * <ul>
 * <li>A point (Accelerate Point In Void) that is the point that moves according to the
 * strength of the vector considered as a accelerator vector</li>
 * <li>A fake point (Fake Point) that is the point that moves according to the strength of
 * the vector considered as a speed vector</li>
 * <li>The X and Y axis</li>
 * <li>The Min and Max rectangle of the sensor reached values</li>
 * </ul>
 * This class use a Thread to redraw the screen (30 redrawing by second).
 * The use of an handler for the thread and the gui thread communication is used too.
 */
public class OrientationView extends View {
    private static final String TAG = "OrientationView";
    /******************************************************************************************/
    /** Private constant **************************************************************************/
    /******************************************************************************************/
    /**
     * The tag for the log
     */
    private static final String tag = "SensorsAccelerometer";
    /******************************************************************************************/
    /** Attributes associated to the main activity and the screen *****************************/
    /******************************************************************************************/
    /**
     * Main activity
     */
    private SensorOrientationTutoActivity activity;
    /******************************************************************************************/
    /** Attributes associated to canvas **************************************************************************/
    /******************************************************************************************/
    /**
     * The paint to draw the view
     */
    private Paint paint = new Paint();
    /**
     * The Canvas to draw within
     */
    private Canvas canvas;
    private Path northPath = new Path();
    private Path southPath = new Path();
    private boolean mAnimate;
    private Paint.FontMetrics mFontMetrics;
    private int textColor, backgroundCircleColor, circleColor;
    private float fontHeight;
    /******************************************************************************************/
    /** Attributes used to manage the points coordinates **************************************/
    /******************************************************************************************/

    /**
     * The source point of the transformation matrix
     */
    float[] src = new float[8];
    /**
     * The destination point of the transformation matrix
     */
    float[] dst = new float[8];

    /******************************************************************************************/
    /** Handler and Thread attribute **********************************************************/
    /******************************************************************************************/
    /**
     * The boolean to initialize the data upon
     */
    boolean init = false;
    /**
     * An atomic boolean to manage the external thread's destruction
     */
    AtomicBoolean isRunning = new AtomicBoolean(false);
    /**
     * An atomic boolean to manage the external thread's destruction
     */
    AtomicBoolean isPausing = new AtomicBoolean(false);
    /**
     * The handler used to slow down the re-drawing of the view, else the device's battery is
     * consumed
     */
    private final Handler slowDownDrawingHandler;
    /**
     * The thread that call the redraw
     */
    private Thread background;

    /******************************************************************************************/
    /** Constructors **************************************************************************/
    /******************************************************************************************/

    /**
     * @param context
     */
    public OrientationView(Context context) {
        super(context);
        // instanciate the calling activity
        activity = (SensorOrientationTutoActivity) context;
        // Define the path that draw the North arrow (a path is a sucession of lines)
        northPath.moveTo(0, -60);
        northPath.lineTo(-10, 0);
        northPath.lineTo(10, 0);
        northPath.close();
        // Define the path that draw the South arrow (a path is a sucession of lines)
        southPath.moveTo(-10, 0);
        southPath.lineTo(0, 60);
        southPath.lineTo(10, 0);
        southPath.close();
        // Define how to draw text
        paint.setTextSize(20);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        mFontMetrics = paint.getFontMetrics();
        fontHeight = mFontMetrics.ascent + mFontMetrics.descent;
        // define the color used
        Resources res = getResources();
        textColor = res.getColor(R.color.text_color);
        backgroundCircleColor = res.getColor(R.color.background_circle_color);
        circleColor = res.getColor(R.color.circle_color);
        // handler definition to redraw the screen (not too quickly)
        slowDownDrawingHandler = new Handler() {
            /*
             * (non-Javadoc)
             *
             * @see android.os.Handler#handleMessage(android.os.Message)
             */
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                redraw();
            }
        };
        // Launching the Thread to update draw
        background = new Thread(new Runnable() {
            /**
             * The message exchanged between this thread and the handler
             */
            Message myMessage;

            // Overriden Run method
            public void run() {
                try {
                    while (isRunning.get()) {
                        if (isPausing.get()) {
                            Thread.sleep(2000);
                        } else {
                            // Redraw to have 30 images by second
                            Thread.sleep(1000 / 30);
                            // Send the message to the handler (the handler.obtainMessage is more
                            // efficient that creating a message from scratch)
                            // create a message, the best way is to use that method:
                            myMessage = slowDownDrawingHandler.obtainMessage();
                            // then send the message

                            slowDownDrawingHandler.sendMessage(myMessage);
                        }
                    }
                } catch (Throwable t) {
                    // just end the background thread
                }
            }
        });
        // Initialize the threadSafe booleans
        isRunning.set(true);
        isPausing.set(false);
        // and start it
        background.start();
    }

    /******************************************************************************************/
    /** Drawing method **************************************************************************/
    /******************************************************************************************/

    /**
     * The method to redraw the view
     */
    private void redraw() {
        // Log.d(tag, "redraw");
        // and make sure to redraw asap
        invalidate();
    }
    //initialize the needed size
    /**
     * width and height of the view
     */
    int w, h;
    /**
     * Center x and center y
     */
    int cx, cy;
    /**
     * The quarter of the hight
     */
    int quarterH;

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.w = w;
        this.h = h;
        cx = w / 2;
        cy = h / 2;
        quarterH = h / 4;
        Log.e(TAG, "The size is w=" + w + " and h=" + h);
        int width, height;
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Point size = new Point();
            display.getSize(size);
            width = size.x;
            height = size.y;
        } else {
            width = display.getWidth();  // deprecated
            height = display.getHeight();  // deprecated
        }
        Log.e(TAG, "The screen size is w=" + width + " and h=" + height);
    }

    @Override
    public void onDraw(Canvas canvas) {

        // draw the grid
        drawGrid(canvas);
        // draw the north arrow
        drawNorthArrow(canvas);
        //draw the referential
//        drawReferential(canvas);
        // draw the pitch
        drawPitchCircle(canvas);
        // draw the roll
        drawRollCircle(canvas);

    }


    /**
     * Draw the refenential on screen, is good to detect canvas.restore leak
     * @param canvas
     */
    private void drawReferential(Canvas canvas){
//        paint.setColor(textColor);
        paint.setTextSize(12);
        for (int x=0; x<w;x=x+100 ){
            for(int y=0;y<h;y=y+100){
                canvas.drawText("("+x+","+y+")", x, y, paint);
            }
        }
    }

    /**
     * @param canvas
     */

    private void drawGrid(Canvas canvas) {
        paint.setColor(Color.RED);
        canvas.drawLine(w / 2, 0, w / 2, h, paint);
        canvas.drawLine(w / 4, 0, w / 4, h, paint);
        canvas.drawLine(3 * w / 4, 0, 3 * w / 4, h, paint);
        canvas.drawLine(0, h / 2, w, h / 2, paint);
        canvas.drawLine(0, h / 4, w, h / 4, paint);
        canvas.drawLine(0, 3 * h / 4, w, 3 * h / 4, paint);
    }

    /**
     * @param canvas
     */
    private void drawRollCircle(Canvas canvas) {
        // first save the canvas configuration (orientation, translation)
        canvas.save();
        // find the roll to display
        float roll = activity.z;
        // Translate the canvas to draw the circle int the first upper quarter of the screen
        // center on the w/2 axis
        canvas.translate(cx, cy - 3 * quarterH / 2);

        // rotate the arc for it to map the roll value
        canvas.rotate(roll, 0, 0);
        // Define the rectangle in which the circle is drawn
        RectF pitchOval = new RectF(-quarterH / 2, -quarterH / 2, quarterH / 2, quarterH / 2);
        // draw the background circle
        paint.setColor(backgroundCircleColor);
        canvas.drawArc(pitchOval, 0, 360, false, paint);
        // draw the roll circle
        paint.setColor(circleColor);
        canvas.drawArc(pitchOval, 0, 180, false, paint);
        // and now displays the value of the roll
        paint.setColor(textColor);
        canvas.drawText(((int) roll) + "", 0, -5, paint);
        //And below draw the name Roll
        String rollName = getResources().getString(R.string.roll_name);
        float hText = -fontHeight + 5;
        canvas.drawText(rollName, 0, hText, paint);
        // and now go back to the initial canvas state
        canvas.restore();
    }

    /**
     * @param canvas
     */
    private void drawPitchCircle(Canvas canvas) {
        // first save the canvas configuration (orientation, translation)
        canvas.save();
        // find the pitch to display
        float pitch = activity.y;
        // Translate the canvas to draw the circle int the last down quarter of the screen
        // center on the w/2 axis
        canvas.translate(cx, cy + 3 * quarterH / 2);
        // Define the rectangle in which the circle is drawn
        RectF pitchOval = new RectF(-quarterH / 2, -quarterH / 2, quarterH / 2, quarterH / 2);
        // draw the background circle
        paint.setColor(backgroundCircleColor);
        canvas.drawCircle(0, 0, quarterH / 2, paint);
        // draw the roll circle
        paint.setColor(circleColor);
        //What i want is:90+pitch;360+2*pitch
        //when pitch=0, startAngle= 90, sweepAngle=360
        //when pitch=-45, startAngle= 90+pitch, sweepAngle=360+2*pitch
        //when pitch=-90, startAngle= 0, sweepAngle=180
        //when pitch=-135, startAngle= 90+pitch, sweepAngle=360+2*pitch
        //when pitch=90, startAngle= 0, sweepAngle=-180
        if (pitch <= 0) {
            canvas.drawArc(pitchOval, -90 - pitch, 360 + 2 * pitch, false, paint);
        } else {
            canvas.drawArc(pitchOval, 90 + pitch, 360 - 2 * pitch, false, paint);
        }
        //before
        //canvas.drawArc(pitchOval, 0 - pitch / 2, 180 + pitch, false, paint);
        // and now displays the value of the roll
        paint.setColor(textColor);
        canvas.drawText(((int) pitch) + "", 0, -5, paint);
        //And below draw the name Pitch
        String pitchName = getResources().getString(R.string.pitch_name);
        float hText = -fontHeight + 5;
        canvas.drawText(pitchName, 0, hText, paint);
        // and now go back to the initial canvas state
        canvas.restore();
    }

    /**
     * @param canvas
     */
    private void drawNorthArrow(Canvas canvas) {
        canvas.save();
        // get the azimuth to display
        float azimut = -activity.x;
        // center the arrow in the middle of the screen
        canvas.translate(cx, cy);
        // rotate the canvas such that the arrow will indicate the north
        // even if it draws vertical
        canvas.rotate(azimut);
        // draw the background circle
        paint.setColor(circleColor);
        canvas.drawCircle(0, 0, quarterH / 2 - fontHeight + 10, paint);
        paint.setColor(backgroundCircleColor);
        canvas.drawCircle(0, 0, quarterH / 2, paint);

        // Now display the scale of the compass
        paint.setColor(Color.WHITE);
        // display the graduation
        //the text position

        float hText = -quarterH / 2 - fontHeight + 3;
        // each 15° draw a graduation |
        int step = 15;
        for (int degree = 0; degree < 360; degree = degree + step) {
            // if it's not a cardinal point draw the graduation
            if ((degree % 45) != 0) {
                canvas.drawText("|", 0, hText, paint);
            }
            canvas.rotate(-step);
        }

        // then draw cardinal points
        canvas.drawText("N", 0, hText, paint);
        canvas.rotate(-45);
        canvas.drawText("NW", 0, hText, paint);
        canvas.rotate(-45);
        canvas.drawText("W", 0, hText, paint);
        canvas.rotate(-45);
        canvas.drawText("SW", 0, hText, paint);
        canvas.rotate(-45);
        canvas.drawText("S", 0, hText, paint);
        canvas.rotate(-45);
        canvas.drawText("SE", 0, hText, paint);
        canvas.rotate(-45);
        canvas.drawText("E", 0, hText, paint);
        canvas.rotate(-45);
        canvas.drawText("NE", 0, hText, paint);
        canvas.rotate(-45);

        // Draw the arrow
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.RED);
        canvas.drawPath(northPath, paint);
        paint.setColor(circleColor);
        canvas.drawPath(southPath, paint);

        // and now displays the value of the azimut
        paint.setColor(textColor);
        paint.setStyle(Paint.Style.FILL);
        //Draw the text horizontal
        canvas.restore();
        canvas.save();
        // center the arrow in the middle of the screen
        float hTextValue = -quarterH / 2 + fontHeight - 5;
        canvas.translate(cx, cy);
        canvas.drawText(((int) activity.x) + "", 0, -hTextValue, paint);
        canvas.restore();
    }
}
