package Model;

import View.Main;
import javafx.collections.ObservableList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class myModel {
    private static myModel singleton = null; /*for table view function*/
    private ReadFile myFileReader;
    public static Parse myDocumentsParser;
    private Parse myQueryParser;
    private Indexer myIndexer;
    private Searcher mySearcher;
    private Ranker myRanker;
    private Boolean useStemming;
    private Boolean useSemantic;
    private List<Pattern> patterns;
    private List<Pattern> descPatterns;

    //singleton
    public static myModel getInstance() {
        if (singleton == null)
            singleton = new myModel();
        return singleton;

    }

    //constructor
    public myModel(){
        myIndexer=new Indexer();
        myDocumentsParser = new Parse();
        myFileReader=new ReadFile();
        myQueryParser= new Parse();
        myRanker= new Ranker();
        mySearcher=new Searcher(myQueryParser,myRanker);
        useStemming=false;
        useSemantic=false;
        patterns=new LinkedList<>();

    }

    /**
     * Returns the list of entities
     * @param docId - the docId for which we wish to return it's most dominant entity
     * @return a string sequence containing all the entities
     */
    public String getEntities(String docId) {
        return myRanker.getEntitiesOfDoc(docId);
    }

    /**
     * updates all the relevant classes instances with the user choices.
     * @param dataPath - the path to corpus.
     * @param dictAndPostPath- the path in which the dictionary and posting files will be/are stored in disk.
     * @param useStemming- the user choice about whether or not he is interested in result with stemming.
     */
    public void updateUserChoices(String dataPath, String dictAndPostPath,boolean useStemming){
        this.useStemming=useStemming;
        myDocumentsParser.setPath(dataPath);
        myDocumentsParser.setUsingStemmer(useStemming);
        myDocumentsParser.setPathForWriting(dictAndPostPath);
        myQueryParser.setUsingStemmer(useStemming);
        myQueryParser.setPath(dataPath);
        myQueryParser.setPathForWriting(dictAndPostPath);
        myFileReader.setCorpus(dataPath);
        myFileReader.setParser(myDocumentsParser);
        myIndexer.setPath(dictAndPostPath);
        myIndexer.setStemming(useStemming);
        mySearcher.setPathForSearch(dictAndPostPath);
    }

    /**
     * Returns all languages which were collected during the inverted index process.
     * @return a list of documents.
     */
    public ObservableList<String> getLanguages(){
        return myFileReader.getLanguages();
    }

    /**
     * Returns all cities which were collected during the inverted index process.
     * @return a Set of cities as String
     */
    public Set<String> getCities() {
        return myIndexer.getCities();
    }

    /**
     * Activates the program on a chosen data set.
     */
    public String[] activateCorpusIndexer(){
        try {
            cleanFilesIfExists(myIndexer.getPath());
            //start building the inverted index
            String[] processData = new String[4];
            long starttime = System.nanoTime();
            myFileReader.readFile();
            myIndexer.createInvertedIndex();
            long endtime = System.nanoTime();
            loadDictionariesToDisk(useStemming);
            //add process requested information
            processData[0] = String.valueOf((endtime - starttime)*1.6667*Math.pow(10,-11));
            processData[1] = String.valueOf(myFileReader.getNumberOfDocuments());
            myFileReader.setNumOfDocuments(0);
            processData[2] = String.valueOf(myIndexer.getNumberOfTerms());//considering that
            myIndexer.clearStructors();
            return processData;
        }catch(Exception e){
            return null;
        }
    }

    /**
     * A function that clears the files in a given path, if exists
     * @param path - the given path
     */
    public void cleanFilesIfExists(String path){
        final File folder = new File(path);
        final File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File pathname, String name) {
                if(myDocumentsParser.getStemmerValue())
                    return name.endsWith("_stemmed");
                else
                    return !name.endsWith("_stemmed");
            }
        });
        for (final File file : files) {
            file.delete();
        }
        File documents= new File(path+"\\documents.txt");
        if(documents.exists())
            documents.delete();

    }

    /**
     * write both dictionaries from main memory to disk
     * @param useStemming- a boolean parameter indicating which
     */
    public void loadDictionariesToDisk(boolean useStemming){
        myIndexer.loadTermsDictionaryIntoDisk(useStemming);
        myIndexer.loadCitiesDictionaryIntoDisk();
        myFileReader.loadLanguagesToDisk(myDocumentsParser.getPathForWriting());
    }

    /**
     * Reset all the system data.
     * posting file(s) and dictionaries will be deleted.
     * all main memory structures will be reset.
     */
    public boolean resetIndexing()
    {
        myDocumentsParser.resetData();
        myFileReader.resetData();
        myRanker.resetData();
        patterns.clear();
        return myIndexer.resetData();
    }

    /**
     *returns the dictionary so it can displayed in GUI.
     * @return a list of all words in dictionary and their tf in corpus
     */
    public List<String> showDictionary()
    {
        return myIndexer.getDictionaryForDisplay(myDocumentsParser.getStemmerValue());
    }

    /**
     *load the dictionaries to memory.
     * @return true if load was made successfully and false otherwise.
     */
    public boolean loadDataToMemory(){
        myFileReader.loadLanguagesToMemory(myDocumentsParser.getPathForWriting());
        myRanker.loadDocsDictionaryToMainMemory(myDocumentsParser.getPathForWriting());
        myRanker.calcAverageDocumentsLength();
        return myIndexer.loadDictionariesToMainMemory(myDocumentsParser.getStemmerValue());
    }

    /**
     * returns the results so each result is converted to a result entry( a form of <queryId,docName,rank>
     * @param query - the query to which we wish to retrieve documents.
     * @param citiesSelection - a list of cities from which the user we wish to see results.
     * @param useSemantic- the user choice about whether or not to add semantic treatment
     * @param userChoiceOfLanguages- a list of languages from which the user wish to see results.
     * @return an arraylist of all the query results in the form of result entry.
     */
    public ArrayList<ResultEntry> runManualQuery(String query,ObservableList<String> citiesSelection,
                                                 boolean useSemantic,List<String> userChoiceOfLanguages) {
        //first,generates a random queryId between 1000 and 2000.
        String queryId=String.valueOf(400 + (int)(Math.random() * ((999 - 400) + 1)));
        return runSingleQuery(query,queryId,citiesSelection,useSemantic,userChoiceOfLanguages);
    }

    /**
     * returns the results of the given queries to the gui.
     * @param path - a path in which a file with a number of queries is located.
     * @param citiesSelection - a list of cities from which the user we wish to see results.
     * @param useSemantic- the user choice about whether or not to add semantic treatment
     * @param userChoiceOfLanguages- a list of languages from which the user wish to see results.
     * @return an arraylist of all the query results in the form of result entry.
     */
    public ArrayList<ResultEntry> runQueryFile(String path,ObservableList<String> citiesSelection,
                                               boolean useSemantic, List<String> userChoiceOfLanguages) {
        compilePatterns(patterns);
        ArrayList<ResultEntry> totalResults=new ArrayList<>();
        try {
            //get all queries in file
            Document doc = Jsoup.parse(new String(Files.readAllBytes(Paths.get(path))));
            Elements queries = doc.getElementsByTag("top");
            //Using threads - one for every query
            List<Thread> threads = new LinkedList<>();
            for (Element queryData : queries) {
                //extract query ID
                String queryId = extractQueryID(queryData);
                //extract actual query
                String query = queryData.select("title").text()+";"+getNarrativeWords(extractNarrative(queryData))+extractDescription(queryData);
                Thread t = new Thread(() -> {
                    totalResults.addAll(runSingleQuery(query, queryId, citiesSelection,useSemantic,userChoiceOfLanguages));
                });
                threads.add(t);
            }
            for (Thread t : threads) {
                t.start();
            }
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (Exception e) { }
            }
        }catch (Exception e){

        }
        Collections.sort(totalResults, new Comparator<ResultEntry>() {
            @Override
            public int compare(ResultEntry o1, ResultEntry o2) {
                return o1.getQueryId().compareTo(o2.getQueryId());
            }
        });
        return totalResults;
    }

    // gets the content inside the description
    private String extractDescription(Element queryData) {
        String description = queryData.select("desc").text();
        if( !(description==null || description.isEmpty())) {
            description = description.substring(13);
            return description;
        }
        return "";
    }

    //gets the content inside the narrative
    private String extractNarrative(Element queryData){
        String narrative = queryData.select("narr").text();
        if( ! (narrative==null || narrative.isEmpty()) ) {
            narrative = narrative.substring(11);
            return narrative;
        }
        return null;
    }

    //gets the query ID from tag <num>
    private String extractQueryID(Element queryData){
        String queryID = queryData.select("num").text();
        queryID = queryID.toString().substring(8, 11);
        return queryID;
    }

    /**
     *returns the results so each result is converted to a result entry( a form of <queryId,docName,rank>
     * @param query - the query to retrieve documents information for.
     * @param queryId - the query Id
     * @param userChoiceOfLanguages- a list of languages from which the user wish to see results
     * @param citiesSelection - a list of cities from which the user we wish to see results.
     * @return Arraylist with all the results converted to resultEntry.
     */
    private ArrayList<ResultEntry> runSingleQuery(String query,String queryId, ObservableList<String> citiesSelection,boolean useSemantic,List<String> userChoiceOfLanguages){
        String queryTitle= Main.split(query,";")[0];
        ArrayList<ResultEntry> results=new ArrayList<>();
        TreeMap<String,Double> queryResults;
        if(useSemantic)
            query+=getSemanticWords(query);
        if(citiesSelection.isEmpty() || userChoiceOfLanguages.isEmpty()) {
            if (citiesSelection.isEmpty())
                queryResults = mySearcher.returnRelevantDocuments(query, null, myQueryParser.getStemmerValue(), userChoiceOfLanguages);
            else
                queryResults = mySearcher.returnRelevantDocuments(query, citiesSelection, myQueryParser.getStemmerValue(), null);
        }
        else
            queryResults = mySearcher.returnRelevantDocuments(query,citiesSelection,myQueryParser.getStemmerValue(),userChoiceOfLanguages);
        if(queryResults!=null) {
            for (Map.Entry<String, Double> singleQueryResult : queryResults.entrySet()) {
                results.add(new ResultEntry(queryId, queryTitle, singleQueryResult.getKey(), singleQueryResult.getValue()));
            }
        }
        else
            results.add(new ResultEntry(queryId,queryTitle,"none",0));
        return results;
    }

    /**
     * Returns whether the user requested treatment with stemming or without
     * @return True-if stemming is requested or false otherwise.
     */
    public boolean getStemmerValue(){
        return useStemming;
    }

    /**
     * Returns whether the user requested semantic treatment or not
     * @return True-if semantic treatment is requested or false otherwise.
     */
    public boolean getSemanticValue(){
        return useSemantic;
    }

    /**
     * The function gets a query and returns all the semantic connected words of every query word
     * @param query - the given query
     * @return - a String with all the semantic words, splited with space
     */
    private String getSemanticWords(String query){
        StringBuffer semanticWords = new StringBuffer();
        String[] wordsInQuery = Main.split(query, " ");
        for (int i = 0; i <wordsInQuery.length; i++) {
            try {
                //working with DataMuse API
                URL url = new URL("https://api.datamuse.com/words?rel_syn=" +wordsInQuery[i]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String input;
                StringBuffer answer = new StringBuffer();
                while ((input = bufferedReader.readLine()) != null)
                    answer.append(input);
                bufferedReader.close();
                //System.out.println(answer.toString());
                connection.disconnect();
                String[] answerAsArray = Main.split(answer.toString(), "{|[|,|\"|:|]|}");
                int numOfSemanticWords = 1;
                for (int j = 0; j < answerAsArray.length && j < numOfSemanticWords; j++){
                    semanticWords.append(answerAsArray[4*j+1]+" ");
                }
            }
            catch (Exception e){

            }
        }
        return " "+semanticWords.toString();
    }

    /**
     * Compile several patterns which represent a sentence about document relevancy.
     * @param patternsList - a list in which we wish to enter all the compiled patterns
     */
    @SuppressWarnings("Duplicates")
    private void compilePatterns(List<Pattern> patternsList) {
        if(patterns.isEmpty()) {
            String pattern1 = "document discussing";
            String pattern2 = "documents discussing";
            String pattern3 = "are relevant.";
            String pattern4 = "is considered relevant.";
            String pattern5 = "relevant documents must contain information on";
            String pattern6 = "relevant documents must contain the following information:";
            String pattern7 = ".";
            String pattern8 = "documents which refer to";
            String pattern9 = "is relevant.";
            String pattern10 = "are considered relevant.";
            //"document discussing....is relevant."
            Pattern p1 = Pattern.compile(Pattern.quote(pattern1) + "(?s)(.*?)" + Pattern.quote(pattern9), Pattern.CASE_INSENSITIVE);
            patternsList.add(p1);
            //"documents discussing....are relevant."
            Pattern p2 = Pattern.compile(Pattern.quote(pattern2) + "(?s)(.*?)" + Pattern.quote(pattern3), Pattern.CASE_INSENSITIVE);
            patternsList.add(p2);
            //"document discussing....is considered relevant."
            Pattern p3 = Pattern.compile(Pattern.quote(pattern1) + "(?s)(.*?)" + Pattern.quote(pattern4), Pattern.CASE_INSENSITIVE);
            patternsList.add(p3);
            //"documents discussing....are considered relevant."
            Pattern p4 = Pattern.compile(Pattern.quote(pattern2) + "(?s)(.*?)" + Pattern.quote(pattern10), Pattern.CASE_INSENSITIVE);
            patternsList.add(p4);
            //"relevant documents must contain information on...."
            Pattern p5 = Pattern.compile(Pattern.quote(pattern5) + "(?s)(.*?)" + Pattern.quote(pattern7), Pattern.CASE_INSENSITIVE);
            patternsList.add(p5);
            //"relevant documents must contain the following information:..."
            Pattern p6 = Pattern.compile(Pattern.quote(pattern6) + "(?s)(.*?)" + Pattern.quote(pattern7), Pattern.CASE_INSENSITIVE);
            patternsList.add(p6);
            //"documents which refer to....are relevant";
            Pattern p7 = Pattern.compile(Pattern.quote(pattern8) + "(?s)(.*?)" + Pattern.quote(pattern3), Pattern.CASE_INSENSITIVE);
            patternsList.add(p7);
        }
    }

    /**
     *loop over each compiled pattern and returns relevant words extracted from the narrative based on the pattern.
     * @param narrative - the narrative extracted from the query
     * @return a string or containing all the relevant words extracted or "" if no match was found.
     */
    private String getNarrativeWords(String narrative){
        if(narrative!=null) {
            Matcher m;
            for (int i = 0; i < patterns.size(); i++) {
                m = patterns.get(i).matcher(narrative);
                if (m.find())
                    return m.group(1);
            }
            return "";
        }
        return "";
    }
}
