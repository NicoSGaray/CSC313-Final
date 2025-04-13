import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.InputStreamReader;

public class ClientThread extends Thread {
    private String serverIp;
    private Main mainRef;

    public ClientThread(String ip, Main mainRef) {
        this.serverIp = ip;
        this.mainRef = mainRef;
    }

    public void run() {
        try {
            System.out.println("[CLIENT] Attempting to connect to server at IP: " + serverIp + "...");
            Socket socket = new Socket(serverIp, 4444);
            System.out.println("[CLIENT] Connected to server.");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Hello from client!");
            System.out.println("[CLIENT] Sent greeting to server.");

            System.out.println("[CLIENT] Waiting for messages from server...");

            String msgFromServer;
            while ((msgFromServer = in.readLine()) != null) {
                System.out.println("[CLIENT] Server says: " + msgFromServer);

                if (msgFromServer.equals("START_GAME")) {
                    System.out.println("[CLIENT] Received START_GAME. Switching to PLAYING state.");
                    mainRef.setGameState(GameState.PLAYING);
                    break;
                }
            }

            System.out.println("[CLIENT] START_GAME received. Letting main loop take over.");
        } catch (IOException e) {
            System.err.println("[CLIENT] Connection error:");
            e.printStackTrace();
        }
    }
}