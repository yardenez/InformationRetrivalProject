package Model;

import javax.xml.transform.Result;

/**
 * Result entry represent a single record in the query results.
 */
public class ResultEntry {

    private String queryId;
    private String DocName;
    private String queryText;
    private double rank;


    //constructor
    public ResultEntry(String queryId,String queryText,String DocName, double rank){
        this.queryText=queryText;
        this.queryId=queryId;
        this.DocName=DocName;
        this.rank=rank;
    }

    /**Getters and Setters */
    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getDocName() {
        return DocName;
    }

    public void setDocName(String docName) {
        DocName = docName;
    }

    public double getRank() {
        return rank;
    }

    public void setRank(double rank) {
        this.rank = rank;
    }

    public String getQueryText() { return queryText; }

    public void setQueryText(String queryText) { this.queryText = queryText; }
}
