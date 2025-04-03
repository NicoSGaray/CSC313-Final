import java.io.*;
import java.net.*;
import java.util.List;

public class ServerThread extends Thread {
    private ServerSocket serverSocket;
    private List<String> playerList;
    private boolean running = true;    

    public ServerThread(List<String> playerList) {
        this.playerList = playerList;
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(4444);
            System.out.println("[SERVER] Waiting for client(s) on port 4444...");

            while (running) {
                Socket clientSocket = serverSocket.accept();
                String clientIp = clientSocket.getInetAddress().getHostAddress();
                System.out.println("[SERVER] Client connected: " + clientIp);

                // Add to player list if not already there
                if (!playerList.contains(clientIp)) {
                    playerList.add(clientIp);
                }

                // Basic communication
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                out.println("Welcome to the lobby!");
                in.readLine(); // read whatever client sends

                clientSocket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    
}