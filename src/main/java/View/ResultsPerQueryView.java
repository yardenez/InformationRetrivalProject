package View;

import Controller.Controller;
import Model.QueryResultCol;
import Model.ResultEntry;
import Model.SearchedQueryCol;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class ResultsPerQueryView extends AView implements Initializable {
    private Controller controller = Controller.getInstance();
    public TableView<QueryResultCol> tableQueryResults;
    public TableColumn<QueryResultCol, String> docIdCol;
    public TableColumn<QueryResultCol, String> docRankCol;
    public TableColumn<QueryResultCol, String> showEntitiesCol;
    public Button btn_back;

    @SuppressWarnings("Duplicates")
    public void initialize(URL location, ResourceBundle resources) {
        docIdCol.setCellValueFactory(new PropertyValueFactory<>("docId"));
        docRankCol.setCellValueFactory(new PropertyValueFactory<>("docRank"));
        showEntitiesCol.setCellValueFactory(new PropertyValueFactory<>("btn_showEntities"));
        docRankCol.setCellValueFactory(new PropertyValueFactory<>("docRank"));
        ObservableList<ResultEntry> queryResults  = controller.getSingleQueryResults();
        ObservableList<QueryResultCol> data = FXCollections.observableArrayList();
        for (ResultEntry rs : queryResults) {
                data.add(new QueryResultCol(rs.getDocName(),String.valueOf(rs.getRank()),new Button()));
        }
        tableQueryResults.setItems(data);

    }

    public void handleBackPressed(){
        openNewWindow("/searchedQueries.fxml",btn_back,600,350);
    }
}

