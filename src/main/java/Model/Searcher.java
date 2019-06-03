package Model;

import View.Main;
import javafx.util.Pair;
import sun.reflect.generics.tree.Tree;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class Searcher {

    private Parse myQueryParser;
    private Ranker myRanker;
    private String pathForSearch;

    public Searcher(Parse queryParser, Ranker ranker) {
        this.myQueryParser = queryParser;
        this.myRanker = ranker;
        pathForSearch = null;
    }

    /**
     * sets the searcher path for search to the given path.
     *
     * @param path - the path in which the posting files are being stored.
     */
    public void setPathForSearch(String path) {
        this.pathForSearch = path;
    }

    /**
     * Retrive for a given query the most relevant results up to 50 documents.
     * @param query - the query for which we wish to retrieve relevant results.
     * @param userChoisesOfCity - the user choices of city.
     * @param useStemming - whether or not the stemming in the process.
     * @param userChoiceOfLanguages - the user choices of languages
     * @return a Treemap containing the ending results that should be retrieved to the user.
     */
    public synchronized TreeMap<String,Double> returnRelevantDocuments(String query, List<String> userChoisesOfCity,
                                                                       boolean useStemming,List<String> userChoiceOfLanguages) {
        myQueryParser.parse(query);
        Map<String, String> queryProcessedTokens = myQueryParser.getTemporalPostings();
        ArrayList<Pair<String, String>> queryWordsPostingsData = new ArrayList<>();
        TreeMap<String,Double> documentsOrderByRank=null;
        //loop over each query word
        for (Map.Entry<String, String> mapEntry : queryProcessedTokens.entrySet()) {
            String numOfAppearncesInQuery = mapEntry.getValue();
            //search word in dictionary and extract her posting details if exists.
            if (Indexer.termsDictionary.containsKey(mapEntry.getKey())) {
                String queryWord = mapEntry.getKey();
                long ptrToPosting = Indexer.termsDictionary.get(queryWord).getPtr();
                String wordPostingData = getWordPostingData(queryWord, ptrToPosting, useStemming);
                Pair<String, String> newPair = null;
                if (wordPostingData != null) {
                    newPair = new Pair<>(wordPostingData, numOfAppearncesInQuery);
                    queryWordsPostingsData.add(newPair);
                }
            }
        }
        documentsOrderByRank = myRanker.rankDocuments(myQueryParser, queryWordsPostingsData);
        if(!documentsOrderByRank.isEmpty()) {
            //filter the retrieved documents based on the user choice, if one was made.
            if (userChoisesOfCity != null && !userChoisesOfCity.isEmpty())
                documentsOrderByRank = filterDocumentsByCity(documentsOrderByRank, userChoisesOfCity, useStemming);
            //filter the retrieved documents by user language choice, if one was made.
            if(userChoiceOfLanguages!=null && !userChoiceOfLanguages.isEmpty())
               documentsOrderByRank = filterDocumentsByLanguage(documentsOrderByRank,userChoiceOfLanguages);
            //filter the retrieved documents to the first 50 documents
            if (documentsOrderByRank.size() > 50)
                return myRanker.getFirstEntries(50, documentsOrderByRank);
            return documentsOrderByRank;
        }
        return  null;
    }

    /**
     *Returns the posting data of the given query word.
     * @param queryWord - the word for whom we wish to pull the corresponding posting data.
     * @param ptr - a seek pointer to the cities posting file.
     * @param useStemming - a boolean parameter indicating whether we wish to poll the data indexed with stemming or not.
     * @return posting data in the form of: ear;LA092989-0153:1603*1 LA093089-0103:986*1
     */
    private String getWordPostingData(String queryWord,long ptr,boolean useStemming){
        String wordPostingData=null;
        try {
            RandomAccessFile wordPostingFile;
            if(!useStemming)
                wordPostingFile= new RandomAccessFile(pathForSearch + "\\" + returnDestinatoinPostFile(queryWord), "r");
            else
                wordPostingFile=new RandomAccessFile(pathForSearch + "\\" + returnDestinatoinPostFile(queryWord)+"_stemmed", "r");
            wordPostingFile.seek(ptr);
            wordPostingData = wordPostingFile.readLine();
            wordPostingFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wordPostingData;
    }

    /**
     * Filter the retrieved documents so that only documents attached to one of user city choices will be retrieved.
     * @param documentsOrderByRank - an array of the documents retrieved in order of ranking.
     * @param userCityChoices - a list of cities from which the user wish to see documents.
     * @param useStemming - a boolean indicating whether we data with stemming is requested or without.
     * @return A Tree map containing only the documents and their rank after the cities filtering.
     */
    private TreeMap<String,Double> filterDocumentsByCity(TreeMap<String,Double> documentsOrderByRank,
                                                         List<String> userCityChoices, boolean useStemming) {
        TreeMap<String,Double> documentsAfterCityFilter = new TreeMap<>();
        String documentName, cityPostingData;
        Set<String> allDocumentsForCity = new HashSet<>();
        for (Map.Entry<String,Double> entry: documentsOrderByRank.entrySet()) {
            documentName = entry.getKey();
            for (int j = 0; j < userCityChoices.size(); j++) {
                long ptr = Indexer.citiesDictionary.get(userCityChoices.get(j)).getPtr();
                cityPostingData = getCityPostingData(userCityChoices.get(j), ptr, useStemming);
                getAllDocumentsFromPosting(new StringBuilder(cityPostingData), allDocumentsForCity);
                if (allDocumentsForCity.contains(documentName)) {
                    documentsAfterCityFilter.put(documentName,entry.getValue());
                    break;
                }
            }
        }
        return documentsAfterCityFilter;
    }


    /**
     *Filters the retrived documents so that only documents attached to requested languages are kept.
     * @param documentsOrderByRank - an array of the documents retrieved in order of ranking.
     * @param userChoiceOfLanguages - a list of all languages from which the user wish to see results.
     * @return a tree map containing all the documents and their rank after filtering documents attached to unrequested
     * languages.
     */
    private synchronized TreeMap<String,Double> filterDocumentsByLanguage(TreeMap<String,Double> documentsOrderByRank,
                                                                          List<String> userChoiceOfLanguages) {
        TreeMap<String,Double> documentsAfterLanguageFilter = new TreeMap<>();
        String documentName,docData;
        for(Map.Entry<String,Double> entry : documentsOrderByRank.entrySet()){
            documentName=entry.getKey();
            docData=myRanker.getDocData(documentName);
            String[] temp=Main.split(docData,"|");//TODO:Delete temp
            if (temp.length==8) {
                String docLanguage = temp[7];
                if (docLanguage.equals("null")) {
                    documentsAfterLanguageFilter.put(documentName, entry.getValue());
                } else if (userChoiceOfLanguages.contains(docLanguage))
                    documentsAfterLanguageFilter.put(documentName, entry.getValue());
            }
            else
                System.out.println(docData);

        }
        return documentsAfterLanguageFilter;
    }



    /**
     * adds to the given set of documents attached to a specific city all the documents name.
     * @param postingData- city posting data in the form of: "LONDON;FBIS3-8:219,597,1143*3 FBIS3-9:1095*1"
     * @param allDocumentsForCity- an empty set that should be filled with all the documents name extracted.
     */
    private void getAllDocumentsFromPosting(StringBuilder postingData,Set<String> allDocumentsForCity){
        postingData.substring(postingData.indexOf(";")+1);
        String[] allDocuments=Main.split(postingData.toString()," ");
        for(int i=0;i<allDocuments.length;i++){
            allDocuments[i]=allDocuments[i].substring(0,allDocuments[i].indexOf(":"));
            allDocumentsForCity.add(allDocuments[i]);
        }
    }
    /**
     *Returns the posting data of the given city.
     * @param city - the city for whom we wish to pull the corresponding posting data.
     * @param ptr - a seek pointer to the cities posting file.
     * @param useStemming - a boolean parameter indicating whether we wish to poll the data indexed with stemming or not.
     * @return posting data in the form of: TRIPOLI;FBIS3-4493:-1,93*2
     */
    private String getCityPostingData(String city,long ptr,boolean useStemming){
        String cityPostingData=null;
        try {
            RandomAccessFile cityPostingFile;
            if(useStemming)
                cityPostingFile= new RandomAccessFile(pathForSearch + "\\citiesPosting_stemmed", "r");
            else
                cityPostingFile=new RandomAccessFile(pathForSearch + "\\citiesPosting", "r");
           cityPostingFile.seek(ptr);
            cityPostingData = cityPostingFile.readLine();
            cityPostingFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityPostingData;
    }


    /**
     * TODO: maybe instance of indexer instead
     * Returns the file into we need to write the term and it's posting.
     *
     * @param term - the term for whom we wish to discover the relevant posting.
     * @return - a-z,0-9 or cities according to the prefix of given term.
     */
    private String returnDestinatoinPostFile(String term) {
        char prefix = term.toLowerCase().charAt(0);
        if (!Character.isDigit(prefix) && !Character.isLetter(prefix))
            return "chars";
        return String.valueOf(prefix);
    }
}
