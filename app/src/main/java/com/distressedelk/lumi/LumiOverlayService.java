package com.distressedelk.lumi;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

public class LumiOverlayService extends Service {
    private WindowManager wm;
    private TextView bubble;
    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        bubble = new TextView(this);
        bubble.setText("✦  Lumi\nI'm here");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(18);
        bubble.setGravity(Gravity.CENTER);
        int pad = 28; bubble.setPadding(pad,pad,pad,pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(235,20,28,38)); bg.setCornerRadius(36); bg.setStroke(2, Color.rgb(127,232,255));
        bubble.setBackground(bg);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(360,220,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.END; p.x = 24; p.y = 180;
        wm.addView(bubble,p);
        bubble.setOnClickListener(v -> { Intent i = new Intent(this, MainActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); stopSelf(); });
    }
    @Override public void onDestroy() { if (bubble != null) wm.removeView(bubble); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
