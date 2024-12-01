package com.example.met_qua;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;

public class XemliveView {
    @FXML
    private  ImageView imageLive;
    @FXML
    private TextArea txtIp;
    @FXML
    private  TextArea txtPort;
    @FXML
    private Label label;
    @FXML
    private  TextArea txtchat;

    private static MulticastSocket socket;
    private static InetAddress address;
    private static int PORT;

    private void connect(String ip,String port){
        if (socket == null || socket.isClosed()) {
            try {
                PORT = Integer.parseInt(port);
                socket = new MulticastSocket(PORT);
                address = InetAddress.getByName(ip);
                socket.joinGroup(address);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println("Socket đã được tạo và đang kết nối.");
        }
    }
    @FXML
    private  void close(){
        socket.close();
    }
    @FXML
    private  void chat() throws Exception {
        byte[] buffer = txtchat.getText().getBytes();
       // label.setText(label.getText()+"\n"+txtchat.getText());
        send(2,0,buffer);
    }
    @FXML
    private void xemlive(){
        connect(txtIp.getText(),txtPort.getText());
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
        if (dataType == 0) {
            ByteArrayInputStream is = new ByteArrayInputStream(data);
            imageLive.setImage(new Image(is));
        } else if (dataType == 2) {
            String result = new String(data, StandardCharsets.UTF_8);
            Platform.runLater(() -> {
                label.setText(label.getText()+"\n"+result);
            });
        } else {
            System.out.println("Unknown data type: " + dataType);
        }
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
