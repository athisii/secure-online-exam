package com.cdac.secureonlineexam;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author athisii
 * @version 1.0
 * @since 5/9/24
 */

public class QrCodeUtil {
    private static final Logger LOGGER = ApplicationLog.getLogger(QrCodeUtil.class);

    private QrCodeUtil() {

    }

    private static final MultiFormatReader reader = new MultiFormatReader();
    private static final MultiFormatWriter writer = new MultiFormatWriter();


    public static String readQrCode(String pathStr) {
        BufferedImage image;
        try {
            Path path = Path.of(pathStr);
            image = ImageIO.read(path.toFile());
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Files.delete(path);
            return reader.decode(bitmap).getText();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            LOGGER.log(Level.INFO, "**No qr code found in the captured image.\n***Going for next the shot....");
            // qr code not found in the image
        }
        return "";
    }

    public static byte[] createQR(String data, String path, int height, int width) {
        try {
            BitMatrix matrix = writer.encode(new String(data.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8), BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToPath(matrix, "jpeg", Path.of(path));
            MatrixToImageWriter.writeToStream(matrix, "jpeg", byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "**Error creating QR Code: ", ex);
            throw new GenericException("Error creating QR code.");
        }
    }
}
