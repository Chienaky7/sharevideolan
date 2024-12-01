package com.example.met_qua;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import javafx.fxml.FXMLLoader;
import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.DatagramPacket;
import java.nio.*;
import java.util.*;

public class HelloController {
    @FXML
    private Label welcomeText;
    @FXML
    private BorderPane app;
    @FXML
    private MediaView mediamain;
    @FXML
    private ImageView play;
    @FXML
    private Slider slidertime;
    @FXML
    private TextArea txtIp;
    @FXML
    private TextArea txtPort;

    private  boolean pause = true;
    private  MediaPlayer mediaPlayer;
    private static SocketManager soket;

    public static String Ip;
    public static int PORT;
    private static  String filePath;

    @FXML
    protected void openfile() throws IOException {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose a video");
        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter("Image Files", "*.mp4");
        fc.getExtensionFilters().add(imageFilter);
        File file = fc.showOpenDialog(null);
        if (file != null){

            filePath = file.toURI().toString();
            Media media = new Media(filePath);
            mediaPlayer = new MediaPlayer(media);
            mediamain.setMediaPlayer(mediaPlayer);

            mediaPlayer.setOnReady(() -> {
                Duration totalDuration = mediaPlayer.getTotalDuration();
                slidertime.setMax(totalDuration.toMillis());

                slidertime.setValue(0);
            });

            mediaPlayer.currentTimeProperty().addListener((observableValue, oldValue, newValue) -> {
                slidertime.setValue(newValue.toMillis());
            });
            Scene scene = mediamain.getScene();
            mediamain.fitHeightProperty().bind(scene.widthProperty());
            mediamain.fitWidthProperty().bind(scene.heightProperty());
            slidertime.setMax(mediaPlayer.getTotalDuration().toMillis());
            //mediaPlayer.setAutoPlay(true);
            play.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/play-button.png"))));

        }

    }
    @FXML
    protected void openlivescreen() throws IOException {
        Stage stage = (Stage) app.getScene().getWindow(); // Lấy Stage hiện tại
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("live-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);

    }
    @FXML
    protected void openxemlive() throws IOException {
        Stage stage = (Stage) app.getScene().getWindow(); // Lấy Stage hiện tại
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("xemlive-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);

    }
    @FXML
    protected void handleMouseEnter(MouseEvent event){
        play.setVisible(true);
        slidertime.setVisible(true);
    }
    @FXML
    protected void openLive() throws Exception {
//        Stage stage = (Stage) app.getScene().getWindow(); // Lấy Stage hiện tại
//        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("live-view.fxml"));
//        Scene scene = new Scene(fxmlLoader.load());
//        stage.setScene(scene);
        sendMedia();

    }
    @FXML
    protected void handleMouseExit(MouseEvent event){
        play.setVisible(false);
        slidertime.setVisible(false);
    }
    @FXML
    protected void clickPlayPause(MouseEvent event){
        if(pause){
            mediaPlayer.play();
            play.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/pause.png"))));
            pause = false;
        }else {
            mediaPlayer.pause();
            play.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/play-button.png"))));
            pause = true;

        }

    }
    @FXML
    protected void sliderPressed(MouseEvent event){
        mediaPlayer.seek(Duration.seconds(slidertime.getValue() / 1000.0));
    }
    @FXML
    protected void hanldeStop(MouseEvent event){
        mediaPlayer.pause();
        play.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/play-button.png"))));
        mediaPlayer.seek(Duration.seconds(0));
    }
    @FXML
    protected void liveScreen(MouseEvent event){

    }
    private void opensever() throws IOException {
        if (Objects.equals(txtIp.getText(), "") || Objects.equals(txtPort.getText(), "")) {
            JOptionPane.showMessageDialog(null, "Vui lòng nhập IP và Port.");
            return;
        }
        PORT = Integer.parseInt(txtPort.getText());
        soket = new SocketManager();
        soket.connect(txtIp.getText(),PORT);

    }
    private void sendMedia() throws Exception {
        new Thread(()->{
            try {
                opensever();
                sendVideoAndAudio(filePath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();

    }
    private static void sendVideoAndAudio(String inputFilePath) throws Exception {
        String outputPath1 = inputFilePath.replaceFirst("file:/", "");
        Java2DFrameConverter converter = new Java2DFrameConverter();
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(outputPath1);
        FFmpegFrameRecorder recorder = null;
        grabber.start();

        Frame frame;
        int coutr  = 0;
        while ((frame = grabber.grab()) != null) {

            if (frame.image != null) {
                BufferedImage image = converter.convert(frame);
                soket.sendImage(coutr,image);
            }
            if (frame.samples != null) {
                byte[] audioData = convertSamplesToBytes(frame.samples);
                soket.sendAudio(coutr,audioData);
            }
            coutr ++;
        }
    }
    private static byte[] convertSamplesToBytes(Buffer[] samples) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Chuyển đổi từng buffer thành mảng byte
        for (Buffer buffer : samples) {
            if (buffer instanceof ShortBuffer) {
                ShortBuffer shortBuffer = (ShortBuffer) buffer;
                while (shortBuffer.hasRemaining()) {
                    baos.write(shortBuffer.get() & 0xFF);         // Ghi byte thấp
                    baos.write((shortBuffer.get() >> 8) & 0xFF); // Ghi byte cao
                }
            } else {
                throw new IllegalArgumentException("Unsupported Buffer type: " + buffer.getClass());
            }
        }

        return baos.toByteArray();
    }


}