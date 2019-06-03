package Model;

import Controller.Controller;
import View.AView;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class SearchedQueryCol {

    private Controller controller=Controller.getInstance();
    public String queryID;
    public String queryText;
    public Button btn_showResults;

    //getters
    public String getQueryID() {
        return queryID;
    }

    public String getQueryText() {
        return queryText;
    }

    public Button getBtn_showResults() {
        return btn_showResults;
    }

    //setters

    public void setQueryID(String queryID) {
        this.queryID = queryID;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public void setBtn_showResults(Button btn_showResults) {
        this.btn_showResults = btn_showResults;
    }

    //constructor
    public SearchedQueryCol(String _queryId, String queryText, Button _showDetails){
        this.queryID = _queryId;
        this.queryText =queryText;
        this.btn_showResults=_showDetails;
        btn_showResults.setText("Show results");
        //handling pressing show results for given query
        btn_showResults.setOnAction(event -> {
            controller.clearSingleQueryResults();
            ObservableList<ResultEntry> rsList=controller.getQueriesResults();
            if(rsList.size()==1 && rsList.get(0).getDocName()=="none")
            {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setContentText("No results found");
                alert.setHeaderText(null);
                alert.showAndWait();
            }
            else {
                for (int i = 0; i < rsList.size(); i++) {
                    if (rsList.get(i).getQueryId().equals(queryID))
                        controller.addToSingleQueryResult(rsList.get(i));
                }
                FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource("/resultsPerQuery.fxml"));
                try {
                    Parent parent = fxmlLoader.load();
                    AView newView = fxmlLoader.getController();
                    newView.setMyController(this.controller);
                    Scene newScene = new Scene(parent, 350, 300);
                    Stage curStage = (Stage) btn_showResults.getScene().getWindow();
                    curStage.setScene(newScene);
                    curStage.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean equals(Object other){
        if(other instanceof SearchedQueryCol){
            SearchedQueryCol otherQuery= (SearchedQueryCol)other;
            return queryID.equals(otherQuery.queryID) && queryText.equals(otherQuery.queryText);
        }
        return false;
    }
}


