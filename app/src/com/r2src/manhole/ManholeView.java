package com.r2src.manhole;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.util.Log;
import java.util.LinkedList;

public class ManholeView extends SurfaceView implements SurfaceHolder.Callback {
  public static final String TAG = "ManholeView";
  
  class ManholeThread extends Thread {
    public static final int STATE_LOSE    = 1;
    public static final int STATE_PAUSE   = 2;
    public static final int STATE_READY   = 3;
    public static final int STATE_RUNNING = 4;
    public static final int STATE_WIN     = 5;
    
    
    public static final float MANHOLE_RATIO_X1 = 216.5f / 800.0f;
    public static final float MANHOLE_RATIO_X2 = 539.0f / 800.0f;
    public static final float MANHOLE_RATIO_Y1 = 132.0f / 506.0f;
    public static final float MANHOLE_RATIO_Y2 = 370.0f / 506.0f;
    public static final float MANHOLE_RATIO_WIDTH = 43.0f / 800.0f;
    public static final float MANHOLE_RATIO_HEIGHT = 10.0f / 506.0f;
    
    float MANHOLE_X1;
    float MANHOLE_X2;
    float MANHOLE_Y1;
    float MANHOLE_Y2;
    float MANHOLE_WIDTH;
    float MANHOLE_HEIGHT;
    
    public static final float MAN1_RATIO_X = 43.0f / 800.0f;
    public static final float MAN1_RATIO_Y = 130.0f / 800.0f;
    public static final float STEP_X1 = 30.0f / 800.0f;
    
    private Bitmap backgroundImage;
    private Paint paint;
    
    private int canvasHeight = 1;
    private int canvasWidth  = 1;
    
    private Handler handler;
    
    private long lastTime;
    
    private boolean run = false;
    private int mode = STATE_READY;
    
    private RectF manholeCover;
    private Bitmap man1_drawable;
    
    private LinkedList<Man> men;
    
    private SurfaceHolder surfaceHolder;
    
    public ManholeThread(SurfaceHolder sh, Context c, Handler h) {
      surfaceHolder = sh;
      handler = h;
      context = c;
      
      Resources res = context.getResources();
      backgroundImage = BitmapFactory.decodeResource(res, R.drawable.background);
      manholeCover = new RectF(0, 0, 0, 0);
      man1_drawable = BitmapFactory.decodeResource(res, R.drawable.man1);
      men = new LinkedList<Man>();
      paint = new Paint();
      paint.setColor(Color.GRAY);
    }
    
    public void doStart() {
      synchronized (surfaceHolder) {
        lastTime = System.currentTimeMillis() + 100;
        setState(STATE_RUNNING);
        men.add(new Man(0, MANHOLE_Y1 - man1_drawable.getHeight()));
      }
    }
    
    public void pause() {
      synchronized (surfaceHolder) {
        if (mode == STATE_RUNNING) setState(STATE_PAUSE);
      }
    }
    
    public synchronized void restoreState(Bundle savedState) {
      synchronized (surfaceHolder) {
        setState(STATE_PAUSE);
      }
    }
    
    @Override
    public void run() {
      while(run) {
        Canvas c = null;
        try {
          c = surfaceHolder.lockCanvas(null);
          synchronized (surfaceHolder) {
            if (mode == STATE_RUNNING) update();
            doDraw(c);
          }
        }
        finally {
          if (c != null)
            surfaceHolder.unlockCanvasAndPost(c);
        }
      }
    }
    
    public Bundle saveState(Bundle map) {
      synchronized (surfaceHolder) {
        //TODO: fill saveState
      }
      return map;
    }
    
    public void setRunning(boolean b) {
      run = b;
    }
    
    public void setState(int mode) {
      synchronized (surfaceHolder) {
        setState(mode, null);
      }
    }
    
    public void setState(int mode, CharSequence message) {
      this.mode = mode;
      // TODO: handle message
    } 
    
    public void setSurfaceSize(int width, int height) {
      synchronized (surfaceHolder) {
        canvasWidth = width;
        canvasHeight = height;
        
        backgroundImage = Bitmap.createScaledBitmap(backgroundImage,
            width, height, true);
        MANHOLE_X1 = width * MANHOLE_RATIO_X1;
        MANHOLE_X2 = width * MANHOLE_RATIO_X2;
        MANHOLE_Y1 = height * MANHOLE_RATIO_Y1;
        MANHOLE_Y2 = height * MANHOLE_RATIO_Y2;
        MANHOLE_WIDTH = width * MANHOLE_RATIO_WIDTH;
        MANHOLE_HEIGHT = height * MANHOLE_RATIO_HEIGHT;
        DISTANCE_X = width / 8.0f;
        DISTANCE_Y = height / 8.0f;
        man1_drawable = Bitmap.createScaledBitmap(man1_drawable,
            (int) (width * MAN1_RATIO_X), (int) (height * MAN1_RATIO_Y)
            , true);
      }
    }
    
    public void unpause() {
      synchronized (surfaceHolder) {
        lastTime = System.currentTimeMillis() + 100;
      }
      setState(STATE_RUNNING);
    }
    
    private void doDraw(Canvas canvas) {
      canvas.drawBitmap(backgroundImage, 0, 0, null);
      canvas.drawRect(manholeCover, paint);
      for (Man man : men) {
        canvas.drawBitmap(man1_drawable, man.x, man.y, null);
      }
    }
    
    private void update() {
      long now = System.currentTimeMillis();
      if (lastTime > now) return;
      for (Man man : men) {
        // TODO: move downward when falling
        // TODO: move left / right depending on direction
        man.x += STEP_X1;
      }
    }
    
    private void setManholeCover(int i) {
      float left = (i == 1 || i == 3) ? MANHOLE_X1 : MANHOLE_X2;
      float top = (i == 1 || i == 2) ? MANHOLE_Y1 : MANHOLE_Y2;
      Log.d(TAG, "Setting rect to " + left + ", " + top);
      manholeCover.set(left, top, left + MANHOLE_WIDTH, top + MANHOLE_HEIGHT);
    }
    
    float DISTANCE_X = 0;
    float DISTANCE_Y = 0;
    
    void doOnTouch(MotionEvent event) {
      synchronized (surfaceHolder) {
        if (mode != STATE_RUNNING) {
          doStart();
        }
        float x = event.getX();
        float y = event.getY();
        float dist_x1 = Math.abs(x - MANHOLE_X1);
        float dist_x2 = Math.abs(x - MANHOLE_X2);
        float dist_y1 = Math.abs(y - MANHOLE_Y1);
        float dist_y2 = Math.abs(y - MANHOLE_Y2);
        if (dist_x1 < DISTANCE_X && dist_y1 < DISTANCE_Y) {
          setManholeCover(1);
        }
        else if (dist_x2 < DISTANCE_X && dist_y1 < DISTANCE_Y) {
          setManholeCover(2);
        }
        else if (dist_x1 < DISTANCE_X && dist_y2 < DISTANCE_Y) {
          setManholeCover(3);
        }
        else if (dist_x2 < DISTANCE_X && dist_y2 < DISTANCE_Y) {
          setManholeCover(4);
        }
      }
    }
  }
  
  private Context context;
  
  private ManholeThread thread;
  
  public ManholeView(Context context, AttributeSet attrSet) {
    super(context, attrSet);
    SurfaceHolder holder = getHolder();
    holder.addCallback(this);
    
    thread = new ManholeThread(holder, context, new Handler() {
      @Override
      public void handleMessage(Message m) {
        // TODO fill handleMessage
      }
    });
    
    setFocusable(true);
  }
  
  public ManholeThread getThread() {
    return thread;
  }
  
  @Override
  public void onWindowFocusChanged(boolean hasWindowFocus) {
    if (!hasWindowFocus) thread.pause();
  }
  
  public void surfaceChanged(SurfaceHolder holder, int format, int width,
      int height) {
    thread.setSurfaceSize(width, height);
  }
  
  public void surfaceCreated(SurfaceHolder holder) {
    thread.setRunning(true);
    thread.start();
  }
  
  public void surfaceDestroyed(SurfaceHolder holder) {
    boolean retry = true;
    thread.setRunning(false);
    while(retry) {
      try {
        thread.join();
        retry = false;
      }
      catch (InterruptedException e) { }
    }
  }
  
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      Log.d(TAG, "Touched: " + event.getX() + ", " + event.getY());
      thread.doOnTouch(event);
    }
    return true;
  }
  
  class Man {
    
    float x, y;
    public Man(float x, float y) {
      this.x = x;
      this.y = y;
      
    }
    
    
  }
}
