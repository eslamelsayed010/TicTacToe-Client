/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tictactoe;

import com.google.gson.Gson;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import models.RequsetModel;

/**
 * FXML Controller class
 *
 * @author El-Wattaneya
 */
public class VideoLayoutController implements Initializable {
    private String userName;

    @FXML
    private Label winnerLabel;
    @FXML
    private MediaView mediaView;
    @FXML
    private Button newGame;
    @FXML
    private Button close;

    private MediaPlayer mediaPlayer;
    private Runnable onNewGameAction;

    public void setUserName(String userName) {
        this.userName = userName;
    }
    public void initialize(String path) {
        try {
            String videoPath = getClass().getResource(path).toExternalForm();
            if (videoPath != null) {
                Media media = new Media(videoPath);
                mediaPlayer = new MediaPlayer(media);
                mediaView.setMediaPlayer(mediaPlayer);
                mediaPlayer.play();

            } else {
                System.out.println("Video path is null. Check the resource path.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnNewGameAction(Runnable onNewGameAction) {
        this.onNewGameAction = onNewGameAction;
    }

    @FXML
    private void onNewGameClicked(ActionEvent event) {
        // First stop media player
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }  
              
        // Execute new game action to clean up game resources
        if (onNewGameAction != null) {
            onNewGameAction.run();
        }

    }

    @FXML
    private void onCloseClicked(ActionEvent event) {
        try {
            // First stop the media player
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }

            // Get the current video stage
            Stage videoStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Get the game stage from the singleton application stage
            Stage gameStage = TicTacToe.getPrimaryStage();

            // Send logout request
            PlayerSocket playerSocket = PlayerSocket.getInstance();
            DataOutputStream dos = playerSocket.getDataOutputStream();
            
            // Create logout request
            Map<String, String> data = new HashMap<>();
            data.put("username", userName); // You'll need to add userName as a class field
            String jsonRequest = new Gson().toJson(new RequsetModel("logout", data));
            dos.writeUTF(jsonRequest);
            dos.flush();
            
            // Load the menu scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Menu.fxml"));
            Parent menuRoot = loader.load();
            
            // Set the menu scene on the main stage
            gameStage.setScene(new Scene(menuRoot));
            gameStage.show();
            
            // Close the video window
            videoStage.close();

        } catch (IOException ex) {
            Logger.getLogger(VideoLayoutController.class.getName()).log(Level.SEVERE, 
                "Error navigating to menu", ex);
        }
    }

    public void setWinnerText(String text) {
        winnerLabel.setText(text);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
