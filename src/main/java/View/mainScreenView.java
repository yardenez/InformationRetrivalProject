package View;


import Model.ResultEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;

public class mainScreenView extends AView {
    @FXML
    public Button btnBrowseData;
    public Button btnBrowseStoringPath;
    public TextArea txtfld_dataPath;
    public TextArea txtfld_storingPath;
    public CheckBox checkBox_stem;
    public Button btn_showDict;
    public Button btn_reset;
    public Button btn_loadDict;
    public Button btn_startProgram;
    public ChoiceBox<String> box_language;
    public Label lbl_language;
    public Button btn_runQuery;
    public Button btn_browseQueryFile;
    public RadioButton radioBtn_runQuery;
    public RadioButton radioBtn_browseQueryFile;
    public TextArea txtfld_storingQuery;
    public TextArea txtfld_storingQueryFile;
    public ListView<String> lv_cities;
    public ListView<String> lv_languages;
    public CheckBox cb_semantic;


    private boolean useStemming=false;

    public static Thread t;

    public void handleDataSetChoice(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose data set");
        File selectedDirectory= directoryChooser.showDialog(null);
        if(selectedDirectory!=null){
            txtfld_dataPath.setText(selectedDirectory.getAbsolutePath());
        }
    }
    private void loadLanguagesListView(){
        ObservableList<String> languages = FXCollections.observableArrayList();
        languages.addAll(myController.getLanguages());
        lv_languages.setItems(languages);
        lv_languages.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void loadCitiesListView(){
        ObservableList<String> cities = FXCollections.observableArrayList();
        cities.addAll(myController.getCities());
        lv_cities.setItems(cities);
        lv_cities.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }
    public void handleStoringPathChoise(ActionEvent actionEvent){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("choose dictionary and posting location");
        File selectedDirectory= directoryChooser.showDialog(null);
        if(selectedDirectory!=null){
            txtfld_storingPath.setText(selectedDirectory.getAbsolutePath());
        }
    }

    public void loadGivenChoices(ActionEvent actionEvent){
        if(!txtfld_storingPath.getText().isEmpty() && !txtfld_dataPath.getText().isEmpty()) {
            String dataSetPath=txtfld_dataPath.getText();
            String dictionryAndPostingPath=txtfld_storingPath.getText();
            useStemming= checkBox_stem.isSelected();
            this.myController.updateUserChoices(dataSetPath,dictionryAndPostingPath,useStemming);
            btn_startProgram.setDisable(false);
            btn_showDict.setDisable(false);
            btn_loadDict.setDisable(false);
            btn_reset.setDisable(false);
        }
        else{
            displayErrorMessage("Please enter the required paths!","Failed! fields empty ");
        }
    }




    public void startProgram(ActionEvent actionEvent){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<String[]> callable = new Callable<String[]>() {
            @Override
            public String[] call() {
                return myController.activateProgram();
            }
        };
        Future<String[]> future = executor.submit(callable);
        executor.shutdown();
        String[] dataReturned = null;
        try {
            dataReturned = future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        if(dataReturned!=null) {
            displayInformationMessage("number of indexed documents:" + dataReturned[1] + "\n" +
                    "number of unique terms in corpus:" + dataReturned[2] + "\n" +
                    "total time[min]:" + dataReturned[0], "inverted index created successfully");
            //present languages
            loadLanguagesListView();
            loadCitiesListView();
        }
        else
            displayErrorMessage("Error occurred, please try again.","Failed!");

    }

    public void loadDictionaryToMemory(ActionEvent actionEvent) {
        if(!this.myController.loadDictionaryToMemory())
            displayErrorMessage("Dictionary doesn't exist. Please reactivate the process","Failure");
        else{
            displayConfirmationMessage("Dictionary was loaded.","success");
            loadLanguagesListView();
            loadCitiesListView();
        }
    }

    public void reset(ActionEvent actionEvent){
        Boolean success= myController.reset();
        if(success)
            displayInformationMessage("System is now reset","process ended successfully");
        else
            displayErrorMessage("failed to reset system.","failure!");
    }

    public void showDictionary(ActionEvent actionEvent){
        List<String> dictionaryForDisplay= myController.showDictionary();
        //open new window
        if(!dictionaryForDisplay.isEmpty()) {
            FXMLLoader fxmlLoader = new FXMLLoader();
            try {
                InputStream is = this.getClass().getResource("/dictionaryView.fxml").openStream();
                Parent dispalyOfDictScene = fxmlLoader.load(is);
                AView dictionaryView = fxmlLoader.getController();
                dictionaryView.setMyController(this.myController);
                Scene newScene = new Scene(dispalyOfDictScene, 600, 400);
                Stage curStage = (Stage) btn_reset.getScene().getWindow();
                curStage.setScene(newScene);
                ((dictionaryView) dictionaryView).pullDictionary(dictionaryForDisplay);
                curStage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            displayErrorMessage("dictionary is empty", "Failed to show dictionary");
        }
    }


    public void handleRunQuery(){
        myController.clearQueryResults();
        ArrayList<ResultEntry> querySearchResults;
        ObservableList<String> citiesSelection= lv_cities.getSelectionModel().getSelectedItems();
        ObservableList<String> languageSelection =lv_languages.getSelectionModel().getSelectedItems();
         boolean useSemnatic=false;
        if(cb_semantic.isSelected())
            useSemnatic=true;
        if(radioBtn_runQuery.isSelected() && !txtfld_storingQuery.getText().isEmpty()) {
            displayInformationMessage("query is processed","processing");
            querySearchResults = myController.runManualQuery(txtfld_storingQuery.getText(), citiesSelection,useSemnatic,languageSelection);
            ObservableList<ResultEntry> temp=FXCollections.observableArrayList(querySearchResults);
            myController.setQueriesResults(temp);
            loadOtherWindow("/searchedQueries.fxml","all queries",310,400);
        }
        else if (radioBtn_browseQueryFile.isSelected() && !txtfld_storingQueryFile.getText().isEmpty()) {
            displayInformationMessage("query is processed","processing");
            querySearchResults = myController.runQueryFile(txtfld_storingQueryFile.getText(), citiesSelection,useSemnatic,languageSelection);
            myController.setQueriesResults(FXCollections.observableArrayList(querySearchResults));
            loadOtherWindow("/searchedQueries.fxml","all queries",550,400);
        }
        else{
            displayErrorMessage("Please enter a query.","Failed! Field empty");
        }
    }

    public void handleBrowseQueryFile(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose query file");
        File selectedFile= fileChooser.showOpenDialog(null);
        if(selectedFile!=null){
            txtfld_storingQueryFile.setText(selectedFile.getAbsolutePath());
            btn_runQuery.setDisable(false);
        }
    }

    public void handleRadioButtonRunQuery(){
        if (radioBtn_runQuery.isSelected() && !radioBtn_browseQueryFile.isSelected()){
            txtfld_storingQuery.setEditable(true);
            btn_runQuery.setDisable(false);
        }
        else if (!radioBtn_runQuery.isSelected()){
            txtfld_storingQuery.clear();
            txtfld_storingQuery.setEditable(false);
            btn_runQuery.setDisable(true);
        }
        else{
            displayErrorMessage("Please choose only one option.","Failed! Multiple options");
            radioBtn_runQuery.setSelected(false);
        }
    }

    public void handleRadioBrowseQueryFile(){
        if (radioBtn_browseQueryFile.isSelected() && !radioBtn_runQuery.isSelected()){
            //txtfld_storingQueryFile.setEditable(true);
            btn_browseQueryFile.setDisable(false);
        }
        else if (!radioBtn_browseQueryFile.isSelected()){
            txtfld_storingQueryFile.clear();
            //txtfld_storingQueryFile.setEditable(false);
            btn_browseQueryFile.setDisable(true);
            btn_runQuery.setDisable(true);
        }
        else{
            displayErrorMessage("Please choose only one option.","Failed! Multiple options");
            radioBtn_browseQueryFile.setSelected(false);
        }
    }

}
