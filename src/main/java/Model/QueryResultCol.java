package Model;

import Controller.Controller;
import View.Main;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

public class QueryResultCol {
    private Controller controller=Controller.getInstance();
    public String docId;
    public String docRank;
    public Button btn_showEntities;


    //getters
    public String getDocId(){
        return docId;
    }

    public String getDocRank(){
        return docRank;
    }

    public Button getBtn_showEntities(){
        return btn_showEntities;
    }

    //setters
    public void setDocId(String docId) {
        this.docId = docId;
    }

    public void setDocRank(String docRank) {
        this.docRank = docRank;
    }

    public void setBtn_showEntities(Button btn_showEntities){
        this.btn_showEntities=btn_showEntities;
    }

    public QueryResultCol(String _docID ,String _docRank,Button _showEntities){
        this.docId = _docID;
        this.docRank=_docRank;
        this.btn_showEntities=_showEntities;
        btn_showEntities.setText("Show Entities");
        //handling pressing show entities for given document
        btn_showEntities.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            String entities = controller.getEntities(docId);
            String[] entitieslist = Main.split(entities, ",");
            StringBuilder content = new StringBuilder();
            content.append("The document most dominant entities are:\n");
            String[] entityAndRank=null;
            if (entitieslist.length>1){
                for (int i = 0; i < entitieslist.length; i++) {
                    entityAndRank = Main.split(entitieslist[i], "*");
                    if(entityAndRank!=null)
                        content.append(entityAndRank[0] +", ranked: "+entityAndRank[1]+ "\n");
                }
                alert.setContentText(content.toString());
                alert.setHeaderText(null);
                alert.show();
            }
            else {
                alert.setContentText("No entities to show");
                alert.setHeaderText(null);
                alert.show();
            }
        });
    }
}
