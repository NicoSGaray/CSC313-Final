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
            Socket socket = new Socket(serverIp, 4444);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Hello from client!");

            String msgFromServer;
            while ((msgFromServer = in.readLine()) != null) {
                System.out.println("[CLIENT] Server says: " + msgFromServer);
                if (msgFromServer.equals("START_GAME")) {
                    mainRef.setGameState(GameState.PLAYING);
                    break;
                }
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
