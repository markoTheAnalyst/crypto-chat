package sample;

import com.jfoenix.controls.JFXButton;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static sample.Crypt.isCertificateValid;
import static sample.Crypt.symKeys;
import static sample.FileWatcher.messages;
import static sample.LoginController.currentUser;

public class ChatRoom {

    public JFXButton btnRef;
    public Label nameLabel;

    @FXML
    private TextArea txtMsg;

    @FXML
    public VBox chatBox;

    @FXML
    private Button btnSend;

    @FXML
    private Button btnEmoji;

    @FXML
    private VBox clientListBox;

    static String activeChat = " ";
    static String recipient = currentUser;
    static Map<String,VBox> previousChats = new HashMap<>();
    static public Map<String, Boolean> sessions = new HashMap<>();
    static public Map<String, Boolean> sessionReqSent = new HashMap<>();

    @FXML
    void endSession() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Session established");
        alert.setContentText("Do you want to end the session with "+activeChat+"?");

        ButtonType buttonTypeOne = new ButtonType("Yes");
        ButtonType buttonTypeCancel = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeOne, buttonTypeCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == buttonTypeOne){
            try {
                String payload = "End@"+currentUser+"@";
                String hash = Crypt.getSignature(payload," ");
                Steganography.encode(Crypt.encryptRSA(payload, activeChat) + "@" + hash, activeChat);
                sessionReqSent.put(activeChat, false);
                sessions.put(activeChat,false);
                symKeys.remove(activeChat);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void sendAction() throws Exception {
        if(sessions.containsKey(recipient) && sessions.get(recipient)) {
            Crypt.encipher(txtMsg.getText(), recipient);
            update(currentUser, txtMsg.getText());
            txtMsg.setText("");
            txtMsg.requestFocus();
        }
        else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText("Session not established.");
            alert.showAndWait();
        }
    }


    public void update(String username, String message)  {
        Text text=new Text(message);

        text.setFill(Color.WHITE);
        text.getStyleClass().add("message");
        TextFlow tempFlow=new TextFlow();
        if(!username.equals(currentUser)){
            Text txtName=new Text(username + "\n");
            txtName.getStyleClass().add("txtName");
            tempFlow.getChildren().add(txtName);
        }

        tempFlow.getChildren().add(text);
        tempFlow.setMaxWidth(200);

        TextFlow flow=new TextFlow(tempFlow);

        HBox hbox=new HBox(12);

        Circle img =new Circle(32,32,16);
        String path= new File("resources/user-images/user.png").toURI().toString();
        img.setFill(new ImagePattern(new Image(path)));
        img.getStyleClass().add("imageView");

        if(!username.equals(currentUser)){

            tempFlow.getStyleClass().add("tempFlowFlipped");
            flow.getStyleClass().add("textFlowFlipped");
            chatBox.setAlignment(Pos.TOP_LEFT);
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.getChildren().add(img);
            hbox.getChildren().add(flow);

        }else{
            text.setFill(Color.WHITE);
            tempFlow.getStyleClass().add("tempFlow");
            flow.getStyleClass().add("textFlow");
            hbox.setAlignment(Pos.BOTTOM_RIGHT);
            hbox.getChildren().add(flow);
            hbox.getChildren().add(img);
        }

        hbox.getStyleClass().add("hbox");
        //Platform.runLater(() -> chatBox.getChildren().addAll(hbox));
        chatBox.getChildren().addAll(hbox);

    }


    public void updateUI(List<String> clientList)  {
        //Platform.runLater(() -> clientListBox.getChildren().clear());
        clientListBox.getChildren().clear();
        for(String client : clientList){
            if(client.equals(currentUser)) continue;
//            containerPane.getStyleClass().add("online-user-container");
            HBox container=new HBox() ;
            container.setAlignment(Pos.CENTER_LEFT);
            container.setSpacing(10);
            container.setPrefWidth(clientListBox.getPrefWidth());
            container.setPadding(new Insets(3));
            container.getStyleClass().add("online-user-container");
            Circle img =new Circle(30,30,15);
            String path= new File("resources/user-images/user.png").toURI().toString();
            img.setFill(new ImagePattern(new Image(path)));
            container.getChildren().add(img);
            container.setOnMouseClicked(e -> {
                recipient = container.getAccessibleText();
                clearChat(recipient);
                    });
            VBox userDetailContainer=new VBox();
            userDetailContainer.setPrefWidth(clientListBox.getPrefWidth()/1.7);
            Label lblUsername=new Label(client);
            lblUsername.getStyleClass().add("online-label");
            userDetailContainer.getChildren().add(lblUsername);
            container.getChildren().add(userDetailContainer);
            container.setAccessibleText(lblUsername.getText());
            
            clientListBox.getChildren().add(container);

        }
    }

    public boolean establishSession(String username)  {
        if(!sessionReqSent.containsKey(username) || !sessionReqSent.get(username)){
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            //alert.setTitle("Confirmation Dialog with Custom Actions");
            alert.setHeaderText("Session not established");
            alert.setContentText("Do you want to establish a session with "+username+"?");

            ButtonType buttonTypeOne = new ButtonType("Yes");
            ButtonType buttonTypeCancel = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(buttonTypeOne, buttonTypeCancel);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == buttonTypeOne){
                try {
                    String payload = "Hello@"+currentUser+"@"+Crypt.genSymParam();
                    String hash = Crypt.getSignature(payload," ");
                    if(isCertificateValid(username)) {
                        Steganography.encode(Crypt.encryptRSA(payload, username) + "@" + hash, username);
                        sessionReqSent.put(username, true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
        return sessions.containsKey(username) && sessions.get(username);
    }
    public void clearChat(String username)  {

        if(previousChats.size() != 0) {
            if(!establishSession(username))
                return;
            VBox placeHolder = new VBox();
            placeHolder.getChildren().addAll(chatBox.getChildren());
            previousChats.put(activeChat, placeHolder);
            chatBox.getChildren().clear();
            if(!previousChats.containsKey(username))
                previousChats.put(username,new VBox());
            chatBox.getChildren().addAll(previousChats.get(username).getChildren());
        }
        else {
            previousChats.put(username, new VBox());
            sessions.put(currentUser,false);
            sessionReqSent.put(currentUser,false);
            establishSession(username);
            txtMsg.setVisible(true);
            btnSend.setVisible(true);
            btnEmoji.setVisible(true);
        }
        showNewMessages(username);

        activeChat = username;
    }
    public void closeAction() throws IOException {
        List<String> users = Files.readAllLines(Path.of("active.txt"));
        PrintWriter printWriter = new PrintWriter("active.txt");
        for ( String user : users)
            if(!user.contains(currentUser))
                printWriter.println(user);
        printWriter.close();
        for(String user : sessions.keySet()){
            if(sessions.get(user)){
                try {
                    String payload = "End@"+currentUser+"@";
                    String hash = Crypt.getSignature(payload, " ");
                    Steganography.encode(Crypt.encryptRSA(payload, user) + "@" + hash, user);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) chatBox.getScene().getWindow();
        stage.setScene(new Scene(root));
        sessions.clear();
        sessionReqSent.clear();
        symKeys.clear();
    }

    public void refresh() throws IOException {
        List<String> users = Files.readAllLines(Path.of("active.txt"));
        updateUI(users);
    }
    private void showNewMessages(String username){
        Iterator i = messages.iterator();
        while (i.hasNext()) {
            String str = (String) i.next();
            if (str.startsWith(username)) {
                i.remove();
                try {
                    String[] cipherAndSignature = new String(Files.readAllBytes(Path.of("inbox"+ File.separator+Crypt.getHash(currentUser) + File.separator + str))).split("@");
                    String plainText = Crypt.decipher(cipherAndSignature[0],username);
                    update(username, plainText);
                    Files.delete(Paths.get("inbox"+File.separator+Crypt.getHash(currentUser) + File.separator + str));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
