package Controller;

import Model.ResultEntry;
import Model.myModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Class Controller as part of the MVC model.
 * Our controller will move functions from the View to the Model and back from the Model to the View.
 */
public class Controller {

    //private field to connect the Controller to the Model
    private myModel model;
    //add for table view performance
    private static Controller singleton = null;
    private ObservableList<ResultEntry> queriesResults;
    private ObservableList<ResultEntry> singleQueryResults;


    //constructor
    public Controller() {
        this.model=myModel.getInstance();
        queriesResults =FXCollections.observableArrayList();
        singleQueryResults=FXCollections.observableArrayList();
    }

    public static Controller getInstance(){
        if (singleton == null)
            singleton = new Controller();
        return singleton;
    }

    public String[] activateProgram() {
        return this.model.activateCorpusIndexer();
    }

    public boolean reset(){
        return this.model.resetIndexing();
    }

    public void updateUserChoices(String dataPath, String dictAndPostPath,boolean useStemming){
        this.model.updateUserChoices(dataPath,dictAndPostPath,useStemming);
    }

    public List<String> showDictionary()
    {
        return this.model.showDictionary();
    }

    public boolean loadDictionaryToMemory(){
        return this.model.loadDataToMemory();
    }

    public ObservableList<String> getLanguages(){
        return model.getLanguages();
    }

    public Set<String> getCities(){
        return model.getCities();
    }

    public void setQueriesResults(ObservableList<ResultEntry> queriesResults) {
        this.queriesResults = queriesResults;
    }

    public ArrayList<ResultEntry> runManualQuery(String query, ObservableList<String> citiesSelection,boolean useSemantic,
                                                 List<String> userLanguagesChoice) {
        return this.model.runManualQuery(query,citiesSelection,useSemantic,userLanguagesChoice);
        /*
        queriesResults = FXCollections.observableArrayList(results);
        return results;
        */
    }

    public ArrayList<ResultEntry> runQueryFile(String path,ObservableList<String> citiesSelection,boolean useSemantic,
                                               List<String> userLanguagesChoice){
        return this.model.runQueryFile(path,citiesSelection,useSemantic,userLanguagesChoice);
        /*
        queriesResults =FXCollections.observableArrayList(results);
        return results;
        */
    }

    //method for table views
    public ObservableList<ResultEntry> getQueriesResults() {
        return queriesResults;
    }

    public ObservableList<ResultEntry> getSingleQueryResults() {
        return singleQueryResults;
    }

    public void clearQueryResults() {
        queriesResults.clear();
    }

    public void clearSingleQueryResults(){this.singleQueryResults.clear();}

    public void addToSingleQueryResult(ResultEntry rs){
        this.singleQueryResults.add(rs);
    }

    public boolean getStemmerValue(){return this.model.getStemmerValue();}

    public boolean getSemanticValue(){return this.model.getSemanticValue();}

    public String getEntities(String docId){return model.getEntities(docId);}
}
