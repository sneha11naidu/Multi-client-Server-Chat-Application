
package org.example;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String FILENAME = "usernames.txt";

    public static void main(String[] args) {
        List<String> usernames = readUsernamesFromFile();
        String name, serverIP;
        int port;

        do {
            JTextField nameField = new JTextField();
            JTextField portField = new JTextField();
            JTextField serverIPField = new JTextField();

            Object[] fields = {"Name:", nameField, "Port:", portField, "Server IP:", serverIPField};
            int option = JOptionPane.showConfirmDialog(null, fields, "Enter your information", JOptionPane.OK_CANCEL_OPTION);

            if (option != JOptionPane.OK_OPTION) {
                System.exit(0);
            }

            name = nameField.getText();
            port = Integer.parseInt(portField.getText());
            serverIP = serverIPField.getText();

            if (usernames.contains(name)) {
                JOptionPane.showMessageDialog(null, "Username already exists, please choose a unique username.");
            }
        } while (usernames.contains(name) || name.trim().isEmpty() || serverIP.trim().isEmpty() || port == 0);

//        writeUsernameToFile(name);
        Client client = new Client(name, serverIP, port);
    }

    private static List<String> readUsernamesFromFile() {
        List<String> usernames = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FILENAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if(parts.length > 0) {
                    usernames.add(parts[0].trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return usernames;
    }



}