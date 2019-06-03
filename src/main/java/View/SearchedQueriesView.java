package View;

import Controller.Controller;
import Model.ResultEntry;
import Model.SearchedQueryCol;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.ResourceBundle;

public class SearchedQueriesView extends AView  implements Initializable {
    private Controller controller = Controller.getInstance();
    public TableView<SearchedQueryCol> tableSearchQueries;
    public TableColumn<SearchedQueryCol, String> queryIDCol;
    public TableColumn<SearchedQueryCol, String> queryTextCol;
    public TableColumn<SearchedQueryCol, String> showResultsOfQuery;
    public Button btn_browse;
    public Button btn_saveResults;
    public TextField txt_resultsPath;

    public void initialize(URL location, ResourceBundle resources) {
        queryIDCol.setCellValueFactory(new PropertyValueFactory<>("queryID"));
        queryTextCol.setCellValueFactory(new PropertyValueFactory<>("queryText"));
        showResultsOfQuery.setCellValueFactory(new PropertyValueFactory<>("btn_showResults"));
        ObservableList<ResultEntry> searchedQueries  = controller.getQueriesResults();
        ObservableList<SearchedQueryCol> data = FXCollections.observableArrayList();
        for (ResultEntry rs : searchedQueries) {
            SearchedQueryCol searchedQueryDataForCol=new SearchedQueryCol(rs.getQueryId(),rs.getQueryText(),new Button());
            if(!data.contains(searchedQueryDataForCol))
                data.add(new SearchedQueryCol(rs.getQueryId(),rs.getQueryText(),new Button()));
        }
        tableSearchQueries.setItems(data);
        btn_saveResults.setDisable(true);
    }

    //open a folder dialog so the user will be able to choose a path into he wish to save the query results.
    public void handleBrowseSelection(){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose results path ");
        File selectedFile= directoryChooser.showDialog(null);
        if(selectedFile!=null){
            txt_resultsPath.setText(selectedFile.getAbsolutePath());
            btn_saveResults.setDisable(false);
        }
    }

    //save the results of the query in the next form:
    public void handleSaveResultsRequest(){
        ObservableList<ResultEntry> queriesResults  = controller.getQueriesResults();
        try {
            BufferedWriter writer;
            if(!controller.getStemmerValue()) {
                if (controller.getSemanticValue())
                    writer = new BufferedWriter(new FileWriter(txt_resultsPath.getText() + "\\query_res_s.txt"));
                else
                    writer = new BufferedWriter(new FileWriter(txt_resultsPath.getText() + "\\query_res.txt"));
            } else {
                if (controller.getSemanticValue())
                    writer = new BufferedWriter(new FileWriter(txt_resultsPath.getText() + "\\query_res_stemmed_s.txt"));
                else
                    writer = new BufferedWriter(new FileWriter(txt_resultsPath.getText() + "\\query_res_stemmed.txt"));
            }
            for (int i=0;i<queriesResults.size();i++) {
                ResultEntry singleRS=queriesResults.get(i);
                writer.write(singleRS.getQueryId()+ " 0 "+ singleRS.getDocName()+" "+singleRS.getRank()+" 42.38 mt\n");
            }
            writer.close();
            displayConfirmationMessage("Results saved!","Success");
        }catch (Exception e){
            displayErrorMessage("Results were not saved.Please try again","Failed");
            System.out.println("Failed to write query results to disk");//TODO:delete
        }



    }


}
