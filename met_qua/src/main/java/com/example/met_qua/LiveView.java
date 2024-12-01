package com.example.met_qua;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class LiveView {
    @FXML
    ImageView image_live;
    @FXML
    TextArea txtip;
    @FXML
    TextArea txtport;
    @FXML
    TextArea txtchat;
    @FXML
    Label label;

    private static MulticastSocket socket;
    public static int PORT;
    private static InetAddress address;

    @FXML
    private void showVideo() throws IOException {
        opensever();
        new Thread(()->{
            Robot robot = null;
            try {
                robot = new Robot();
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while(true){

                Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                BufferedImage screenFullImage = robot.createScreenCapture(screenRect);
                File tempFile = new File("screenshot.png");
                try {
                    ImageIO.write(screenFullImage, "png", tempFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Image image = new Image(tempFile.toURI().toString());
                try {
                    ImageIO.write(screenFullImage, "jpeg", baos);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                byte[] frameData = baos.toByteArray();
                try {
                    send(0,1,frameData);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                image_live.setImage(image);
            }
        }).start();
        new Thread(this::nhan).start();
    }
    private void opensever() throws IOException {
        if (socket == null || socket.isClosed()) {
            if (Objects.equals(txtip.getText(), "") || Objects.equals(txtport.getText(), "")) {
                JOptionPane.showMessageDialog(null, "Vui lòng nhập IP và Port.");
                return;
            }
            PORT = Integer.parseInt(txtport.getText());
            socket = new MulticastSocket(PORT);
            address = InetAddress.getByName(txtip.getText());
            socket.joinGroup(address);
        } else {
            System.out.println("Socket đã được tạo và đang kết nối.");
        }
    }
    private void nhan(){
        byte[] buffer = new byte[65500];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        new Thread(()->{
            while (true) {
                try {
                    socket.receive(packet);
                    processPacket(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    private void processPacket(DatagramPacket packet) throws Exception {
        byte[] data = packet.getData();

        int dataType = data[0];

        int sequenceNumber = ((data[1] & 0xFF) << 24) |
                ((data[2] & 0xFF) << 16) |
                ((data[3] & 0xFF) << 8) |
                (data[4] & 0xFF);

        byte[] receivedData = new byte[packet.getLength() - 5];
        System.arraycopy(data, 5, receivedData, 0, receivedData.length);
        handleData(dataType, sequenceNumber, receivedData);
    }

    private void handleData(int dataType, int sequenceNumber, byte[] data) throws Exception {
        if (dataType == 2) {
            String result = new String(data, StandardCharsets.UTF_8);
            Platform.runLater(() -> {
                label.setText(label.getText()+"\n"+result);
            });

        }
    }
    @FXML
    private  void close(){
        socket.close();
    }
    @FXML
    private  void chat() throws Exception {
        byte[] buffer = txtchat.getText().getBytes();
        send(2,0,buffer);
    }
    private void send(int dataType, int sequenceNumber, byte[] data) throws Exception {
        byte[] packet = new byte[1 + 4 + data.length];

        packet[0] = (byte) dataType;

        packet[1] = (byte) (sequenceNumber >> 24);
        packet[2] = (byte) (sequenceNumber >> 16);
        packet[3] = (byte) (sequenceNumber >> 8);
        packet[4] = (byte) (sequenceNumber);

        System.arraycopy(data, 0, packet, 5, data.length);

        DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, address, PORT);
        socket.send(datagramPacket);
    }

}
