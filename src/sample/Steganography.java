package sample;
import com.jfoenix.controls.JFXDialog;
import javafx.stage.FileChooser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

public class Steganography {
    public static void encode(String payload, String recipient) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(Path.of("resources/stego").toFile());
        File carrier = fileChooser.showOpenDialog(null);
        int pos = locatePixelArray(carrier);
        int readByte = 0;
        File stegoFile = new File( "inbox"+File.separator+Crypt.getHash(recipient)+File.separator+carrier.getName());
        try {
            Files.copy(carrier.toPath(), stegoFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try (RandomAccessFile stream = new RandomAccessFile(stegoFile, "rw")) {
            stream.seek(pos);
            for (int i = 0; i < 32; i++) {
                readByte = stream.read();
                stream.seek(pos);
                stream.write(readByte & 0b11111110);
                pos++;
            }

            payload += (char) 0;
            int payloadByte;
            int payloadBit;
            int newByte;
            for (char element : payload.toCharArray()) {
                payloadByte = (int) element;
                // System.out.println(element + ":" + Integer.toString(character));
                for (int i = 0; i < 8; i++) {
                    readByte = stream.read();
                    payloadBit = (payloadByte >> i) & 1;
                    newByte = (readByte & 0b11111110) | payloadBit;
                    stream.seek(pos);
                    stream.write(newByte);
                    pos++;
                }
            }

        } catch (IOException e) {
            return;
        }
    }


    public static int locatePixelArray(File file) {
        try (FileInputStream stream = new FileInputStream(file)) {
            stream.skip(10);
            int location = 0;
            for (int i = 0; i < 4; i++) {
                location = location | (stream.read() << (4 * i));
            }
            return location;
        } catch (IOException e) {
            return -1;
        }
    }


    public static String decode(File carrier) {
        int start = locatePixelArray(carrier);
        try (FileInputStream stream = new FileInputStream(carrier)) {
            stream.skip(start);

            for (int i = 0; i < 32; i++) {
                if ((stream.read() & 1) != 0) {
                    return "Picture has not been encoded!!!";
                }
            }

            String result = "";
            int character;
            while (true) {
                character = 0;
                for (int i = 0; i < 8; i++) {
                    character = character | ((stream.read() & 1) << i);
                }
                if (character == 0)
                    break;
                result += (char) character;
            }
            return result;
        } catch (IOException e) {
            return "IOException: " + e.getMessage();
        }
    }

    public static int charactersAvailable(File carrier) {
        return (int) (carrier.length() - locatePixelArray(carrier) + 32) / 8;
    }
}
