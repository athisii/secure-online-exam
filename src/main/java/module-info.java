module com.cdac.secureonlineexam {
    requires java.logging;
    requires java.desktop;

    requires javafx.controls;
    requires javafx.fxml;

    // for reading and writing qr code
    requires com.google.zxing;
    requires com.google.zxing.javase;

    requires opencv;

    opens com.cdac.secureonlineexam to javafx.fxml;
    exports com.cdac.secureonlineexam;
}