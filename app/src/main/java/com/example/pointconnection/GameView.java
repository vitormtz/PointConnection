package com.example.pointconnection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.HashMap;
import java.util.Map;

public class GameView extends View {
    private Paint playerPaint;
    private Paint otherPlayersPaint;
    private Paint backgroundPaint;
    private float playerBallX = 540;
    private float playerBallY = 960;
    private static final float BALL_RADIUS = 35;
    private Map<String, Ball> otherPlayers = new HashMap<>();
    private OnBallMoveListener ballMoveListener;

    public interface OnBallMoveListener {
        void onBallMove(float x, float y);
    }

    public static class Ball {
        public float x, y;
        public long lastUpdate;
        public String playerId;

        public Ball(float x, float y, String playerId) {
            this.x = x;
            this.y = y;
            this.playerId = playerId;
            this.lastUpdate = System.currentTimeMillis();
        }
    }

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        playerPaint = new Paint();
        playerPaint.setColor(Color.BLUE);
        playerPaint.setAntiAlias(true);

        otherPlayersPaint = new Paint();
        otherPlayersPaint.setColor(Color.RED);
        otherPlayersPaint.setAntiAlias(true);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLACK);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        canvas.drawCircle(playerBallX, playerBallY, BALL_RADIUS, playerPaint);

        for (Ball ball : otherPlayers.values()) {
            canvas.drawCircle(ball.x, ball.y, BALL_RADIUS, otherPlayersPaint);
        }

        String info = "Jogadores: " + (otherPlayers.size() + 1);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE) {

            float touchX = event.getX();
            float touchY = event.getY();

            movePlayerBall(touchX, touchY);
        }
        return true;
    }

    public void movePlayerBall(float x, float y) {
        playerBallX = Math.max(BALL_RADIUS, Math.min(x, getWidth() - BALL_RADIUS));
        playerBallY = Math.max(BALL_RADIUS, Math.min(y, getHeight() - BALL_RADIUS));

        if (ballMoveListener != null) {
            ballMoveListener.onBallMove(playerBallX, playerBallY);
        }

        invalidate();
    }

    public void updateOtherPlayer(String playerId, float x, float y) {
        Ball ball = otherPlayers.get(playerId);
        if (ball == null) {
            ball = new Ball(x, y, playerId);
            otherPlayers.put(playerId, ball);
        } else {
            ball.x = x;
            ball.y = y;
            ball.lastUpdate = System.currentTimeMillis();
        }

        invalidate();
    }

    public void removeInactivePlayers() {
        long currentTime = System.currentTimeMillis();
        long timeout = 5000;

        otherPlayers.entrySet().removeIf(entry ->
                currentTime - entry.getValue().lastUpdate > timeout);

        invalidate();
    }

    public void setOnBallMoveListener(OnBallMoveListener listener) {
        this.ballMoveListener = listener;
    }

    public float getPlayerX() {
        return playerBallX;
    }

    public float getPlayerY() {
        return playerBallY;
    }
}