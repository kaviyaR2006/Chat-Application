package chatapp; 
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import javax.swing.*; 
public class Client { 
private static final String SERVER_ADDRESS = "localhost"; 
private static final int SERVER_PORT = 5050; 
private Socket socket; 
private BufferedReader in; 
private PrintWriter out; 
private DataOutputStream dataOut; 
private JFrame frame = new JFrame(); 
private JTextArea chatArea = new JTextArea(20, 50); 
private JTextField inputField = new JTextField(35); 
private JButton sendButton = new JButton("Send"); 
private JButton sendFileButton = new JButton("Send File"); 
private JButton searchButton = new JButton("Search"); 
private JButton analyticsButton = new JButton("View Analytics"); 
private JButton groupButton = new JButton("Group Chat"); 
private String username; 
public Client() { 
username = JOptionPane.showInputDialog(frame, "Enter your username:", "Login", JOptionPane.QUESTION_MESSAGE); 
if (username == null || username.isEmpty()) username = "User" + (int)(Math.random() * 1000); 
chatArea.setEditable(false); 
chatArea.setLineWrap(true); 
chatArea.setWrapStyleWord(true); 
frame.setTitle("Chat Client - " + username); 
// ==== BOTTOM CONTROL PANEL ==== 
JPanel bottomPanel = new JPanel(new BorderLayout(8, 8)); 
bottomPanel.add(inputField, BorderLayout.CENTER);
 // All buttons grouped at bottom-right corner (horizontal layout) 
        JPanel buttonPanel = new JPanel(new 
FlowLayout(FlowLayout.RIGHT, 8, 8)); 
        buttonPanel.add(sendButton); 
        buttonPanel.add(sendFileButton); 
        buttonPanel.add(searchButton); 
        buttonPanel.add(groupButton); 
        buttonPanel.add(analyticsButton); 
 
        bottomPanel.add(buttonPanel, BorderLayout.EAST); 
 
        // ==== MAIN FRAME LAYOUT ==== 
        frame.getContentPane().setLayout(new BorderLayout(10, 10)); 
        frame.getContentPane().add(new JScrollPane(chatArea), 
BorderLayout.CENTER); 
        frame.getContentPane().add(bottomPanel, 
BorderLayout.SOUTH); 
 
        frame.pack(); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        frame.setLocationRelativeTo(null); 
        frame.setVisible(true); 
 
        // ==== BUTTON ACTIONS ==== 
        sendButton.addActionListener(e -> sendMessage()); 
        inputField.addActionListener(e -> sendMessage()); 
        sendFileButton.addActionListener(e -> sendFile()); 
        searchButton.addActionListener(e -> searchMessages()); 
        analyticsButton.addActionListener(e -> out.println("/analytics")); 
        groupButton.addActionListener(e -> showGroupMenu()); 
    } 
 
          
            // ===== FEATURES ===== 
    private void sendMessage() { 
        String message = inputField.getText().trim(); 
        if (!message.isEmpty()) { 
            out.println(message); 
            inputField.setText(""); 
        } 
    } 
 
    private void searchMessages() { 
        String keyword = JOptionPane.showInputDialog(frame, "Enter word to search:");
        if (keyword != null && !keyword.isEmpty()) { 
out.println("/search " + keyword); 
} 
} 
private void showGroupMenu() { 
String[] options = {"Create Group", "Join Group", "Send Group Message"}; 
int choice = JOptionPane.showOptionDialog(frame, "Choose an action", "Group Chat", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]); 
switch (choice) { 
case 0 -> { 
String name = JOptionPane.showInputDialog(frame, "Enter new group name:"); 
if (name != null) out.println("/create " + name); 
} 
case 1 -> { 
String name = JOptionPane.showInputDialog(frame, "Enter group name to join:"); 
if (name != null) out.println("/join " + name); 
} 
case 2 -> { 
String name = JOptionPane.showInputDialog(frame, "Enter group name:"); 
String msg = JOptionPane.showInputDialog(frame, "Enter message:"); 
if (name != null && msg != null) out.println("/group " + name + " " + msg); 
} 
} 
} 
private void sendFile() { 
JFileChooser chooser = new JFileChooser(); 
int option = chooser.showOpenDialog(frame); 
if (option == JFileChooser.APPROVE_OPTION) { 
File file = chooser.getSelectedFile(); 
new Thread(() -> { 
try { 
out.println("[FILE]" + file.getName()); 
 byte[] fileData = Files.readAllBytes(file.toPath()); 
                    dataOut.writeInt(fileData.length); 
                    dataOut.write(fileData); 
                    dataOut.flush(); 
                    chatArea.append("📤 Sent file: " + file.getName() + "\n"); 
                } catch (IOException ex) { 
                    chatArea.append("❌ Error sending file: " + 
ex.getMessage() + "\n"); 
                } 
            }).start(); 
        } 
    } 
 
    public void start() { 
        try { 
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT); 
            in = new BufferedReader(new 
InputStreamReader(socket.getInputStream())); 
            out = new PrintWriter(socket.getOutputStream(), true); 
            dataOut = new DataOutputStream(socket.getOutputStream()); 
 
            out.println(username); 
 
            new Thread(() -> { 
                try { 
                    String msg; 
                    while ((msg = in.readLine()) != null) { 
                        chatArea.append(msg + "\n"); 
                    } 
                } catch (IOException e) { 
                    chatArea.append("Connection closed.\n"); 
                } 
            }).start(); 
 
        } catch (IOException e) { 
            JOptionPane.showMessageDialog(frame, "Unable to connect to server!\n" + e.getMessage()); 
        } 
    } 
 
    public static void main(String[] args) { 
        Client client = new Client(); 
        client.start(); 
    }}