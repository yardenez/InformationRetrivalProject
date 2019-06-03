package View;

import Model.Term;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import java.util.List;

public class dictionaryView extends AView {
    @FXML
    public ListView<String> dictionary_view;
    public Button btn_back;

    public void pullDictionary(List<String> dict) {
        dictionary_view.getItems().setAll(FXCollections.observableList(dict));
    }

    public void handleBackPressed(ActionEvent actionEvent)
    {
        FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource("/mainScreen.fxml"));
        try{
            Parent parent = fxmlLoader.load();
            mainScreenView mainView = (mainScreenView) fxmlLoader.getController();
            mainView.setMyController(this.myController);
            mainView.btn_reset.setDisable(false);
            mainView.btn_loadDict.setDisable(false);
            mainView.btn_showDict.setDisable(false);
            mainView.btn_startProgram.setDisable(false);
            Scene newScene = new Scene(parent,550,650);
            Stage curStage = (Stage) btn_back.getScene().getWindow();
            curStage.setScene(newScene);
            curStage.show();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
