package com.example.met_qua;

import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;
import java.util.TreeMap;

public class SocketManager {
    private static SocketManager instance;
    private static MulticastSocket socket;
    private static InetAddress address;
    private static int port;
    private TreeMap<Integer, FrameData> frameBuffer = new TreeMap<>();
    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    public void connect(String host, int port) throws IOException {
        if (socket == null || socket.isClosed()) {
            try {
                socket = new MulticastSocket(port);
                address = InetAddress.getByName(host);
                SocketManager.port = port;
                socket.joinGroup(address);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println("Socket đã được tạo và đang kết nối.");
        }
    }

    public void sendMess(String data) {

    }

    public void close() throws IOException {
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }
    public void sendImage(int stt,  BufferedImage image ) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", baos);
        byte[] frameData = baos.toByteArray();
        send(0,stt,frameData);
    }
    public void sendAudio(int stt, byte[] audioData ) throws Exception {
        send(1,stt,audioData);
    }
    public void sendData(int stt, byte[] data ) throws Exception {
        send(3,stt,data);
    }
    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }
    private void send(int dataType, int sequenceNumber, byte[] data) throws Exception {
        byte[] packet = new byte[1 + 4 + data.length];

        packet[0] = (byte) dataType;

        packet[1] = (byte) (sequenceNumber >> 24);
        packet[2] = (byte) (sequenceNumber >> 16);
        packet[3] = (byte) (sequenceNumber >> 8);
        packet[4] = (byte) (sequenceNumber);

        System.arraycopy(data, 0, packet, 5, data.length);

        DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, address, port);
        socket.send(datagramPacket);
    }


}