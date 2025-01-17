package tictactoe;

import com.google.gson.Gson;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import models.GameModel;
import models.RequsetModel;
import models.ResponsModel;

public class DashboardController implements Initializable {

    @FXML
    private Button games;
    @FXML
    private Button logout;
    @FXML
    private Label score;
    @FXML
    private ListView<String> onlineusers;
    Gson gson = new Gson();
    private String invitedUser;
    private boolean[] isInvited = {false};
    private String userScore;
    private String userName;
    private Timer refreshTimer;

    String currentName;
    DataOutputStream dos;
    DataInputStream dis;
    @FXML
    private Button backButton;

    public void setScore(String playerscore) {
        userScore = playerscore;
        if (userScore != null) {
            score.setText(userScore);
        } else {
            System.out.println("Error: userScore is null!");
        }
    }

    public void setName(String playerName) {
        userName = playerName;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            PlayerSocket playerSocket = PlayerSocket.getInstance();
            dos = playerSocket.getDataOutputStream();
            dis = playerSocket.getDataInputStream();
            fetchOnlineUsers(dos, dis);
            refresh(dos, dis);

            Platform.runLater(() -> {
                score.getScene().getWindow().setOnCloseRequest(event -> cleanupResources());
            });
        } catch (IOException ex) {
            Logger.getLogger(DashboardController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void fetchOnlineUsers(DataOutputStream dos, DataInputStream dis) {
        try {
            String jsonRequest = gson.toJson(new RequsetModel("fetchOnline", null));
            dos.writeUTF(jsonRequest);
            dos.flush();

            String responseOnlineUsers = dis.readUTF();
            ResponsModel res = gson.fromJson(responseOnlineUsers, ResponsModel.class);
            System.out.println("Status: " + res.getStatus());
            System.out.println("Data: " + res.getData());
            if ("success".equals(res.getStatus())) {
                List<String> users = (List<String>) res.getData();
                currentName = users.get(users.size() - 1);
                score.setText(userScore);
                displayOnlineUsers(users);
            }
        } catch (IOException ex) {
            Logger.getLogger(DashboardController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    Thread t;
    boolean running = true;

    private void refresh(DataOutputStream dos, DataInputStream dis) {
        t = new Thread(() -> {
            while (running) {
                try {
                    String jsonRequest = gson.toJson(new RequsetModel("fetchOnline", null));
                    dos.writeUTF(jsonRequest);
                    dos.flush();

                    String responseOnlineUsers = dis.readUTF();
                    ResponsModel res = gson.fromJson(responseOnlineUsers, ResponsModel.class);

                    switch (res.getStatus()) {
                        case "success":
                            List<String> users = (List<String>) res.getData();

                            Platform.runLater(() -> {
                                displayOnlineUsers(users);
                                currentName = users.get(users.size() - 1);
                            });
                            break;

                        case "invitation":
                            System.out.println("Invitation received: " + res.getMessage());
                            Platform.runLater(() -> showInviteAlert(res.getMessage(), res.getData()));
                            break;

                        case "wait":
                            System.out.println("Wait received: " + res.getMessage());
                            Platform.runLater(() -> showAlert(res.getMessage()));
                            break;

                        case "cancel":
                            System.out.println(res.getMessage());
                            Platform.runLater(() -> showAlert(res.getMessage()));
                            break;
                        case "accept":
                            Platform.runLater(()->startGame(res));
                            
                        default:
                            System.out.println("Unknown status: " + res.getStatus());
                            break;
                    }
                    Thread.sleep(5000);
                } catch (IOException ex) {
                    Logger.getLogger(DashboardController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    System.out.println("Thread stopped");
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void displayOnlineUsers(List<String> users) {
        Platform.runLater(() -> {
            ObservableList<String> observableList = FXCollections.observableArrayList(users);
            onlineusers.setCellFactory(lv -> new ListCell<String>() {
                private final HBox content;
                private final Label label;
                private final Button inviteButton;

                {
                    label = new Label();
                    inviteButton = new Button("Invite");

                    inviteButton.setOnAction(event -> {
                        invitedUser = getItem();
                        sendInvite(currentName, invitedUser);
                        inviteButton.setText("invited");
                    });

                    content = new HBox(20, label, inviteButton);
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                    } else if (item.equals(userName)) {
                        setGraphic(null);
                    } else {
                        label.setText(item);
                        inviteButton.setDisable(isInvited[0]);
                        setGraphic(content);
                    }
                }
            });
            onlineusers.setItems(observableList);
        });
    }

    void sendInvite(String sender, String receiver) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("sender", sender);
            data.put("receiver", receiver);

            RequsetModel requsetModel = new RequsetModel("invite", data);

            String inviteJson = gson.toJson(requsetModel);
            dos.writeUTF(inviteJson);

        } catch (IOException ex) {
            Logger.getLogger(DashboardController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void cancelInvite(Object data) {
        try {
            RequsetModel requsetModel = new RequsetModel("cancel", data);

            String inviteJson = gson.toJson(requsetModel);
            dos.writeUTF(inviteJson);

        } catch (IOException ex) {
            Logger.getLogger(DashboardController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void acceptInvite(Object data) {
        try {
            RequsetModel requsetModel = new RequsetModel("accept", data);

            String acceptJson = gson.toJson(requsetModel);
            dos.writeUTF(acceptJson);

        } catch (IOException ex) {
            Logger.getLogger(DashboardController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void startGame(ResponsModel res) {
        GameModel game = gson.fromJson(gson.toJson(res.getData()), GameModel.class);
        System.out.println("start game bettwen " + game.getPlayer1() + " and " + game.getPlayer2());
        navigateToGame(game);
    }

    void showInviteAlert(String txt, Object data) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, txt, ButtonType.YES, ButtonType.NO);
        Stage currentStage = (Stage) score.getScene().getWindow();
        alert.initOwner(currentStage);
        alert.showAndWait().ifPresent(click -> {
            if (click == ButtonType.NO) {
                cancelInvite(data);
            }
            if (click == ButtonType.YES) {
                System.out.println("Accepting invitation with data: " + data);
                acceptInvite(data);
            }
        });
    }

    public void showAlert(String txt) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        Stage currentStage = (Stage) score.getScene().getWindow();
        alert.initOwner(currentStage);
        alert.setHeaderText(txt);
        alert.getButtonTypes().clear();
        alert.getButtonTypes().add(ButtonType.CLOSE);
        alert.showAndWait();
    }

    
    void navigateToGame(GameModel game)
    {
        try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("onlineGame.fxml"));
        Parent root = loader.load();

        onlineGamecontroller gameController = loader.getController();
        gameController.startGame(game);

        // Set the new scene
        Stage stage = (Stage) score.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Game Screen");
    } catch (IOException ex) {
        Logger.getLogger(DashboardController.class.getName()).log(Level.SEVERE, null, ex);
    }
    }
    private void cleanupResources() {
       running = false;
    if (t != null) {
        t.interrupt();
    }
    System.out.println("Thread stopped");
    }

    @FXML
    private void handleBackButton(ActionEvent event) {
        //new LoginController().navigateToScreen(event, "Menu.fxml", "Menu");
    }
}