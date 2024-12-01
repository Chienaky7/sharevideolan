module com.example.met_qua {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop;
    requires org.bytedeco.javacv;
    requires org.bytedeco.ffmpeg;

    opens com.example.met_qua to javafx.fxml;
    exports com.example.met_qua;
}