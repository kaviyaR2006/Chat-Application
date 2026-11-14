 
package chatapp; 
 
import java.io.*;
import java.net.*; 
import java.util.*; 
 
public class Server { 
    private static final int PORT = 5050; 
 
    // Data Structures 
    private static final Map<String, PrintWriter> clientWriters = Collections.synchronizedMap(new 
HashMap<>()); 
    private static final Map<String, List<String>> chatHistory = Collections.synchronizedMap(new 
HashMap<>()); 
    private static final Map<String, List<String>> groupChats = Collections.synchronizedMap(new 
HashMap<>()); 
 
    private static int totalMessages = 0; 
    private static int totalFiles = 0; 
 
    public static void main(String[] args) { 
        System.out.println("✅ Chat Server started on port " + PORT); 
        try (ServerSocket serverSocket = new ServerSocket(PORT)) { 
            while (true) { 
                Socket socket = serverSocket.accept(); 
                new ClientHandler(socket).start(); 
            } 
        } catch (IOException e) { 
            e.printStackTrace(); 
        } 
    } 
 
    private static class ClientHandler extends Thread { 
        private Socket socket; 
        private String username; 
        private BufferedReader in; 
        private PrintWriter out; 
        private DataInputStream dataIn; 
 
        public ClientHandler(Socket socket) { 
            this.socket = socket; 
        } 
 
        public void run() { 
            try { 
                in = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
                out = new PrintWriter(socket.getOutputStream(), true); 
                dataIn = new DataInputStream(socket.getInputStream());
                // USER MANAGEMENT MODULE 
                out.println("Enter your username:"); 
                username = in.readLine(); 
 
                synchronized (clientWriters) { 
                    if (clientWriters.containsKey(username)) { 
                        out.println("Username already taken! Disconnecting..."); 
                        socket.close(); 
                        return; 
                    } 
                    clientWriters.put(username, out); 
                    chatHistory.put(username, new ArrayList<>()); 
                } 
 
                broadcast("SERVER: " + username + " joined the chat."); 
                showActiveUsers(); 
 
                // MESSAGING, GROUP, SEARCH, ANALYTICS, FILE MODULES 
                String message; 
                while ((message = in.readLine()) != null) { 
                    if (message.equalsIgnoreCase("exit")) break; 
                    else if (message.startsWith("/create ")) handleGroupCreate(message); 
                    else if (message.startsWith("/join ")) handleGroupJoin(message); 
                    else if (message.startsWith("/group ")) handleGroupMessage(message); 
                    else if (message.startsWith("/search ")) handleSearch(message); 
                    else if (message.equalsIgnoreCase("/analytics")) handleAnalytics(); 
                    else if (message.startsWith("[FILE]")) receiveFile(message.substring(6)); 
                    else handleMessage(message); 
                } 
            } catch (IOException e) { 
                System.out.println("❌ Client disconnected: " + username); 
            } finally { 
                try { socket.close(); } catch (IOException ignored) {} 
                synchronized (clientWriters) { 
                    clientWriters.remove(username); 
                } 
                broadcast("SERVER: " + username + " left the chat."); 
                showActiveUsers(); 
            } 
        } 
 
        // ===== MODULE FUNCTIONS ===== 
 
        // MESSAGING MODULE 
        private void handleMessage(String msg) { 
            totalMessages++; 
            chatHistory.get(username).add(msg); 
            broadcast(username + ": " + msg); 
        } 
 
        // GROUP CHAT MODULE
        private void handleGroupCreate(String cmd) { 
            String groupName = cmd.substring(8); 
            groupChats.putIfAbsent(groupName, new ArrayList<>()); 
            groupChats.get(groupName).add(username); 
            out.println("✅ Group created: " + groupName); 
        } 
 
        private void handleGroupJoin(String cmd) { 
            String groupName = cmd.substring(6); 
            groupChats.computeIfAbsent(groupName, k -> new ArrayList<>()).add(username); 
            out.println("✅ Joined group: " + groupName); 
        } 
 
        private void handleGroupMessage(String cmd) { 
            String[] parts = cmd.split(" ", 3); 
            if (parts.length < 3) { 
                out.println("Usage: /group <groupname> <message>"); 
                return; 
            } 
            String groupName = parts[1]; 
            String groupMsg = parts[2]; 
            if (groupChats.containsKey(groupName)) { 
                for (String member : groupChats.get(groupName)) { 
                    PrintWriter writer = clientWriters.get(member); 
                    if (writer != null) writer.println("[Group " + groupName + "] " + username + ": " + groupMsg); 
                } 
            } else { 
                out.println("❌ Group not found: " + groupName); 
            } 
        } 
 
        // FILE SHARING MODULE 
        private void receiveFile(String fileName) { 
            try { 
                int length = dataIn.readInt(); 
                byte[] fileData = new byte[length]; 
                dataIn.readFully(fileData); 
 
                File received = new File("received_" + fileName); 
                try (FileOutputStream fos = new FileOutputStream(received)) { 
                    fos.write(fileData); 
                } 
 
                totalFiles++; 
                broadcast("📥 Received file from " + username + ": " + fileName); 
            } catch (IOException e) { 
                out.println("❌ Error receiving file: " + e.getMessage()); 
            } 
        } 
 
        // SEARCH MODULE  
 
 
        private void handleSearch(String cmd) { 
            String query = cmd.substring(8).trim(); 
            List<String> history = chatHistory.get(username); 
            if (history == null || history.isEmpty()) { 
                out.println("No chat history available."); 
                return; 
            } 
            boolean found = false; 
            for (String msg : history) { 
                if (msg.toLowerCase().contains(query.toLowerCase())) { 
                    out.println("🔍 Found: " + msg); 
                    found = true; 
                } 
            } 
            if (!found) out.println("No messages found for: " + query); 
        } 
 
        // ANALYTICS MODULE 
        private void handleAnalytics() { 
            out.println("📊 Analytics Report:"); 
            out.println("Total Messages Sent: " + totalMessages); 
            out.println("Total Files Shared: " + totalFiles); 
            out.println("Total Active Users: " + clientWriters.size()); 
        } 
 
        // UTILITY FUNCTIONS 
        private void broadcast(String msg) { 
            synchronized (clientWriters) { 
                for (PrintWriter writer : clientWriters.values()) { 
                    writer.println(msg); 
                } 
            } 
        } 
 
        private void showActiveUsers() { 
            StringBuilder sb = new StringBuilder("👥 Active Users: "); 
            synchronized (clientWriters) { 
                for (String name : clientWriters.keySet()) { 
                    sb.append(name).append(", "); 
                } 
            } 
            broadcast(sb.toString()); 
        } 
    } 
}
