package Model;

import java.io.*;
import java.util.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jsoup.nodes.Element;
import View.Main;

import static Model.Indexer.citiesDictionary;

public class Parse {

    private Set<String> stopWordsSet;
    private Map<String,String> months;
    private Map<String, String> temporalPostings;//Map between a term and the documents in which it appeared.
    private int pos; //represent a position in the current doc
    private Boolean useStemming;
    private Stemmer stemmer;
    private String pathForWriting;
    private Doc currentDoc;
    public static int postingFileId=0;

    //constructor
    public Parse() {
        stopWordsSet=new HashSet<>();
        temporalPostings = new HashMap<>();
        currentDoc = null;
        pos=1;
        intiallizeMonthsData();
    }


    /**
     * returns whether stemming was requested or not.
     * @return - true- if stemming was requested and false otherwise.
     */
    public boolean getStemmerValue(){
        return this.useStemming;
    }
    //sets the path for writing to disk.
    public void setPathForWriting(String path){
        pathForWriting=path;
    }

    //sets the path to stop-word list
    public void setPath(String path)
    {
        loadStopWordsList(path);
    }

    //getter for pathForWriting
    public String getPathForWriting() {
        return pathForWriting;
    }

    //setter for currentDoc
    public void setCurrentDoc(Doc currentDoc) {
        this.currentDoc = currentDoc;
    }

    //sets the boolean that says if we want to use stemming or not
    public void setUsingStemmer(boolean useStemming) {
        if(useStemming)
            stemmer = new Stemmer();
        this.useStemming = useStemming;
    }

    //getter for temporal posting
    public Map<String, String> getTemporalPostings(){
        return temporalPostings;
    }

    /**
     * loads the given stop words list into a hash set for further search.
     * @param path - an absolute path in which the stop words text can be found.
     *             assumptions: 1. the stop words will be saved in a text file named:'stop_words'
     *             2. each line will contain one word only.
     */
    private void loadStopWordsList(String path) {
        File file = new File(path + "\\stop_words.txt");
        Scanner sc = null;
        try {
            sc = new Scanner(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (sc != null)
            stopWordsSet = new HashSet<>();
        while (sc.hasNextLine())
            // add each stop words into the hash set
            stopWordsSet.add(sc.nextLine());
        sc.close();
    }

    /**
     * resets the structures of Parser
     */
    public void resetData(){
        temporalPostings = new HashMap<>();
        postingFileId = 0;
    }
    /**
     * clears the temporal dictionary each specific number of documents and writes to a posting file all the data collected so far.
     * !At the end of this process the value of postingFileId represents the number of files created so far in disk.
     */
    public void clearTemporalPostings() {
        TreeMap<String, String> termsDiscovered = new TreeMap<>(new Indexer.StringComparator());
        termsDiscovered.putAll(temporalPostings);
        try {
            //Create the temporal posting file
            BufferedWriter writer = new BufferedWriter(new FileWriter(pathForWriting + "\\file_" + postingFileId));
            for (Map.Entry<String, String> entry : termsDiscovered.entrySet())
                writer.write(entry.getKey()+";"+entry.getValue()+"\n");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        postingFileId++;
        temporalPostings.clear();
    }

    /**
     * The main function in this class. The function gets a text, splits it word by word, parse every word by different parsing lows,
     * collect information about the word and adds it to temporal posting map.
     * @param text - thr given text.
     */
    public void parse(String text) {
        Map<String, Double> entitiesDiscoveredInDoc = new HashMap<>();
        String[] tokens = text.split("\\?|\\||\\]|\\[|- |\"|''|--|\\*|\\=|\\:|\\+| |\\|\n|\\#|\\@|\t|\\<|\\>|;|\\(|\\{|\\}|\\)");
        int index = 0;
        pos = 0;
        String processedToken = "";
        String token = "";
        String nextToken = "";
        String secondNextToken = "";
        String thirdNextToken = "";
        //loop over each token and parse it.
        while (index < tokens.length) {
            if (isValidToken(clearDelimiters(tokens[index]))) {
                if (stopWordsSet.contains(clearDelimiters(tokens[index].toLowerCase()))) {
                    pos++;
                    index++;
                    continue;
                } else {
                    token = clearDelimiters(tokens[index]);
                    /* get the next three tokens*/
                    if (index + 1 < tokens.length && isValidToken(clearDelimiters(tokens[index + 1])))
                        nextToken = clearDelimiters(tokens[index + 1]);
                    if (index + 2 < tokens.length && isValidToken(clearDelimiters(tokens[index + 2])))
                        secondNextToken = clearDelimiters(tokens[index + 2]);
                    if (index + 3 < tokens.length && isValidToken(clearDelimiters(tokens[index + 3])))
                        thirdNextToken = clearDelimiters(tokens[index + 3]);

                    /*now start parsing process*/
                    //ranges and expressions

                    //number fraction-number fraction
                    if (isTokenANumber(token) && nextToken.contains("-") && isTokenAFracture(secondNextToken)) {
                        int idx = nextToken.indexOf("-");
                        if (isTokenAFracture(nextToken.substring(0, idx)) && isTokenANumber(nextToken.substring(idx + 1)))
                            processedToken = token + " " + nextToken + " " + secondNextToken;
                        index = index + 2;
                    }
                    //number-number, word-word,word-word-word,nubmer-word, word-number
                    else if (token.contains("-")) {
                        processedToken = token;
                        if (Character.isLetter(token.charAt(0)) && Character.isUpperCase(token.charAt(0))) {
                            processedToken = token.toUpperCase();
                        } else/* if (Character.isLetter(token.charAt(0))&& Character.isLowerCase(token.charAt(0)))*/ {
                            processedToken = token.toLowerCase();
                        }
                    }
                    //between number and number
                    else if ((token.equals("between") || (token.equals("Between"))) && isTokenANumber(nextToken)
                            && ((secondNextToken.equals("And") || secondNextToken.equals("and")) && isTokenANumber(thirdNextToken))) {
                        processedToken = nextToken + "-" + thirdNextToken;
                        index = index + 3;
                    }

                    //Months

                    //DD Month
                    else if (months.containsKey(nextToken) && isTokenANumber(token) && !token.contains(".") && !token.contains(",") && Integer.valueOf(token) >= 1 && Integer.valueOf(token) <= 31) {
                        if (token.length() == 1)
                            token = "0" + token;
                        processedToken = months.get(nextToken) + "-" + token;
                        index++;
                    }
                    //Month DD
                    else if (months.containsKey(token) && isTokenANumber(nextToken) && !nextToken.contains(".") && !nextToken.contains(",") && Integer.valueOf(nextToken) >= 1 && Integer.valueOf(nextToken) <= 31) {
                        if (nextToken.length() == 1)
                            nextToken = "0" + nextToken;
                        processedToken = months.get(token) + "-" + nextToken;
                        index++;
                    }
                    //yyyy month
                    else if (months.containsKey(nextToken) && isTokenANumber(token) && !token.contains(".") && !token.contains(",") && token.length() == 4) {
                        processedToken = token + "-" + months.get(nextToken);
                        index++;
                    }
                    //month yyyy
                    else if (months.containsKey(token) && isTokenANumber(nextToken) && !nextToken.contains(".") && !nextToken.contains(",") && nextToken.length() == 4) {
                        processedToken = nextToken + "-" + months.get(token);
                        index++;
                    }
                    //Prices

                    //Price over million Dollars

                    //Price Dollars
                    else if (isTokenANumber(token) && !isTokenUnderMillion(token) && isTokenADollarForm(nextToken)) {
                        float number = Float.valueOf(numberWithoutComma(token));
                        if (number % 1000000 == 0)
                            processedToken = String.valueOf((int) (number / 1000000)) + " M Dollars";
                        else
                            processedToken = String.valueOf(number / 1000000) + " M Dollars";
                        index++;
                    }
                    //$price million
                    else if (token.startsWith("$") && isTokenANumber(token.substring(1)) && isTokenAMillionForm(nextToken)) {
                        processedToken = numberWithoutComma(token.substring(1)) + " M Dollars";
                        index++;
                    }
                    //$price billion
                    else if (token.startsWith("$") && isTokenANumber(token.substring(1)) && isTokenABillionForm(nextToken)) {
                        float number = Float.valueOf(numberWithoutComma(token.substring(1)));
                        number = number * 1000;
                        //(number-(int)number)- get the decimal part
                        if ((number - (int) number) == 0)
                            processedToken = String.valueOf(((int) number)) + " M Dollars";
                        else
                            processedToken = String.valueOf(number) + " M Dollars";
                        index++;
                    }
                    //$price trillion
                    else if (token.startsWith("$") && isTokenANumber(token.substring(1)) && isTokenATrillionForm(nextToken)) {
                        float number = Float.valueOf(numberWithoutComma(token.substring(1)));
                        number = number * 1000000;
                        //(number-(int)number)- get the decimal part
                        if ((number - (int) number) == 0)
                            processedToken = String.valueOf(((int) number)) + " M Dollars";
                        else
                            processedToken = String.valueOf(number) + " M Dollars";
                        index++;
                    }
                    //$price
                    else if (token.startsWith("$") && isTokenANumber(token.substring(1)) && !isTokenUnderMillion(token.substring(1))) {
                        float number = Float.valueOf(numberWithoutComma(token.substring(1)));
                        if (number % 1000000 == 0)
                            processedToken = String.valueOf((int) (number / 1000000)) + " M Dollars";
                        else
                            processedToken = String.valueOf(number / 1000000) + " M Dollars";
                    }
                    //Price m Dollars
                    else if (isTokenANumber(token) && isTokenAMillionForm(nextToken) && isTokenADollarForm(secondNextToken)) {
                        processedToken = numberWithoutComma(token) + " M Dollars";
                        index += 2;
                    }
                    //Price bn Dollars
                    else if (isTokenANumber(token) && isTokenABillionForm(nextToken) && isTokenADollarForm(secondNextToken)) {
                        float number = Float.valueOf(numberWithoutComma(token));
                        number = number * 1000;
                        //(number-(int)number)- get the decimal part
                        if ((number - (int) number) == 0)
                            processedToken = String.valueOf(((int) number)) + " M Dollars";
                        else
                            processedToken = String.valueOf(number) + " M Dollars";
                        index += 2;
                    }
                    //Price trillion Dollars - not must
                    else if (isTokenANumber(token) && isTokenATrillionForm(nextToken) && isTokenADollarForm(secondNextToken)) {
                        float number = Float.valueOf(numberWithoutComma(token));
                        number = number * 1000000;
                        //(number-(int)number)- get the decimal part
                        if ((number - (int) number) == 0)
                            processedToken = String.valueOf(((int) number)) + " M Dollars";
                        else
                            processedToken = String.valueOf(number) + " M Dollars";
                        index += 2;
                    }
                    //Price million U.S. Dollars
                    else if (isTokenANumber(token) && isTokenAMillionForm(nextToken) && isTokenADollarForm(secondNextToken + " " + thirdNextToken)) {
                        processedToken = numberWithoutComma(token) + " M Dollars";
                        index += 3;
                    }
                    //Price billion U.S. Dollars
                    else if (isTokenANumber(token) && isTokenABillionForm(nextToken) && isTokenADollarForm(secondNextToken + " " + thirdNextToken)) {
                        float number = Float.valueOf(numberWithoutComma(token));
                        number = number * 1000;
                        //(number-(int)number)- get the decimal part
                        if ((number - (int) number) == 0)
                            processedToken = String.valueOf(((int) number)) + " M Dollars";
                        else
                            processedToken = String.valueOf(number) + " M Dollars";
                        index += 3;
                    }
                    //Price trillion U.S. Dollars
                    else if (isTokenANumber(token) && isTokenATrillionForm(nextToken) && isTokenADollarForm(secondNextToken + " " + thirdNextToken)) {
                        float number = Float.valueOf(numberWithoutComma(token));
                        number = number * 1000000;
                        //(number-(int)number)- get the decimal part
                        if ((number - (int) number) == 0)
                            processedToken = String.valueOf(((int) number)) + " M Dollars";
                        else
                            processedToken = String.valueOf(number) + " M Dollars";
                        index += 3;
                    }

                    //Prices under million Dollars

                    //Price Dollars
                    else if (isTokenANumber(token) && isTokenUnderMillion(token) && isTokenADollarForm(nextToken)) {
                        processedToken = token + " Dollars";
                        index++;
                    }
                    //Price fraction Dollars
                    else if (isTokenANumber(token) && isTokenUnderMillion(token) && isTokenAFracture(nextToken) && isTokenADollarForm(secondNextToken)) {
                        processedToken = token + " " + nextToken + " Dollars";
                        index += 2;
                    }
                    //$price
                    else if (token.startsWith("$") && isTokenANumber(token.substring(1)) && isTokenUnderMillion(token.substring(1)))
                        processedToken = token.substring(1) + " Dollars";
                        //Price U.S. dollars - not must (docNo FBIS3-26563)
                    else if (isTokenANumber(token) && isTokenUnderMillion(token) && isTokenADollarForm(nextToken + " " + secondNextToken)) {
                        processedToken = token + " Dollars";
                    }

                    //Percentages

                    //Number%
                    else if (token.endsWith("%") && isTokenANumber(token.substring(0, token.length() - 1)))
                        processedToken = numberWithoutComma(token.substring(0, token.length() - 1)) + '%';

                        //Number percent || Number percentage
                    else if (isTokenANumber(token) && isTokenAPercentageForm(nextToken)) {
                        processedToken = numberWithoutComma(token) + "%";
                        index++;
                    }
                    //Number fracture percentage - not must
                    else if (isTokenANumber(token) && isTokenAFracture(nextToken) && isTokenAPercentageForm(secondNextToken)) {
                        processedToken = numberWithoutComma(token) + " " + nextToken + " " + secondNextToken;
                        index += 2;
                    }

                    //Kilometers

                    //Number kilometer
                    else if (isTokenANumber(token) && isTokenAKilometerForm(nextToken)) {
                        processedToken = numberWithoutComma(token) + " km";
                        index++;
                    }
                    //Number million kilometer
                    else if (isTokenANumber(token) && isTokenAMillionForm(nextToken) && isTokenAKilometerForm(secondNextToken)) {
                        processedToken = numberWithoutComma(token) + "M km";
                        index += 2;
                    }
                    //Number billion kilometer
                    else if (isTokenANumber(token) && isTokenABillionForm(nextToken) && isTokenAKilometerForm(secondNextToken)) {
                        processedToken = numberWithoutComma(token) + "B km";
                        index += 2;
                    }
                    //Number trillion kilometer
                    else if (isTokenANumber(token) && isTokenATrillionForm(nextToken) && isTokenAKilometerForm(secondNextToken)) {
                        float number = Float.valueOf(numberWithoutComma(token)) * 1000;
                        if ((number - (int) number) == 0)
                            processedToken = String.valueOf(((int) number)) + "B km";
                        else
                            processedToken = String.valueOf(number) + "B km";
                        index += 2;
                    }
                    //Number square kilometer
                    else if (isTokenANumber(token) && nextToken.equals("square") && isTokenAKilometerForm(secondNextToken)) {
                        processedToken = numberWithoutComma(token) + " " + nextToken + " km";
                        index += 2;
                    }

                    //Kilograms

                    //Number kilogram
                    else if (isTokenANumber(token) && isTokenAKilogramForm(nextToken)) {
                        processedToken = numberWithoutComma(token) + " kg";
                        index++;
                    }
                    //Number million kilogram
                    else if (isTokenANumber(token) && isTokenAMillionForm(nextToken) && isTokenAKilogramForm(secondNextToken)) {
                        processedToken = numberWithoutComma(token) + "M kg";
                        index += 2;
                    }
                    //Number billion kilogram
                    else if (isTokenANumber(token) && isTokenABillionForm(nextToken) && isTokenAKilogramForm(secondNextToken)) {
                        processedToken = numberWithoutComma(token) + "B kg";
                        index += 2;
                    }
                    //Number trillion kilogram
                    else if (isTokenANumber(token) && isTokenATrillionForm(nextToken) && isTokenAKilogramForm(secondNextToken)) {
                        float number = Float.valueOf(numberWithoutComma(token)) * 1000;
                        if ((number - (int) number) == 0)
                            processedToken = String.valueOf(((int) number)) + "B kg";
                        else
                            processedToken = String.valueOf(number) + "B kg";
                        index += 2;
                    }

                    //Numbers

                    //Numbers above 1,000

                    //Numbers between 1,000 to 1,000,000

                    //Number in form of xx,xxx or xx,xxx.xx or xxxx or xxxx.xx
                    else if (isTokenANumber(token) && Float.valueOf(numberWithoutComma(token)) >= 1000 && Float.valueOf(numberWithoutComma(token)) < 1000000) {
                        float number = Float.valueOf(numberWithoutComma(token));
                        number = number / 1000;
                        if ((number - (int) number) == 0)
                            processedToken = String.valueOf(((int) number)) + "K";
                        else
                            processedToken = String.valueOf(number) + "K";
                    }
                    //Number in form of xxx thousand or xxx.xx thousand
                    else if (isTokenANumber(token) && isTokenAThousandForm(nextToken)) {
                        processedToken = numberWithoutComma(token) + "K";
                        index++;
                    }

                    //Numbers between 1,000,000 to 1,000,000,000

                    //Number in form of xx,xxx,xxx or xx,xxx,xxx.xx or xxxxxxx or xxxxxxx.xx
                    else if (isTokenANumber(token) && Float.valueOf(numberWithoutComma(token)) >= 1000000 && Float.valueOf(numberWithoutComma(token)) < 1000000000) {
                        float number = Float.valueOf(numberWithoutComma(token));
                        number = number / 1000000;
                        if ((number - (int) number) == 0)
                            processedToken = String.valueOf(((int) number)) + "M";
                        else
                            processedToken = String.valueOf(number) + "M";
                    }
                    //Number in form of xxx million or xxx.xx million
                    else if (isTokenANumber(token) && isTokenAMillionForm(nextToken)) {
                        processedToken = numberWithoutComma(token) + "M";
                        index++;
                    }

                    //Numbers above billion

                    //Number in form of xx,xxx,xxx,xxx or xx,xxx,xxx,xxx.xx or xxxxxxxxxx or xxxxxxxxxx.xx
                    else if (isTokenANumber(token) && Float.valueOf(numberWithoutComma(token)) >= 1000000000) {
                        float number = Float.valueOf(numberWithoutComma(token));
                        number = number / 1000000000;
                        if ((number - (int) number) == 0)
                            processedToken = String.valueOf(((int) number)) + "B";
                        else
                            processedToken = String.valueOf(number) + "B";
                    }
                    //Number in form of xxx billion or xxx.xx billion
                    else if (isTokenANumber(token) && isTokenABillionForm(nextToken)) {
                        processedToken = numberWithoutComma(token) + "B";
                        index++;
                    }
                    //Number in form of xxx trillion or xxx.xx trillion
                    else if (isTokenANumber(token) && isTokenATrillionForm(nextToken)) {
                        float number = Float.valueOf(numberWithoutComma(token));
                        number = number * 1000;
                        if ((number - (int) number) == 0)
                            processedToken = String.valueOf(((int) number)) + "B";
                        else
                            processedToken = String.valueOf(number) + "B";
                        index++;
                    }

                    //Numbers under 1000

                    //Number in form of number fracture
                    else if (isTokenANumber(token) && Float.valueOf(numberWithoutComma(token)) >= 0 && Float.valueOf(numberWithoutComma(token)) < 1000 && isTokenAFracture(nextToken)) {
                        processedToken = token + " " + nextToken;
                        index++;
                    }
                    //Number in form of number
                    else if (isTokenANumber(token) && Float.valueOf(numberWithoutComma(token)) >= 0 && Float.valueOf(numberWithoutComma(token)) < 1000) {
                        processedToken = token;
                    }

                    //now dealing with words

                    else {
                        String[] seperatedToken = null;
                        if (token.contains("/") && index != 0) {
                            seperatedToken = Main.split(token, "/");
                            if (seperatedToken.length > 1 && seperatedToken[0].length() != 1 && seperatedToken[1].length() != 1) {
                                tokens[index - 1] = token.substring(0, token.indexOf("/"));
                                tokens[index] = token.substring(token.indexOf("/") + 1);
                                index--;
                                continue;
                            }
                        }
                        if (token.contains(".") && index != 0) {
                            seperatedToken = Main.split(token, ".");
                            if (seperatedToken.length > 1 && seperatedToken[0].length() != 1 && seperatedToken[1].length() != 1) {
                                tokens[index - 1] = token.substring(0, token.indexOf("."));
                                tokens[index] = token.substring(token.indexOf(".") + 1);
                                index--;
                                continue;
                            }
                        }
                        if (token.contains(",") && index != 0) {
                            seperatedToken = Main.split(token, ",");
                            if (seperatedToken.length > 1 && seperatedToken[0].length() != 1 && seperatedToken[1].length() != 1) {
                                tokens[index - 1] = token.substring(0, token.indexOf(","));
                                tokens[index] = token.substring(token.indexOf(",") + 1);
                                index--;
                                continue;
                            }
                        }
                        if (useStemming) {
                            stemmer.setTerm(token.toLowerCase());
                            stemmer.stem();
                            if (token.equals(token.toUpperCase()))
                                stemmer.setTerm(stemmer.getTerm().toUpperCase());
                            token = stemmer.getTerm();
                        }
                        String tokenInUpper = token.toUpperCase(), tokenInLower = token.toLowerCase();
                        Boolean tokenExistInLower = temporalPostings.containsKey(tokenInLower), tokenExistInUpper = temporalPostings.containsKey(tokenInUpper);
                        if (Character.isUpperCase(token.charAt(0))) { // the first char is a capital letter
                            processedToken = tokenInUpper;
                            if (tokenExistInLower)
                                processedToken = tokenInLower;
                        } else { //first char is in lower case
                            processedToken = tokenInLower;
                            if (tokenExistInUpper) {
                                String currentTermPostingData = temporalPostings.remove(tokenInUpper);
                                temporalPostings.put(tokenInLower, currentTermPostingData);
                            }
                        }
                    }
                }
                if (isValidToken(processedToken) && !stopWordsSet.contains(processedToken)) {
                    addProcessedTokenToTempPosting(processedToken);
                    processedToken=clearDelimiters(processedToken);
                    if (currentDoc != null) {
                        currentDoc.setLength(currentDoc.getLength() + 1);
                        //discover entities of given document
                        if (entitiesDiscoveredInDoc.containsKey(processedToken.toUpperCase()) ||
                                entitiesDiscoveredInDoc.containsKey(processedToken.toLowerCase()) ||
                                entitiesDiscoveredInDoc.containsKey(processedToken)) {
                            if (Character.isLowerCase(processedToken.charAt(0)))
                                if (entitiesDiscoveredInDoc.remove(processedToken.toUpperCase()) == null) {
                                    if (entitiesDiscoveredInDoc.remove(processedToken.toLowerCase()) == null) {
                                        entitiesDiscoveredInDoc.remove(processedToken);
                                    }
                                }
                        }//token not registered as entity
                        else if (Character.isUpperCase(processedToken.charAt(0))) {
                            entitiesDiscoveredInDoc.put(processedToken, Double.valueOf(0));
                        }
                    }
                    pos++;
                }
                processedToken = "";
                token = "";
                nextToken = "";
                secondNextToken = "";
                thirdNextToken = "";

            }
            index++;
        }
        //deal with entities
        determineFiveDominantEntities(currentDoc, entitiesDiscoveredInDoc);
    }

    /*private functions for the use of the parser.*/

    /**
     * The function determine the five most dominant entities in the doc by the entity's tf and first
     * position in the document.
     * @param currentDoc - the current document
     * @param entitiesDiscoveredInDoc - Map with all entities discovered in the document
     */
    private void determineFiveDominantEntities(Doc currentDoc, Map<String, Double> entitiesDiscoveredInDoc) {
        StringBuilder docEntities = new StringBuilder();
        if (currentDoc != null) {
            for (String entity : entitiesDiscoveredInDoc.keySet()) {
                entitiesDiscoveredInDoc.put(entity, getDominanceRank(entity, currentDoc.getLength()));

            }
            TreeMap<String, Double> target = new TreeMap<>(new Ranker.RankCompartor(entitiesDiscoveredInDoc));
            target.putAll(entitiesDiscoveredInDoc);
            int counter = 1;
            for (Map.Entry<String, Double> entity:target.entrySet()) {
                if (counter > 5)
                    break;
                docEntities.append("," + entity.getKey()+"*"+(int)Math.round(entity.getValue() * 1000)/(double)1000);
                counter++;
            }
            if (docEntities.toString().isEmpty())
                currentDoc.setEntitiesList("null");
            else
                currentDoc.setEntitiesList(docEntities.toString().substring(1));
            docEntities.setLength(0);
            entitiesDiscoveredInDoc.clear();
            target.clear();
        }
    }
    /**
     * Calculate the rank of a given entity when the rank is made based on the tf.
     * In case the entity appears in the first 10% of the document, the rank is reinforced.
     * @param entity - the entity we wish to rank based on it's document data
     * @return the dominanace rank
     */
    private double getDominanceRank(String entity, int docLength){
        String entityData;
        //get entity data of posting after parsing
        if(temporalPostings.containsKey(entity)){
            entityData=temporalPostings.get(entity);
        }
        else{
            if(temporalPostings.containsKey(entity.toLowerCase())){
                entityData=temporalPostings.get(entity.toLowerCase());
            }
            else{
                entityData=temporalPostings.get(entity.toUpperCase());
            }
        }
        String lastDocData;
        if(entityData.contains(" "))
            lastDocData= entityData.substring(entityData.lastIndexOf(" "));
        else
            lastDocData=entityData;
        int startOfTf=lastDocData.indexOf("*");
        int startOfPositions=lastDocData.indexOf(":");
        int endOfFirstPosition=lastDocData.indexOf(",");
        int tf=Integer.valueOf(lastDocData.substring(startOfTf+1));
        int firstPosition;
        if(endOfFirstPosition!=-1)
            firstPosition=Integer.valueOf(lastDocData.substring(startOfPositions+1,endOfFirstPosition));
        else
            firstPosition=Integer.valueOf(lastDocData.substring(startOfPositions+1,startOfTf));
        //consider the words appearing in the first 10% of the document to be important
        int isEntityInTheHeadOfDoc = 0;
        if((firstPosition/docLength)<= 0.1)
            isEntityInTheHeadOfDoc = 1;

        return 0.9*tf+0.1*isEntityInTheHeadOfDoc;
    }

    /**
     * check whether the token is valid for parsing.
     * @param token - the token we wish to validate.
     * @return true if the token is a valid token for processing, otherwise- false.
     */
    private boolean isValidToken (String token){
        if (token == null||token.equals("\n") || token.equals("\t")||token.equals("<text>") ||
                token.equals("") || token.equals("</text>") || token.equals("/text") || (token.isEmpty())|| token.equals(" "))
            return false;
        return true;
    }

    /**
     * Clear the token from unnecessary delimiters.
     * @param token - the token we wish to clean from delimiters.
     * @return a cleaned form of the token.
     */
    public String clearDelimiters(String token){
        if (token.startsWith(".")||token.startsWith("'") || token.startsWith(",") || token.startsWith(";") || token.startsWith(":")
                || token.startsWith("\"") ||token.startsWith("?") || token.startsWith("!")|| token.startsWith("(") || token.startsWith(")")
                || token.startsWith("-") || token.startsWith("+") || token.startsWith("`") || token.startsWith("\"") || token.startsWith("#")
                || token.startsWith("@") || token.startsWith("&") || token.startsWith("{") || token.startsWith("}") ||token.startsWith("[")
                || token.startsWith("]")){
            token = token.substring(1);
            token= clearDelimiters(token);
        }
        if (token.endsWith(".") || token.endsWith( ",")|| token.endsWith("'") || token.endsWith(";") || token.endsWith(":")
                || token.endsWith("(") || token.endsWith(")") || token.endsWith("?") || token.endsWith("!") || token.endsWith("\"")
                || token.endsWith("-") || token.endsWith("+") ||  token.endsWith("`")|| token.endsWith("\"")|| token.endsWith("#")
                || token.endsWith("@") || token.endsWith("&") || token.endsWith("{") || token.endsWith("}") ||token.endsWith("[")
                || token.endsWith("]") ){
            token = token.substring(0, token.length() - 1);
            token= clearDelimiters(token);
        }
        if (token.endsWith("'s") || token.endsWith("./") || token.endsWith("'S")){
            token = token.substring(0,token.length()-2);
            token = clearDelimiters(token);
        }
        return token;
    }
    /**
     * function that gets a number as a String and returns the same String without commas
     * @param token - the given number as a token
     * @return - the same token without commas
     */
    public String numberWithoutComma(String token) {
        String ans = "";
        if (token == null || token.equals("") || token.isEmpty())
            return ans;
        for (char c : token.toCharArray()) {
            if (c != ',')
                ans += c;
        }
        return ans;
    }

    /**
     * A function that gets a string and checks if it is a fracture by the assignment conditions
     * @param token - the possible fracture
     * @return true if the String is a fracture, else false
     */
    private boolean isTokenAFracture(String token) {
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        int countSlash = 0;
        for (char c: token.toCharArray()){
            if (c == '/')
                countSlash++;
            if (!Character.isDigit(c) && c != '/' && countSlash<2)
                return false;
        }
        if (countSlash == 1)
            return true;
        return false;
    }

    /**
     * A function that checks whether a given token contains a numeric value
     * @param token - the given token
     * @return true if the token contains a numeric value, otherwise false
     */
    public boolean isTokenANumber(String token){
        int countDots=0;
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        for (char c : token.toCharArray()){
            if (c == ',' )
                continue;
            if( c== '.') {
                countDots++;
                continue;
            }
            if (!Character.isDigit(c))
                return false;
        }
        return countDots<=1;
    }

    /*checks if the given token meets one of the Dollar forms of writing.*/
    private boolean isTokenADollarForm (String token){
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("Dollars") || token.equals("dollars") ||
                token.equals("dollar") ||  token.equals("Dollar") ||
                token.equals("U.S dollars") || token.equals("U.S Dollars") ||
                token.equals("U.S dollar") || token.equals("U.S Dollar"))
            return true;
        return false;
    }

    //checks if a given token is in one of the forms of kilometer
    private boolean isTokenAKilometerForm(String token) {
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("Kilometer") || token.equals("KILOMETER") || token.equals("kilometer") ||  token.equals("km") ||
                token.equals("KM") || token.equals("Km") || token.equals("K.M") || token.equals("k.m"))
            return true;
        return false;

    }

    //checks if a given token is in one of the forms of kilometer
    private boolean isTokenAKilogramForm(String token) {
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("Kilogram") || token.equals("KILOGRAM") || token.equals("kilogram") ||  token.equals("kg") ||
                token.equals("KG") || token.equals("Kg") || token.equals("K.G") || token.equals("k.g"))
            return true;
        return false;

    }

    //checks if a given token is in one of the forms of thousand
    private boolean isTokenAThousandForm(String token){
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("Thousand") || token.equals("thousand") || token.equals("Thousands") ||
                token.equals("thousands") || token.equals("THOUSAND") || token.equals("THOUSANDS"))
            return true;
        return false;
    }

    //checks if a given token is in one of the forms of million
    private boolean isTokenAMillionForm(String token){
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("Million") || token.equals("million") || token.equals("Millions") ||
                token.equals("millions")|| token.equals("MILLIONS") || token.equals("MILLION")
                || token.equals("m") || token.equals("M"))
            return true;
        return false;
    }

    //checks if a given token is in one of the forms of billion
    private boolean isTokenABillionForm(String token){
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("Billion") || token.equals("billion") || token.equals("Billions") ||
                token.equals("billions") || token.equals("BILLIONS") || token.equals("BILLION")
                || token.equals("bn") || token.equals("BN")|| token.equals("Bn") || token.equals("B"))
            return true;
        return false;
    }
    //checks if a given token is in one of the forms of trillion
    private boolean isTokenATrillionForm(String token){
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("Trillion") || token.equals("trillion") || token.equals("Trillions") ||
                token.equals("trillions")|| token.equals("TRILLIONS") || token.equals("TRILLION"))
            return true;
        return false;
    }

    //checks if a given token is in one of the forms of percentage
    private boolean isTokenAPercentageForm(String token){
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        if (token.equals("percent") || token.equals("Percent") || token.equals("PERCENT") ||
                token.equals("percentage") || token.equals("Percentage") || token.equals("PERCENTAGE"))
            return true;
        return false;
    }

    //checks if a given number (as string) is under 1 million
    private boolean isTokenUnderMillion(String token){
        if (token == null || token.equals("") || token.isEmpty())
            return false;
        float number = Float.valueOf(numberWithoutComma(token));
        if (number < 1000000)
            return true;
        return false;
    }

    //this function init a structure with all the forms of all the months
    private void intiallizeMonthsData(){
        months=new HashMap<String, String>()
        {{
            put("Jan","01"); put("Feb","02"); put("Mar","03"); put("Apr","04"); put("May","05");put("Jun","06");
            put("Jul","0"); put("Aug","08"); put("Sep","09");  put("Oct","10");  put("Nov","11");  put("Dec","12");
            put("Sept","09");  put("January","01"); put("February","02"); put("March","03"); put("April","04");
            put("May","05"); put("June","06"); put("July","07"); put("August","08"); put("September","09");
            put("October","10"); put("November","11");put("December","12");
            put("JANUARY","01");put("FEBRUARY","02");put("MARCH","03");put("APRIL","04");put("MAY","05");
            put("JUNE","06");put("JULY","07");put("AUGUST","08");
            put("SEPTEMBER","09");put("OCTOBER","10");put("NOVEMBER","11");put("DECEMBER","12");
        }};
    }

    /**
     * A function that adds a processed token to the temporal and if needed, updates the the current document's
     * max frequency, number of unique terms and the processed token's position in the document
     * @param processedToken - the token after the parsing process
     */
    public void addProcessedTokenToTempPosting(String processedToken){
        processedToken=clearDelimiters(processedToken);
        if (!processedToken.equals("cellrule") && !processedToken.equals("tablecell") && !processedToken.equals("cvj") && !processedToken.equals("chj")
                &&!processedToken.equals("CELLRULE") && !processedToken.equals("TABLECELL") && !processedToken.equals("CVJ")
                && !processedToken.equals("©'O'HGR") || !processedToken.equals("©'o'hgr")&& !processedToken.equals("lĉurike")&&!processedToken.equals("CHJ")){
            //token is not in temporal posting
            if(!processedToken.isEmpty() &&!temporalPostings.containsKey(processedToken)){
                String postingData = "1";
                if (currentDoc != null){
                    postingData = currentDoc.getName()+":"+pos+"*1";
                    currentDoc.setNumOfUniqueTerms(currentDoc.getNumOfUniqueTerms()+1);
                    if (currentDoc.getMaxFrequency() < 1)
                        currentDoc.setMaxFrequency(1);
                }
                temporalPostings.put(processedToken,postingData);
            }
            //token is in temporal dictionary
            else if(!processedToken.isEmpty()){
                String currentPostingData=temporalPostings.get(processedToken);
                StringBuilder builder = new StringBuilder();
                if(currentDoc!=null) {
                    if (currentPostingData.contains(currentDoc.getName())) {
                        int index = currentPostingData.lastIndexOf('*');
                        int curTermFreq = Integer.valueOf(currentPostingData.substring(index + 1));
                        builder.append(currentPostingData);
                        builder.insert(index, "," + pos);
                        index = builder.toString().lastIndexOf('*');
                        builder.replace(index + 1, builder.toString().length(), String.valueOf(curTermFreq + 1));
                        if (currentDoc.getMaxFrequency() < curTermFreq + 1)
                            currentDoc.setMaxFrequency(curTermFreq + 1);
                    } else {
                        builder.append(currentPostingData + " " + currentDoc.getName() + ":" + pos + "*1");
                        currentDoc.setNumOfUniqueTerms(currentDoc.getNumOfUniqueTerms() + 1);
                        if (currentDoc.getMaxFrequency() < 1)
                            currentDoc.setMaxFrequency(1);
                    }
                }else {
                    builder.setLength(0);
                    builder.append(Integer.valueOf(currentPostingData)+1);
                }
                temporalPostings.put(processedToken, builder.toString());
            }
        }
    }
}
