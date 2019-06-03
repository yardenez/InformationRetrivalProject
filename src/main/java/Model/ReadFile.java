package Model;

import View.Main;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static Model.Indexer.citiesDictionary;

/**
 * Reads the documents pool and sends each document to the parser for following processing.
 */
public class ReadFile {
    private File corpus;
    private int numOfParsedDocs; //number of documents to parse until writing to disk.
    private Parse parser;
    private Queue<Doc> documents;
    private HashSet<String> languages;
    private int numOfDocuments; //number of documents in corpus

    public ReadFile(){
        numOfParsedDocs=0;//counts number of docs parsed so when arriving threshold, temporal dictionary will be cleared.
        documents = new LinkedList<>();
        languages = new HashSet<>();
        numOfDocuments = 0;
    }

    /**
     * Sets the parser of the class to be the given parser.
     * @param parser - a Parse instance to which we wish to send all documents for parsing.
     */
    public void setParser(Parse parser)
    {
        this.parser=parser;
    }

    /**
     * sets the the corpus to the corpus which is reachable in the given path.
     * @param path - the path of the corpus.
     */
    public void setCorpus(String path){

        this.corpus =new File(path);
    }
    /**
     * reads all the file from the giving corpus and send them to parse one by one.
     */
    public void readFile () throws Exception{
        File[] filesList=null;
        Elements listOfDocs=null;
        Doc currentDoc = null;
        File[] corpusFiles=corpus.listFiles();
        for(int i=0;i<corpusFiles.length;i++){
            File fileEntry=corpusFiles[i];
            if(fileEntry.isDirectory()) {
                filesList = fileEntry.listFiles();
                //for(int m=0;m<filesList.length;m++)
                listOfDocs = extractSubDocuments(filesList[0].getAbsolutePath()); //filesList[i]
                if (listOfDocs != null)
                    for (int j = 0; j < listOfDocs.size(); j++) {
                        Element document = listOfDocs.get(j);
                        if (document != null) {
                            //extract document details
                            currentDoc =  extractDocumentDetails(document);
                            //adding city to the cities dictionary
                            if (currentDoc.getCity()!= null && !citiesDictionary.containsKey(currentDoc.getCity()))
                                citiesDictionary.put(currentDoc.getCity(), new CityTerm(currentDoc.getCity()));
                            if (currentDoc.getLanguage() != null && !languages.contains(currentDoc.getLanguage()))
                                languages.add(currentDoc.getLanguage());
                            if(document.select("text").size() > 0) {
                                //extract the text included between the tags <text></text>
                                parser.setCurrentDoc(currentDoc);
                                //parse also the title if exists
                                String text = document.getElementsByTag("text").toString();
                                if (text.length() > 7)
                                    text = text.substring(7);
                                if(currentDoc.getTitle()!=null)
                                    parser.parse(currentDoc.getTitle()+" "+text);
                                else
                                    parser.parse(document.getElementsByTag("text").toString());
                                numOfParsedDocs++;
                            }

                            documents.add(currentDoc);
                            //arriving threshold
                            if (numOfParsedDocs == 500) {
                                parser.clearTemporalPostings();
                                writeDocumentsListToDisk(parser.getPathForWriting());
                                numOfParsedDocs = 0;
                            }
                        }
                    }
            }
        }
        numOfParsedDocs=0;
        parser.clearTemporalPostings();
        writeDocumentsListToDisk(parser.getPathForWriting());
    }

    /**
     * extracts all sub documents which are separated by the tags <Doc></Doc> within a given doc.
     * @param path - a path for a specific document which possibly contain sub documents.
     * @return all elements when each element represent a specific document.
     */
    private Elements extractSubDocuments(String path){
        try {
            Document doc = Jsoup.parse(new String(Files.readAllBytes( Paths.get(path))));
            return doc.getElementsByTag("doc");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * returns the number of documents extracted during all process.
     * @return the number of documents which passed a parse process.
     */
    public int getNumberOfDocuments() {
        return numOfDocuments;
    }

    /**
     * sets the number of documents extracted to the given number.
     * @param numOfDocuments- the number of documents we wish to reassign to the field.
     */
    public void setNumOfDocuments(int numOfDocuments) {
        this.numOfDocuments = numOfDocuments;
    }

    /**
     * get all languages extracted from the documents in the corpus.
     * @return an Observable list containing all the languages.
     */
    public ObservableList<String> getLanguages() {
        ObservableList<String> listOfLanguages = FXCollections.observableArrayList();
        listOfLanguages.addAll(languages);
        FXCollections.sort(listOfLanguages, new Indexer.StringComparator());
        return listOfLanguages;
    }

    /**
     * resets the structures of ReadFile
     */
    public void resetData(){
        documents = new LinkedList<>();
        languages = new HashSet<>();
        numOfDocuments = 0;
    }

    /**
     * Write all the documents extracted so far to the disk.
     * each document is written with it's relevant information: name, title, date, city, max tf, num of unique terms and
     * length(not including stop words).
     */
    private void writeDocumentsListToDisk(String pathForWriting) {
        if (!documents.isEmpty()) {
            StringBuilder documentsData=new StringBuilder();
            BufferedWriter writerDocuments = null;
            try {
                writerDocuments = new BufferedWriter(new FileWriter(pathForWriting + "\\documents.txt",true));
                // writerDocuments.write("doc_name:title|date|city|max_tf|numOfUniqueTerms|length|entities" + "\n");
                Doc document = null;
                while (!documents.isEmpty()) {
                    document = documents.poll();
                    if (writerDocuments != null) {
                        documentsData.append(document.getName() + ";;" + document.getTitle() + "|" + document.getDate() + "|" + document.getCity()
                                + "|" + document.getMaxFrequency() + "|" + document.getNumOfUniqueTerms() + "|" + document.getLength() +"|"+document.getEntitiesList()+ "|"+document.getLanguage()+ "\n");
                        numOfDocuments++;
                    }
                }
                writerDocuments.write(documentsData.toString());
                writerDocuments.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            documents.clear();
        }
    }

    /**
     * Writes all the languages found into a file named: "languages.txt" in the given path.
     * @param pathForWriting- the path in which we want to save the languages file.
     */
    public void loadLanguagesToDisk(String pathForWriting){
        if (!languages.isEmpty()) {
            StringBuilder languagesData=new StringBuilder();
            BufferedWriter writeLanguages = null;
            try {
                writeLanguages = new BufferedWriter(new FileWriter(pathForWriting + "\\languages.txt",true));
                for (String language:languages) {
                    languagesData.append(language+"\n");
                }
                if(writeLanguages!=null){
                    writeLanguages.write(languagesData.toString());
                }
                writeLanguages.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            languages.clear();
        }
    }

    /**
     *Load the languages to Main memory from the "languages.txt" file at the given path
     * @param pathForWriting - the path in which the file is at.
     */
    @SuppressWarnings("Duplicates")
    public boolean loadLanguagesToMemory(String pathForWriting) {
        try {
            languages.clear();
            File languagesFile=new File(pathForWriting+"\\languages.txt");
            if (languagesFile.exists()) {
                Scanner scanner = new Scanner(languagesFile);
                while (scanner.hasNext()) {
                    languages.add(scanner.nextLine());
                }
                scanner.close();
            }
            else
                return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * A function that gets a document and returns a Doc object with the details from the document
     * @param document - the given document
     * @return new Doc with name, date, title and city
     */
    private Doc extractDocumentDetails(Element document) {
        Doc doc = new Doc();
        //set name
        doc.setName(document.getElementsByTag("docno").toString().split("  | |\n")[2]);
        //set date
        String dateByTag = document.getElementsByTag("date1").toString();
        if (dateByTag != null && !dateByTag.isEmpty() && dateByTag != ""){
            String[] date = Main.split(dateByTag, " |\n");
            doc.setDate(date[1]+" "+date[2]+" "+date[3]);
        }
        if (doc.getDate() == null){
            dateByTag = document.getElementsByTag("date").toString();
            if (dateByTag != null && !dateByTag.isEmpty() && dateByTag != ""){
                String[] date = Main.split(dateByTag, " |\n");
                doc.setDate(date[1]);
                if (doc.getDate().equals("<p>")){
                    date = Main.split(dateByTag, ",|  | |\n");
                    doc.setDate(date[2]+" "+date[3]+" "+date[4]);
                }
            }
        }
        //set title
        String titleByTag = document.getElementsByTag("ti").toString();
        if (titleByTag != null && !titleByTag.isEmpty() && titleByTag != "" && titleByTag != " "){
            String[] title = Main.split(titleByTag, " |\n");
            StringBuilder s_b_title = new StringBuilder();
            for (int i = 1; i < title.length-1; i++){
                s_b_title.append(title[i]+" ");
            }
            if (s_b_title != null && s_b_title.toString()!=null && !s_b_title.toString().isEmpty() && s_b_title.toString() != "" && s_b_title.toString() != " "){
                doc.setTitle(s_b_title.toString().substring(0, s_b_title.toString().length()-1));
            }
        }
        else {
            titleByTag = document.getElementsByTag("headline").toString();
            if (!document.getElementsByTag("p").toString().isEmpty()){
                titleByTag = document.getElementsByTag("headline").toString();
                if (titleByTag != null && !titleByTag.isEmpty() && titleByTag != "" && titleByTag != " "){
                    String[] separatedTitle = Main.split(titleByTag,"<p>|;");
                    StringBuilder s_b_title = new StringBuilder();
                    s_b_title.append(separatedTitle[2]);
                    if (s_b_title != null && s_b_title.toString() != null && !s_b_title.toString().isEmpty() && s_b_title.toString() != "" && s_b_title.toString() != " ")
                        doc.setTitle(s_b_title.toString().substring(1));
                }
            }
            else {
                if (titleByTag != null && !titleByTag.isEmpty() && titleByTag != "" && titleByTag != " ") {
                    String[] separateDateAndTitle = Main.split(titleByTag, "/|\n");
                    StringBuilder s_b_title = new StringBuilder();
                    for (int i = 2; 1 < separateDateAndTitle.length && !separateDateAndTitle[i].equals("<"); i++)
                        s_b_title.append(separateDateAndTitle[i] + " ");
                    if (s_b_title != null && s_b_title.toString() != null && !s_b_title.toString().isEmpty() && s_b_title.toString() != "" && s_b_title.toString() != " ") {
                        doc.setTitle(s_b_title.toString().substring(1, s_b_title.toString().length() - 2));
                    }
                }
            }
        }
        //set city
        String cityByTag = document.select("f").select("[p=104]").toString();
        if (cityByTag != null && !cityByTag.isEmpty() && cityByTag != "" ){
            String[] city = Main.split(cityByTag, " |\n");
            if (!city[1].equals("p=\"104\"></f>") ){
                String cityName=parser.clearDelimiters(Main.split(cityByTag, " |\n")[2].toUpperCase());
                if(!cityName.isEmpty() && !cityName.startsWith("0") && !cityName.startsWith("1")) {
                    doc.setCity(cityName);
                    String postingData = null;
                    //if the city doesn't exists in temporal posting
                    if (!parser.getTemporalPostings().containsKey(doc.getCity())) {
                        postingData = doc.getName() + ":-1*1";
                        parser.getTemporalPostings().put(doc.getCity(), postingData);
                    }
                    //the city exist in the temporal posting
                    else {
                        StringBuilder builder = new StringBuilder();
                        postingData = parser.getTemporalPostings().get(doc.getCity());
                        builder.append(postingData + " " + doc.getName() + ":-1*1");
                        parser.getTemporalPostings().put(doc.getCity(), builder.toString());
                    }
                }
            }
        }
        //set language
        String languageByTag = document.select("f").select("[p=105]").toString();
        if (languageByTag != null && !languageByTag.isEmpty() && languageByTag != "" ){
            String[] language = Main.split(languageByTag, " |\n");
            if (!language[1].equals("p=\"105\"></f>") &&
                    !parser.isTokenANumber(parser.clearDelimiters(Main.split(languageByTag, " |\n")[2].toUpperCase())))
                doc.setLanguage(parser.clearDelimiters(Main.split(languageByTag, " |\n")[2].toUpperCase()));
        }
        return doc;
    }

}
