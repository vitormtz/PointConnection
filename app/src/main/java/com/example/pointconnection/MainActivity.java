package com.example.pointconnection;

import android.os.Bundle;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private GameView gameView;
    private UDPClient udpClient;
    private static final String SERVER_IP = "136.248.120.193";
    private static final int SERVER_PORT = 9999;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        gameView = new GameView(this);
        setContentView(gameView);

        udpClient = new UDPClient(SERVER_IP, SERVER_PORT, gameView);

        gameView.setOnBallMoveListener(new GameView.OnBallMoveListener() {
            @Override
            public void onBallMove(float x, float y) {
                sendPositionToServer(x, y);
            }
        });
        udpClient.startConnection();
    }

    private void sendPositionToServer(float x, float y) {
        if (udpClient != null) {
            udpClient.sendPosition(x, y);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (udpClient != null) {
            udpClient.startConnection();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (udpClient != null) {
            udpClient.stopConnection();
        }
    }
}