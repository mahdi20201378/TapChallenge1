package com.example.tapchallenge;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private int score = 0;
    private int best = 0;
    private int seconds = 30;
    private boolean running = false;
    private CountDownTimer timer;
    private TextView scoreView, timeView, bestView, statusView;
    private Button tapButton, startButton;
    private LinearLayout root;

    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density); }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height));
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(16,20,38));
        getWindow().setNavigationBarColor(Color.rgb(16,20,38));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(28), dp(24), dp(24));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(24,31,59), Color.rgb(12,15,29)}));

        TextView title = label("TAP CHALLENGE", 27, Color.WHITE, true);
        root.addView(title, params(-1, 54));
        TextView subtitle = label("Tap as fast as you can!", 15, Color.rgb(183,190,218), false);
        root.addView(subtitle, params(-1, 34));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setGravity(Gravity.CENTER);
        stats.setPadding(0, dp(18), 0, dp(18));
        root.addView(stats, params(-1, 112));
        scoreView = statCard(stats, "SCORE", "0");
        timeView = statCard(stats, "TIME", "30s");
        bestView = statCard(stats, "BEST", "0");

        statusView = label("Ready? Press START to begin.", 16, Color.WHITE, true);
        root.addView(statusView, params(-1, 58));

        tapButton = new Button(this);
        tapButton.setText("TAP!");
        tapButton.setTextSize(30);
        tapButton.setTextColor(Color.WHITE);
        tapButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tapButton.setAllCaps(false);
        tapButton.setBackground(shape(Color.rgb(111,79,255), 120));
        tapButton.setElevation(dp(10));
        LinearLayout.LayoutParams tapParams = params(220, 220);
        tapParams.gravity = Gravity.CENTER;
        tapParams.topMargin = dp(16);
        tapParams.bottomMargin = dp(24);
        root.addView(tapButton, tapParams);
        tapButton.setEnabled(false);
        tapButton.setOnClickListener(v -> {
            if (running) {
                score++;
                scoreView.setText(String.valueOf(score));
                if (score > best) { best = score; bestView.setText(String.valueOf(best)); }
            }
        });

        startButton = new Button(this);
        startButton.setText("START GAME");
        startButton.setTextSize(17);
        startButton.setTextColor(Color.WHITE);
        startButton.setAllCaps(false);
        startButton.setBackground(shape(Color.rgb(28,190,150), 18));
        root.addView(startButton, params(-1, 58));
        startButton.setOnClickListener(v -> startGame());

        TextView footer = label("30 seconds • One tap = one point", 13, Color.rgb(150,160,190), false);
        LinearLayout.LayoutParams fp = params(-1, 48); fp.topMargin = dp(14);
        root.addView(footer, fp);
        setContentView(root);
    }

    private TextView statCard(LinearLayout parent, String caption, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackground(shape(Color.rgb(35,43,75), 16));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(82), 1f);
        cp.setMargins(dp(5), 0, dp(5), 0);
        parent.addView(card, cp);
        TextView cap = label(caption, 11, Color.rgb(164,174,210), true);
        card.addView(cap, params(-1, 28));
        TextView val = label(value, 24, Color.WHITE, true);
        card.addView(val, params(-1, 40));
        return val;
    }

    private void startGame() {
        if (timer != null) timer.cancel();
        score = 0; seconds = 30; running = true;
        scoreView.setText("0"); timeView.setText("30s");
        statusView.setText("GO! GO! GO!");
        tapButton.setEnabled(true);
        tapButton.setAlpha(1f);
        startButton.setEnabled(false);
        startButton.setText("PLAYING...");
        timer = new CountDownTimer(30000, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                seconds = (int)Math.ceil(millisUntilFinished / 1000.0);
                timeView.setText(seconds + "s");
            }
            @Override public void onFinish() {
                running = false;
                timeView.setText("0s");
                tapButton.setEnabled(false);
                tapButton.setAlpha(0.55f);
                statusView.setText("Time's up! You scored " + score + "!");
                startButton.setEnabled(true);
                startButton.setText("PLAY AGAIN");
            }
        }.start();
    }

    @Override protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }
}
