package org.example;

import javax.swing.*;
import java.io.IOException;

public class ServerMain {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Server");

        frame.add(createServerPanel(frame));

        frame.setSize(300, 100);
        frame.setVisible(true);
    }

    private static JPanel createServerPanel(JFrame frame) {
        final var panel = new JPanel();
        final var label = new JLabel("Enter port number:");
        final var portSpinner = new JSpinner(new SpinnerNumberModel(8080, 0, 65535, 1));
        final var startBtn = new JButton("Start server");
        panel.add(label);
        panel.add(portSpinner);
        panel.add(startBtn);
        startBtn.addActionListener(e -> {
            int port = (int) portSpinner.getValue();
            startServer(port);
            frame.dispose();
        });
        return panel;
    }

    private static void startServer(final int port) {
        final var serverThread =
                new Thread(() -> {
                    try {
                        final var server = new Server(port);
                    } catch (final IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
        serverThread.start();
    }

}