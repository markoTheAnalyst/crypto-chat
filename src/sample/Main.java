package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import static sample.Crypt.loadCert;

public class Main extends Application {

    static Map<String,String> passwords = new HashMap<>();

    @Override
    public void start(Stage primaryStage) throws Exception{
        Security.addProvider(new BouncyCastleProvider());
        Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
        for (String user : Files.readAllLines(Path.of("users.txt"))){
            passwords.put(user.split("-")[0],user.split("-")[1]);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
