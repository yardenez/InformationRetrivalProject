package Model;

public class Doc {

    private String name;
    private String title;
    private String date;
    private String city;
    private String language;
    private int length;
    private int maxFrequency;
    private int numOfUniqueTerms;
    private String entitiesList;

    public Doc(){
        name = null;
        title = null;
        date = null;
        city = null;
        language =null;
        entitiesList=null;
        length = 0;
        maxFrequency = 0;
        numOfUniqueTerms = 0;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() { return title; }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getMaxFrequency() {
        return maxFrequency;
    }

    public void setMaxFrequency(int maxFrequency) {
        this.maxFrequency = maxFrequency;
    }

    public int getNumOfUniqueTerms() {
        return numOfUniqueTerms;
    }

    public void setNumOfUniqueTerms(int numOfUniqueTerms) {
        this.numOfUniqueTerms = numOfUniqueTerms;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getEntitiesList() { return entitiesList; }

    public void setEntitiesList(String entitiesList) { this.entitiesList = entitiesList; }
}
