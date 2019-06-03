package View;

//import Controller.Controller;
import Controller.Controller;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public abstract class AView {

   protected Controller myController;

    /**
     * Sets the view's controller with given controller.
    // * @param controller - the specific controller which the view interacts with.
     */
    public void setMyController(Controller controller) {this.myController =controller;}

    //general message displays

    public void displayInformationMessage(String alertMessage, String title){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(alertMessage);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public void displayConfirmationMessage(String alertMessage, String title){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setContentText(alertMessage);
        alert.show();
    }

    public void displayErrorMessage(String alertMessage, String title){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(alertMessage);
        alert.setHeaderText(null);
        alert.show();
    }

    /**
     * opens a new scene above the primary stage
     * @param fxmlName - the fmxl we wish to load.
     * @param btn - a button which appears on the current scene. used in order to get the current stage.
     */
    public void openNewWindow(String fxmlName, Button btn , int width, int height){
        FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource(fxmlName));
        try{
            Parent parent = fxmlLoader.load();
            AView newView = fxmlLoader.getController();
            newView.setMyController(this.myController);
            Scene newScene = new Scene(parent,width,height);
            Stage curStage = (Stage) btn.getScene().getWindow();
            curStage.setScene(newScene);
            curStage.show();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * opens a new window aside the current window.
     * @param fxmlName -the fmxl we wish to load.
     * @param title- the title of the window.
     * @param width - the width of the new window.
     * @param height- thw height of the new window.
     */
    public void loadOtherWindow(String fxmlName,String title, int width, int height){
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource(fxmlName));
            Parent parent = (Parent) fxmlLoader.load();
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(parent));
            stage.showAndWait();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}//AView
