import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ServerThread extends Thread {
    private ServerSocket serverSocket;
    private List<String> playerList;
    private boolean running = true;
    private List<PrintWriter> clientWriters = new ArrayList<>();

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
            
                if (!playerList.contains(clientIp)) {
                    playerList.add(clientIp);
                }
            
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            
                out.println("Welcome to the lobby!");
            
                // OPTIONAL: If you want to keep the connection alive
                new Thread(() -> {
                    try {
                        String line;
                        while ((line = in.readLine()) != null) {
                            System.out.println("[SERVER] Received from client: " + line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            
                clientWriters.add(out);
            }            

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastMessage(String message) {
        for (PrintWriter writer : clientWriters) {
            writer.println(message);
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