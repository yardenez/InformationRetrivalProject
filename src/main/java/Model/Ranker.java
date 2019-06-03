package Model;

import View.Main;
import javafx.util.Pair;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Ranker {

    private Map<String,String> documentsDictionary;
    private double k;
    private double b;
    private int avgDocLength;

    public Ranker(){
        k=1.4;//a recommended value range from 1.2 to 2.0.
        b=0.75;
        this.documentsDictionary=new ConcurrentHashMap<>();
        avgDocLength=0;
    }

    /**
     * Ranks all the documents relevant for the query.
     * @param titleParser - a parser instance for the use of parsing the title.
     * @param allQueryPostings - an array containing pairs of posting data of a given term and the number of appearance
     *                         in the query.
     * @return TreeMap containing all the documents and their rank orders by their rank.
     */
    public TreeMap<String,Double> rankDocuments(Parse titleParser, ArrayList<Pair<String,String>> allQueryPostings){
        //documentsRank map will contain for each document in one of the given posting files it's rank.
        Map<String,Double> documentsRank =new HashMap<>();
        StringBuilder docData=new StringBuilder(),docName=new StringBuilder();
        int numAppearancesInQuery=0;
        String[] docsPossibleForRetrival=null;
        for(int i=0; i<allQueryPostings.size(); i++){
            Pair<String,String> pair =allQueryPostings.get(i);
             /*each pair contains as key the posting data of one of the query term and as value the number of
            appearances of the same term in query. for example: <eager;FBIS3-23:19073*1, 2>
            */
            numAppearancesInQuery = Integer.valueOf(pair.getValue());
            docsPossibleForRetrival= getAllDocsOptionalForRetrival(pair.getKey());
            for(int j=0;j<docsPossibleForRetrival.length;j++)
            {
                docData.append(docsPossibleForRetrival[j]);
                docName.append(docData.substring(0,docData.indexOf(":")));
                int P = isQueryWordInTheHeadOfTheDoc(docName.toString(), docData.toString());
                int T = isQueryWordInTitle(titleParser, docName.toString(), pair.getKey());
                int tf=Integer.valueOf(docData.substring(docData.indexOf("*")+1));
                double rank=calculateRank(numAppearancesInQuery,tf,getDocLength(docName),docsPossibleForRetrival.length, T, P);
                if(documentsRank.containsKey(docName.toString()))
                    rank=rank+documentsRank.get(docName.toString());
                //cut to 3 digits after decimal
                rank= Double.parseDouble(new DecimalFormat("###.###").format(rank));
                documentsRank.put(docName.toString(),rank);
                docData.setLength(0);
                docName.setLength(0);
                titleParser.getTemporalPostings().clear();
            }
        }
        TreeMap<String,Double> sortedMapByRank= new TreeMap<>(new RankCompartor(documentsRank));
        sortedMapByRank.putAll(documentsRank);
        return sortedMapByRank;
    }

    /**
     * A function that checks if the query word appears in the first 10% of the document
     * @param docName - the given docName
     * @param docData - the document's details in the dictionary
     * @return - 1 if the query word appears in the first 10% of the document, otherwise 0;
     */
    private int isQueryWordInTheHeadOfTheDoc(String docName, String docData) {
        String docDetails = documentsDictionary.get(docName);
        String docLength = Main.split(docDetails, "|")[5];
        String firstPosition = Main.split(docData, ":|,|*")[1];
        if (firstPosition.equals("-1") && Main.split(docData, ",").length>1)
            firstPosition = Main.split(docData, ",")[1];
        if (!firstPosition.equals("-1")){
            Integer.valueOf(firstPosition);
            if ((Integer.valueOf(firstPosition)/Integer.valueOf(docLength)) <= 0.1)
                return 1;
        }
        return 0;
    }

    /**
     * A function that checks whether a query word appears in the title
     * @param titleParser - needed in order to parse the document's title
     * @param docName - needed in order to find the document's title in the documentDictionary
     * @param queryWordPosting - needed in order to extract the query word
     * @return - 1 if the query word appears in the title, otherwise 0;
     */
    private int isQueryWordInTitle(Parse titleParser, String docName, String queryWordPosting) {
        String docDetails = documentsDictionary.get(docName);
        String title = Main.split(docDetails, "|")[0];
        titleParser.parse(title);
        HashSet<String> wordsInTitle = new HashSet<>(titleParser.getTemporalPostings().keySet());
        int index = queryWordPosting.indexOf(";");
        String queryWord = queryWordPosting.substring(0,index);
        for (String titleWord: wordsInTitle) {
            if (titleWord.toUpperCase().equals(queryWord.toUpperCase()))
                return 1;
        }
        return 0;
    }
    /**
     * computes the rank of a documents based on the BM25 formula.
     * f(q,d)=∑_(w∈q∩d)〖c(w,q)*((k+1)*c(w,d))/(c(w,d)+k(1-b+b+|d|/avgdl))〗*log〖(M+1)/df(w) 〗
     * when: c(w,q)=the number of appearances of the word in query
     *       c(w,d)= the number of appearances in doc.
     *       avgdl- avearage document length
     *       |d|=given doc length
     *       M=number of documents in corpus
     *       b and k are set by recommendations.
     * the BM25 will be 0.9 of the final rank. another 0.05 will be for whether the term appears in the title or not,
     * and another 0.05 for whether the term appears in the first 10% from the document length.
     *
     * @return the result of the rank as if the document contains only the word w.
     *         the results will be summed later for the complete rank.
     */
    private double calculateRank(int numAppearancesInQuery,int tf,int docLength,int df, int title, int position){
        return 0.9*(numAppearancesInQuery*(k+1)*tf/(tf+k*(1-b+b*docLength/avgDocLength)))
                *Math.log(documentsDictionary.size()/df) + 0.05*title +0.05*position;
    }

    /**
     * Returns the length of the given doc.
     * @param DocName- the document for whom we wish to discover the length.
     * @return the given doc's length if the documents dictionary is not empty and -1 otherwise.
     */
    private int getDocLength(StringBuilder DocName){
        if(!documentsDictionary.isEmpty()){
            String docInformation =documentsDictionary.get(DocName.toString());
            return Integer.valueOf(Main.split(docInformation,"|")[5]);
        }
        else
            return -1;
    }

    /**
     * returns an array with all documents in which the word appeared in, icluding additional details.
     * @param wordPostingData- a posting data that belongs to one of the query words.
     *                      for example:"eager;FBIS3-23:19073*1 FBIS3-28:24*1"
     * @return an array containing in each slot posting information in regard to a specific document.
     *          for example:[FBIS3-23:19073*1][FBIS3-28:24*1]
     */
    private String[] getAllDocsOptionalForRetrival(String wordPostingData){
        wordPostingData= wordPostingData.substring(wordPostingData.indexOf(";")+1);
        return Main.split(wordPostingData," ");
    }

    /**
     * Returns the list of entities
     * @param docId - the docId for which we wish to return it's most dominant entity
     * @return a string sequence containing all the entities
     */
    public String getEntitiesOfDoc(String docId) {
        String docData=documentsDictionary.get(docId);
        String[] docDataSplitted = Main.split(docData,"|");
        return docDataSplitted[6];
    }

    /**
     * returns the doc data of a given doc
     * @param docId - the doc we wish the retrieve information for.
     * @return the doc data as saved in the disk.
     */
    public String getDocData(String docId) {
        return documentsDictionary.get(docId);
    }

    /**
     * RankCompartor compars between two documents by their rank.
     */
    public static class RankCompartor implements Comparator<String>{
        private Map<String,Double> map;

        public RankCompartor(Map<String, Double > map)
        {
            this.map=map;
        }

        public int compare(String doc1, String doc2){
            int result=map.get(doc2).compareTo(map.get(doc1));
            if(result==0)
                result=1;
            return result;
        }
    }

    /**
     * Returns the first n elements of a map.
     * @param n- the number of elements we wish to retrieve.
     * @param source - the map from which we wish to filter the number of elements.
     * @return the first n elements of the given map.
     */
    public TreeMap<String,Double> getFirstEntries(int n, TreeMap<String,Double> source){
        int count=0;
        Map<String,Double> target=new LinkedHashMap<>();
        for(Map.Entry<String,Double> entry:source.entrySet()){
            if(count>=n)
                break;
            target.put(entry.getKey(),entry.getValue());
            count++;
        }
        TreeMap<String,Double> finalTarget=new TreeMap<>(new RankCompartor(target));
        finalTarget.putAll(target);
        return finalTarget;
    }


    /**
     * calculates the average length of a document in corpus.
     * At the moment of calculation, the documents dictionary must be in main memory.
     */
    public void calcAverageDocumentsLength(){
        int sumOfLength=0,counter=0;
        for (String docData:documentsDictionary.values()) {
            //the length of a documents will always appear at the end.
            String doclengthTemp=Main.split(docData,"|")[5];
            sumOfLength+= Integer.valueOf(doclengthTemp);
            counter++;
        }
        avgDocLength=sumOfLength/counter;
    }

    /**
     * Loads all documents data from disk to main memory.
     * @param path - the path in which the document dictionary is written.
     */
    public void loadDocsDictionaryToMainMemory(String path){
        try {
            documentsDictionary.clear();
            File documents = new File(path + "\\documents.txt");
            if (documents.exists()) {
                Scanner scanner = new Scanner(documents);
                StringBuilder documentData = new StringBuilder();
                while (scanner.hasNext()) {
                    documentData.append(scanner.nextLine());
                    //extract the document name and it's relevant data.
                    int delimiterIdx = documentData.indexOf(";;");
                    String docName = documentData.substring(0, delimiterIdx);
                    String docData = documentData.substring(delimiterIdx + 2);
                    documentsDictionary.put(docName, docData);
                    documentData.delete(0,documentData.length());
                }
                scanner.close();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     *A function that clears all data structures using in this class
     */
    public void resetData(){
        documentsDictionary=new HashMap<>();
    }
}

