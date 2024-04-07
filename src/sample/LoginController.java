package sample;


import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static sample.Main.passwords;

public class LoginController {

    public Button logBtn;
    public PasswordField tbPass;
    public static String currentUser;
    public TextField tbUsername;

    public void buttonClick() throws IOException {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information Dialog");
        alert.setHeaderText(null);

        currentUser = tbUsername.getText();
        for (String user : Files.readAllLines(Path.of("active.txt")))
            if(user.equals(currentUser)) {
                Platform.runLater(() -> {
                    alert.setContentText(currentUser + " is already logged in!");
                    alert.showAndWait();
                });
                return;
            }
        if(passwords.containsKey(currentUser) && Crypt.isPassCorrect(passwords.get(currentUser),tbPass.getText())){
            PrintWriter printWriter = new PrintWriter(new FileWriter("active.txt",true));
            printWriter.println(currentUser);
            printWriter.close();
            Stage stage = (Stage) tbPass.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("chat.fxml"));
            Parent root = loader.load();//FXMLLoader.load(getClass().getResource("chat.fxml"));
            ChatRoom controller = loader.getController();
            controller.nameLabel.setText(currentUser);
            new FileWatcher(Paths.get("inbox"+ File.separator+Crypt.getHash(currentUser)),controller).start();
            stage.setScene(new Scene(root));
        }
        else{
            Platform.runLater(() -> {
                alert.setContentText("Wrong username or password.");
                alert.showAndWait();
            });
        }

    }
}
