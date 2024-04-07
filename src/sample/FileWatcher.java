package sample;



import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import javax.crypto.spec.SecretKeySpec;

import java.io.*;
import java.nio.file.*;
import java.security.Key;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Optional;

import static sample.ChatRoom.*;
import static sample.Crypt.symKeys;
import static sample.LoginController.currentUser;

public class FileWatcher extends Thread {
    private Path dir;
    private ChatRoom chatRoom;
    public static LinkedList<String> messages = new LinkedList<>();
    FileWatcher(Path dir, ChatRoom chatRoom){
        this.dir = dir;
        this.chatRoom = chatRoom;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
            while (true) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException ex) {
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    sleep(1000);
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE && fileName.toString().trim().endsWith(".txt")) {
                        String sender = fileName.toString().split("-")[0];
                        String[] cipherAndSignature = new String(Files.readAllBytes(Path.of(dir + File.separator + fileName))).split("@");
                        String plainText = Crypt.decipher(cipherAndSignature[0],sender);
                        if(Crypt.isSignatureValid(cipherAndSignature[1],plainText,cipherAndSignature[2],sender)) {
                            if (sender.startsWith(activeChat)) {
                                Platform.runLater(() -> {
                                    try {
                                        chatRoom.update(sender, plainText);
                                        Files.delete(Paths.get(dir + File.separator + fileName));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });

                            } else {
                                messages.add(String.valueOf(fileName));
                            }
                        }
                        else
                            Files.delete(Paths.get(dir + File.separator + fileName));

                    }
                    else if (kind == StandardWatchEventKinds.ENTRY_CREATE && fileName.toString().equals("ack")){
                        String cipherText = new String(Files.readAllBytes(Paths.get(dir + File.separator + fileName)));
                        String[] cipherAndSignature = cipherText.split("@");
                        String plainText = Crypt.decryptRSA(cipherAndSignature[0]);
                        String sender = plainText.split("@")[1];
                        if(Crypt.isSignatureValid(cipherAndSignature[1],plainText,cipherAndSignature[2],sender)) {
                            String[] parts = plainText.split("@");
                            if("OK".equals(parts[0])) {
                                byte[] decKey = Base64.getDecoder().decode(parts[3]);
                                Key secretKey = new SecretKeySpec(decKey, 0, decKey.length, parts[2]);
                                symKeys.put(sender, secretKey);
                                sessionReqSent.put(sender, true);
                                sessions.put(sender, true);
                            }
                            else
                                sessionReqSent.put(sender, false);
                        }
                        else
                            sessionReqSent.put(sender, false);
                        Files.delete(Paths.get(dir + File.separator + fileName));
                    }
                    else if (kind == StandardWatchEventKinds.ENTRY_CREATE && fileName.toString().trim().endsWith(".bmp")){
                        String message = Steganography.decode(new File(dir + File.separator + fileName));
                        String[] cipherAndSignature = message.split("@");
                        String text = Crypt.decryptRSA(cipherAndSignature[0]);
                        String sender = text.split("@")[1];
                        String[] parts = text.split("@");
                        if (Crypt.isSignatureValid(cipherAndSignature[1],text,cipherAndSignature[2],sender)) {
                            if ("Hello".equals(parts[0])) {
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                    //alert.setTitle("Confirmation Dialog with Custom Actions");
                                    alert.setHeaderText("Session request");
                                    alert.setContentText("Do you want to establish a session with " + sender + "?");

                                    ButtonType buttonTypeOne = new ButtonType("Yes");
                                    ButtonType buttonTypeCancel = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);

                                    alert.getButtonTypes().setAll(buttonTypeOne, buttonTypeCancel);

                                    Optional<ButtonType> result = alert.showAndWait();
                                    if (result.get() == buttonTypeOne) {
                                        sessionReqSent.put(sender, true);
                                        sessions.put(sender, true);
                                        byte[] decKey = Base64.getDecoder().decode(parts[3]);
                                        Key secretKey = new SecretKeySpec(decKey, 0, decKey.length, parts[2]);
                                        symKeys.put(sender, secretKey);
                                        try {
                                            PrintWriter printWriter = new PrintWriter("inbox" + File.separator + Crypt.getHash(sender) + File.separator + "ack");
                                            String payload = "OK@" + currentUser + "@" + parts[2] + "@" + parts[3];
                                            printWriter.print(Crypt.encryptRSA(payload, sender)+"@"+Crypt.getSignature(payload,cipherAndSignature[2]));
                                            printWriter.close();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        try {
                                            PrintWriter printWriter = new PrintWriter("inbox" + File.separator + Crypt.getHash(sender) + File.separator + "ack");
                                            String payload = "NO@" + currentUser + "@" + parts[2] + "@" + parts[3];
                                            printWriter.print(Crypt.encryptRSA(payload, sender)+"@"+Crypt.getSignature(payload,cipherAndSignature[2]));
                                            printWriter.close();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });

                            } else if ("End".equals(parts[0])) {
                                sessionReqSent.put(sender, false);
                                sessions.put(sender, false);
                                symKeys.remove(sender);
                            }
                        }
                        else{
                            try {
                                PrintWriter printWriter = new PrintWriter("inbox" + File.separator + Crypt.getHash(sender) + File.separator + "ack");
                                String payload = "NO@" + currentUser + "@" + parts[2] + "@" + parts[3];
                                printWriter.print(Crypt.encryptRSA(payload, sender)+"@"+Crypt.getSignature(payload,cipherAndSignature[2]));
                                printWriter.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
