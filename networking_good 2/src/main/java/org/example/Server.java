package org.example;

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class Server {

    private static final String ACTIVE_USERS_CMD = "/activeusers";
    private static final String KICK_CMD = "/kick";
    private static final String PRIVATE_MSG_CMD = "@";
    private static List<ClientHandler> clients;
    private static ServerGUI serverGUI;
    private static volatile boolean stopServer = false;
    


    public Server(int port) throws IOException {
        PrintWriter writer = new PrintWriter("usernames.txt");
        writer.print("");
        writer.close();
        clients = new ArrayList<>();
        ServerSocket serverSocket = new ServerSocket(port);
        ServerGUI serverGUI = new ServerGUI();
        main();
        while (!stopServer) {
            Socket socket = serverSocket.accept();
            serverGUI.appendServerInfo("New client connected");
            ClientHandler clientHandler = new ClientHandler(socket);
            clients.add(clientHandler);
            clientHandler.start();
        }
        serverSocket.close();
    }

    public static void stopServer() {
        stopServer = true;
        serverGUI.appendServerInfo("Server stopped");
    }

    public static void main() {

    }

    public static void broadcast(String message, ClientHandler sender) {
        if (sender == null) {
            serverGUI.appendServerInfo("Error: Sender is null");
            return;
        }

        String decodedMessage;
        try {
            decodedMessage = URLDecoder.decode(message, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            decodedMessage = message;
        }

        switch (decodedMessage) {
            case ACTIVE_USERS_CMD -> {
                StringBuilder activeUsers = new StringBuilder();
                for (ClientHandler client : clients) {
                    activeUsers.append(client.getClientName()).append("\n");
                }
                sender.sendMessage(activeUsers.toString());
            }
            case KICK_CMD -> sender.sendMessage("You must specify a user to kick");
            default -> {
                if (decodedMessage.startsWith(PRIVATE_MSG_CMD)) {
                    String[] parts = decodedMessage.split(" ", 2);
                    if (parts.length < 2) {
                        sender.sendMessage("Invalid private message format");
                        return;
                    }
                    String recipientName = parts[0].substring(1);
                    String privateMessage = parts[1];
                    sendPrivateMessage(privateMessage, sender, recipientName);
                } else {
                    for (ClientHandler client : clients) {
                        client.sendMessage(decodedMessage);
                    }
                }
            }
        }
    }

    public static void sendPrivateMessage(String message, ClientHandler sender, String recipientName) {
        for (ClientHandler client : clients) {
            if (client != sender && client.getClientName().equals(recipientName)) {
                client.sendMessage("Private message from " + sender.getClientName() + ": " + message);
                sender.sendMessage("Private message to " + recipientName + ": " + message);
                return;
            }
        }
        sender.sendMessage("Recipient not found");
    }

    public static List<ClientHandler> getClients() {
        return clients;
    }

    private static class ClientHandler extends Thread {
        private final String name;
        private final Socket socket;
        private final BufferedReader input;
        private final PrintWriter output;

        public ClientHandler(Socket socket) throws IOException {

            this.socket = socket;
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            name = input.readLine();
            output.println("Welcome " + name);
            for (ClientHandler client : clients) {
                client.sendMessage(name + " has joined the chat!");
            }
        }

        private void removeUsernameFromFile() {
            try {
                List<String> lines = Files.readAllLines(Paths.get("usernames.txt"), StandardCharsets.UTF_8);
                List<String> updatedLines = new ArrayList<>();
                for (String line : lines) {
                    String[] parts = line.split(",");
                    if (!parts[0].equals(name)) {
                        updatedLines.add(line);
                    }
                }
                Files.write(Paths.get("usernames.txt"), updatedLines, StandardCharsets.UTF_8);
            } catch (IOException e) {
                serverGUI.appendServerInfo("Error removing username from file: " + e.getMessage());
            }
        }

        private void kickUser(String username, String kickerName) {
            String admin = null;
            try (BufferedReader br = new BufferedReader(new FileReader("usernames.txt"))) {
                String line;
                line = br.readLine();
                String[] parts = line.split(",");
                if (parts.length > 0) {
                    admin = parts[0].trim();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!kickerName.equals(admin)) {

                sendMessage("You are not authorized to kick users");
                return;
            }

            if (username==admin) {
                JOptionPane.showMessageDialog(null, username);
                sendMessage("Cannot kick user with reserved username");
                return;
            }

            Optional<ClientHandler> optionalClientHandler = Server.getClients().stream()
                    .filter(((Function<ClientHandler, Boolean>) c -> c.getClientName().equals(username))::apply)
                    .findFirst();

            if (optionalClientHandler.isPresent()) {
                ClientHandler clientToKick = optionalClientHandler.get();

                clientToKick.sendMessage("You have been kicked from the server");

                clientToKick.disconnect();
                sendMessage(username + " has been kicked from the server");
            } else {
                sendMessage("User " + username + " not found");
            }
        }

        public void disconnect() {
            try {
                socket.close();
            } catch (IOException e) {
                serverGUI.appendServerInfo("Error closing socket");
            }
            removeUsernameFromFile();
            clients.remove(this);
//
        }

        public String getClientName() {
            return name;
        }

        public void sendMessage(String message) {
            try {
                output.println(URLEncoder.encode(message, StandardCharsets.UTF_8.toString()));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        //        @Override
        public void run() {
            String inputLine;
            try {
                while ((inputLine = input.readLine()) != null) {
                    if (inputLine.startsWith("/kick")) {
                        String[] parts = inputLine.split(" ");
                        if (parts.length < 1) {
                            sendMessage("Invalid kick command format. Usage: /kick username");
                        } else {
                            String usernameToKick = parts[1];
                            kickUser(usernameToKick, getClientName());
                        }
                    } else if (inputLine.startsWith("/")) {
                        if (inputLine.equals(ACTIVE_USERS_CMD)) {
                            StringBuilder builder = new StringBuilder("Active users:");
                            for (ClientHandler client : clients) {
                                builder.append(" ").append(client.getClientName());
                            }
                            sendMessage(builder.toString());
                        }
                    } else if (inputLine.equals("quit")) {
                        break;
                    } else if (inputLine.startsWith(PRIVATE_MSG_CMD)) {
                        String[] parts = inputLine.split(" ", 2);
                        if (parts.length < 2) {
                            sendMessage("Invalid private message format");
                        } else {
                            String recipientName = parts[0].substring(1);
                            String message = parts[1];
                            Server.sendPrivateMessage(message, this, recipientName);
                        }
                    } else {
                        broadcast(name + ": " + inputLine, this);
                    }
                }
            } catch (IOException e) {
                serverGUI.appendServerInfo("Error handling client");
                removeUsernameFromFile();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    serverGUI.appendServerInfo("Error closing socket");
                }
                clients.remove(this);
                for (ClientHandler client : clients) {
                    client.sendMessage(name + " has left the chat!");
                }
            }
        }
    }

}