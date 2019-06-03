package View;


import Controller.Controller;
import Model.CityTerm;
import Model.ResultEntry;
import Model.Term;
import Model.myModel;
import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;
import java.util.*;

import static Model.Indexer.citiesDictionary;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Welcome to our retrieval system!");
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/mainScreen.fxml"));
        Parent root = (Parent) fxmlLoader.load();
        primaryStage.setScene(new Scene(root, 550, 650));
        myModel myModel = Model.myModel.getInstance();
        Controller myController = Controller.getInstance();
        AView view = fxmlLoader.getController();
        view.setMyController(myController);
        primaryStage.show();
        // dealing with closing the app
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Exit");
                alert.setContentText("Are you sure that you want to exit?");
                alert.setHeaderText(null);
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                } else if (result.get() == ButtonType.CANCEL) {
                    alert.close();
                    event.consume();
                }
            }
        });
    }


    public static void main(String[] args) {
        launch(args);

        //code for checking query manual
        /*
        myModel myModel = new myModel();
        myModel.updateUserChoices("C:\\Users\\yarden\\IdeaProjects\\testCorpus","C:\\Users\\yarden\\IdeaProjects\\posting",false);
        myModel.loadDataToMemory();
        ObservableList<String> list= FXCollections.observableArrayList();
        list.add("MOSCOW");
        list.add("BEIJING");
        ArrayList<ResultEntry> results = myModel.runManualQuery("separate",list);
        */
    }
    /**
     * This method proved a quicker implemtion of the str.split() method.
     * Used to improve preformences.
     *
     * @param str       - the string we wish to split.
     * @param delimter- the delimter we wish to split the string by.
     * @return a String array contains all the splitted tokens.
     */
    public static String[] split(String str, String delimter) {
        ArrayList<String> splittedData = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(str, delimter);
        while (tokenizer.hasMoreTokens()) {
            splittedData.add(tokenizer.nextToken());
        }
        String[] splitResult = new String[splittedData.size()];
        return splittedData.toArray(splitResult);
    }
}

