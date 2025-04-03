import java.io.*;
import java.net.*;

public class ClientThread extends Thread {
    private String serverIp;

    public ClientThread(String ip) {
        this.serverIp = ip;
    }

    public void run() {
        try {
            System.out.println("[CLIENT] Connecting to server at " + serverIp + ":4444...");
            Socket socket = new Socket(serverIp, 4444);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String msgFromServer = in.readLine();
            System.out.println("[CLIENT] Server says: " + msgFromServer);

            out.println("Hello from client!");

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
