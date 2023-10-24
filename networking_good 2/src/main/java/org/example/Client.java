package org.example;

import com.vdurmont.emoji.EmojiParser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


public class Client {
    // Instance variables
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String clientIP;
    private String name;
    private int clientPort;
    private JTextArea messageArea = new JTextArea(20, 40);
    private JTextField inputField = new JTextField(40);
    private DefaultListModel<String> userModel = new DefaultListModel<>();
    private JFrame frame = new JFrame("Chat Client");

    // Constructor
    public Client(String name, String serverIP, int port) {

        this.name = name;

        // Initialize components
        messageArea.setEditable(false);
        messageArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        messageArea.setMargin(new Insets(5, 5, 5, 5));
        inputField.setBorder(new EmptyBorder(5, 0, 0, 0));

        final JList<String> userList = new JList<>(userModel);

        // Create popup menu
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem kickMenuItem = new JMenuItem("Kick");
        JMenuItem informationMenuItem = new JMenuItem("User information");
        popupMenu.add(kickMenuItem);
        popupMenu.add(informationMenuItem);

        // Event listener for private chat menu item
        kickMenuItem.addActionListener(e -> {
            String selectedItem = userList.getSelectedValue();
            output.println("/kick " + selectedItem);
        });
        informationMenuItem.addActionListener(e -> {
            String selectedItem = userList.getSelectedValue();
        
            try (BufferedReader br = new BufferedReader(new FileReader("usernames.txt"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length > 2 && parts[0].trim().equals(selectedItem)) {
                        JFrame userInfoFrame = new JFrame("User Information");
                        GridLayout layout = new GridLayout(3, 1);
                        layout.setHgap(5);
                        JPanel userInfoPanel = new JPanel(layout);
                        userInfoPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
                        userInfoPanel.add(new JLabel("Username:"));
                        userInfoPanel.add(new JLabel(parts[0].trim()));
        
                        userInfoPanel.add(new JLabel("IP Address:"));
                        userInfoPanel.add(new JLabel(parts[1].trim()));
        
                        userInfoPanel.add(new JLabel("Port:"));
                        userInfoPanel.add(new JLabel(parts[2].trim()));
        
                        userInfoFrame.add(userInfoPanel);
                        userInfoFrame.pack();
                        userInfoFrame.setResizable(false);
                        userInfoFrame.setLocationRelativeTo(null);
                        userInfoFrame.setVisible(true);
                        break;
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        // Add mouse listener to JList to show popup menu on right click
        userList.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JList list = (JList) e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    list.setSelectedIndex(index);
                    popupMenu.show(list, e.getX(), e.getY());
                }
            }
        });

        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        messageScrollPane.setBorder(new EmptyBorder(0, 0, 5, 0));

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1));
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        JPanel textPanel = new JPanel(new BorderLayout());
        JButton refreshButton = new JButton("Refresh");

        // Event listener for refresh button
        refreshButton.addActionListener(e -> {
            userModel.clear();
            readUsernamesFromFile();
        });

        inputField.setBorder(BorderFactory.createCompoundBorder(
                inputField.getBorder(),
                BorderFactory.createEmptyBorder(0, 5, 5, 5)));
        JButton sendButton = new JButton("Send");
        sendButton.setBorder(new EmptyBorder(5, 0, 0, 0));

        // Add components to panel
        sidePanel.add(userList, BorderLayout.CENTER);
        buttonPanel.add(refreshButton, BorderLayout.NORTH);
        buttonPanel.add(sendButton, BorderLayout.SOUTH);
        sidePanel.add(buttonPanel, BorderLayout.SOUTH);

        textPanel.add(inputField, BorderLayout.SOUTH);
        textPanel.add(messageScrollPane, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.add(textPanel, BorderLayout.CENTER);
        mainPanel.add(sidePanel, BorderLayout.EAST);

        frame.add(mainPanel);

        // Connect to server and send user's name
        try {
            socket = new Socket(serverIP, port);
            clientIP = socket.getInetAddress().getHostAddress(); // update client IP address
            clientPort = socket.getPort(); // update client port number
            saveUsernameToFile();
            readUsernamesFromFile();
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            // Prompt user for name
            output.println(name);
            frame.setTitle(name);

            // Receive welcome message from server
            String serverMessage = input.readLine();
            messageArea.append(serverMessage + "\n");

            // Start message receiving thread
            Thread messageThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = input.readLine()) != null) {
                        if (URLDecoder.decode(message, StandardCharsets.UTF_8.toString()).equals("You have been kicked from the server")) {
                            JOptionPane.showMessageDialog(frame, "You have been kicked from the server.", "Info", JOptionPane.INFORMATION_MESSAGE);
                            frame.dispose();
                            break;
                        } else {
                            messageArea.append(URLDecoder.decode(message, StandardCharsets.UTF_8.toString()) + "\n");
                        }
                    }
                } catch (IOException e) {
                    try {
                        removeAllUsername();
                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    }
                    JOptionPane.showMessageDialog(frame, "You have been disconnected from the server.", "Error", JOptionPane.ERROR_MESSAGE);
                    frame.dispose();
                }
            });
            messageThread.start();
        } catch (IOException e) {
            removeUsername(name);
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error connecting to server", "Error", JOptionPane.ERROR_MESSAGE);
            frame.dispose();
            System.exit(1);
        }

        // Add event listener for input field
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                removeUsername(name);
            }
        });

        // Event listeners for send button and input field
        inputField.addActionListener(e -> sendMessage());
        sendButton.addActionListener(e -> sendMessage());

        // Show the window
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Sends the message in the input field to the server
    private void sendMessage() {
        String inputLine = inputField.getText();
        inputLine = EmojiParser.parseToUnicode(inputLine);

        if (inputLine.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Message cannot be empty");
            return;
        }

        output.println(inputLine);
        output.flush();
        if (inputLine.equals("quit")) {
            System.exit(0);
        }
        inputField.setText("");
    }

    // Reads usernames from file and displays them in list
    public void readUsernamesFromFile() {
        try (BufferedReader br = new BufferedReader(new FileReader("usernames.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 0) {
                    userModel.addElement(parts[0].trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Removes all usernames from the username file
    private void removeAllUsername() throws FileNotFoundException {
        PrintWriter writer = new PrintWriter("usernames.txt");
        writer.print("");
        writer.close();
    }

    public void saveUsernameToFile() {
        try {
            String usernameToSave = name + "," + clientIP + "," + clientPort;
            File file = new File("usernames.txt");
            FileWriter writer = new FileWriter(file, true);
            writer.write(usernameToSave + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Removes a single username from the username file
    private boolean findUsername(String username) {
        try (BufferedReader br = new BufferedReader(new FileReader("usernames.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 0 && parts[0].trim().equals(username)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void removeUsername(String username) {
        try {
            File file = new File("usernames.txt");
            File temp = new File("temp.txt");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            BufferedWriter writer = new BufferedWriter(new FileWriter(temp));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (!parts[0].trim().equals(username)) {
                    writer.write(line + "\n");
                }
            }
            writer.close();
            reader.close();
            file.delete();
            temp.renameTo(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}