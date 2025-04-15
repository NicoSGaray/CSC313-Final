import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {
    private static final int PORT = 4444;
    private static Map<Integer, PlayerHandler> players = new ConcurrentHashMap<>();
    private static int playerCounter = 0;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("[SERVER] Game Server running on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            PlayerHandler handler = new PlayerHandler(clientSocket, playerCounter);
            players.put(playerCounter, handler);
            new Thread(handler).start();
            System.out.println("[SERVER] Player " + playerCounter + " connected.");
            playerCounter++;
        }
    }

    public static void broadcast(String message, int senderIndex) {
        for (Map.Entry<Integer, PlayerHandler> entry : players.entrySet()) {
            entry.getValue().send(message); // ✅ no senderIndex check
        }
    } 
    
    public static void broadcast(String message) {
        for (PlayerHandler handler : players.values()) {
            handler.send(message);
        }
    }    
}

class PlayerHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private int playerIndex;

    public PlayerHandler(Socket socket, int index) {
        this.socket = socket;
        this.playerIndex = index;
        System.out.println("[SERVER] PlayerHandler created for index " + playerIndex);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("ASSIGN_INDEX:" + playerIndex);
            System.out.println("[SERVER] Assigned index " + playerIndex);

            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.startsWith("UPDATE:")) {
                    GameServer.broadcast(msg, playerIndex);
                } else if (msg.equals("START_GAME")) {
                    GameServer.broadcast("START_GAME", playerIndex);
                } else if (msg.startsWith("SHOVE:")) {
                    GameServer.broadcast(msg, playerIndex);
                }                
            }            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public void send(String msg) {
        out.println(msg);
        out.flush();
    }
}
