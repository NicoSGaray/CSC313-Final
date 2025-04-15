import java.io.*;
import java.net.*;

public class GameClient extends Thread {
    private String serverIp;
    private Main mainRef;
    private int playerIndex = -1;
    private PrintWriter out;
    private BufferedReader in;

    public GameClient(String serverIp, Main mainRef) {
        this.serverIp = serverIp;
        this.mainRef = mainRef;
    }

    public void run() {
        System.out.println("[CLIENT] Connecting to server...");
        try {
            Socket socket = new Socket(serverIp, 4444);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.startsWith("ASSIGN_INDEX:")) {
                    playerIndex = Integer.parseInt(msg.split(":")[1]);
                    mainRef.setCurrentCarIndex(playerIndex);
                    System.out.println("[CLIENT] Assigned car index: " + playerIndex);
                } else if (msg.startsWith("UPDATE:")) {
                    String[] parts = msg.split(":");
                    int index = Integer.parseInt(parts[1]);
                    float x = Float.parseFloat(parts[2]);
                    float y = Float.parseFloat(parts[3]);
                    float z = Float.parseFloat(parts[4]);
                    float angle = Float.parseFloat(parts[5]);
                    mainRef.updateRemoteCar(index, x, y, z, angle);
                } else if (msg.equals("START_GAME")) {
                    System.out.println("[CLIENT] Received START_GAME");
                    mainRef.setGameState(GameState.PLAYING);
                } else if (msg.startsWith("SHOVE:")) {
                    int sourceIndex = Integer.parseInt(msg.split(":")[1]);
                    mainRef.performShoveFromRemote(sourceIndex);
                } else if (msg.startsWith("ENEMY_SHOVE:")) {
                    String[] parts = msg.split(":");
                    int targetIndex = Integer.parseInt(parts[1]);
                    float shoveX = Float.parseFloat(parts[2]);
                    float shoveZ = Float.parseFloat(parts[3]);
                    mainRef.applyShoveToCar(targetIndex, shoveX, shoveZ);
                }                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendCustomMessage(String msg) {
        if (out != null) {
            out.println(msg);
            out.flush();
        }
    }    

    public void sendUpdate(float x, float y, float z, float angle) {
        if (out != null) {
            String msg = "UPDATE:" + playerIndex + ":" + x + ":" + y + ":" + z + ":" + angle;
            out.println(msg);
            out.flush();
        }
    }

    public int getPlayerIndex() {
        return playerIndex;
    }
}