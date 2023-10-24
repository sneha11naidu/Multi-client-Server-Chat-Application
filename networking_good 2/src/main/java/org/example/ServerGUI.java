package org.example;

import javax.swing.*;
import java.awt.*;

public class ServerGUI extends JFrame {

    private JLabel serverInfoLabel;
    private JButton stopButton;
    private JTextArea serverTextArea;

    public ServerGUI() {
        setTitle("Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);

        serverTextArea = new JTextArea();
        serverTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(serverTextArea);
        add(scrollPane, BorderLayout.CENTER);

        serverInfoLabel = new JLabel("Server is running...");
        serverInfoLabel.setHorizontalAlignment(JLabel.CENTER);
        add(serverInfoLabel, BorderLayout.NORTH);

        stopButton = new JButton("Stop Server");
        stopButton.addActionListener(e -> stopServer());

        add(stopButton, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void stopServer() {
        try {
//            Server.stopServer();
            dispose();
            System.exit(0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void appendServerInfo(String info) {
        serverTextArea.append(info + "\n");
    }
}