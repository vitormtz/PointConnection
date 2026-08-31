package com.example.pointconnection;

import android.os.Handler;
import android.os.Looper;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPClient {
    private final String serverIP;
    private final int serverPort;
    private DatagramSocket socket;
    private boolean isRunning = false;
    private final String playerId;
    private ExecutorService executor;
    private Handler mainHandler;
    private GameView gameView;
    private Map<String, Long> knownPlayers = new HashMap<>();
    private long lastBroadcast = 0;
    private static final long BROADCAST_INTERVAL = 1000;

    public UDPClient(String serverIP, int serverPort, GameView gameView) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.gameView = gameView;
        this.playerId = UUID.randomUUID().toString();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newFixedThreadPool(3);
    }

    public void startConnection() {
        if (isRunning) {
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new DatagramSocket();
                    isRunning = true;

                    sendDiscoveryMessage();
                    startListening();
                    startPeriodicBroadcast();

                } catch (Exception e) {
                    e.printStackTrace();
                    isRunning = false;
                }
            }
        });
    }

    private void sendDiscoveryMessage() {
        sendMessage("DISCOVER:" + playerId + ":0:0");
    }

    private void startPeriodicBroadcast() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    try {
                        Thread.sleep(BROADCAST_INTERVAL);

                        long currentTime = System.currentTimeMillis();

                        if (currentTime - lastBroadcast > BROADCAST_INTERVAL) {
                            if (gameView != null) {
                                sendMessage("HEARTBEAT:" + playerId + ":" +
                                        gameView.getPlayerX() + ":" +
                                        gameView.getPlayerY());
                            }
                            lastBroadcast = currentTime;
                        }

                        cleanupInactivePlayers();

                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
    }

    private void cleanupInactivePlayers() {
        long currentTime = System.currentTimeMillis();
        long timeout = 5000;

        knownPlayers.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > timeout);

        if (gameView != null) {
            mainHandler.post(() -> gameView.removeInactivePlayers());
        }
    }

    public void sendPosition(float x, float y) {
        if (!isRunning || socket == null) {
            return;
        }

        sendMessage("POSITION:" + playerId + ":" + x + ":" + y);
    }

    private void sendMessage(String message) {
        if (!isRunning || socket == null) {
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] data = message.getBytes();

                    InetAddress serverAddress = InetAddress.getByName(serverIP);
                    DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);

                    socket.send(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void startListening() {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];

                while (isRunning && socket != null && !socket.isClosed()) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);

                        String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                        String senderIP = packet.getAddress().getHostAddress();

                        processReceivedMessage(receivedMessage, senderIP);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void processReceivedMessage(String message, String senderIP) {
        try {
            String[] parts = message.split(":");

            if (parts.length < 2) {
                return;
            }

            String messageType = parts[0];
            String senderId = parts[1];

            if (senderId.equals(playerId)) {
                return;
            }

            knownPlayers.put(senderId, System.currentTimeMillis());

            switch (messageType) {
                case "DISCOVER":
                    if (gameView != null) {
                        sendMessage("RESPONSE:" + playerId + ":" +
                                gameView.getPlayerX() + ":" +
                                gameView.getPlayerY());
                    }
                    break;

                case "RESPONSE":
                case "POSITION":
                case "HEARTBEAT":
                    if (parts.length >= 4) {
                        try {
                            float x = Float.parseFloat(parts[2]);
                            float y = Float.parseFloat(parts[3]);

                            mainHandler.post(() -> {
                                if (gameView != null) {
                                    gameView.updateOtherPlayer(senderId, x, y);
                                }
                            });

                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopConnection() {
        isRunning = false;

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}