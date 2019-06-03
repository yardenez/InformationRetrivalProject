package Model;

import View.Main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CityTerm extends Term {

    private String country;
    private String currency;
    private String populationSize;

    //constructors

    public CityTerm(String term_name,int df,int corpusTf,int ptr,String country, String currency,String populationSize){
        super(term_name,df,corpusTf,ptr);
        this.country=country;
        this.currency=currency;
        this.populationSize=populationSize;
    }

    public CityTerm(CityTerm term){
        super(term.getName(),term.getDf(),term.getCorpusTf(),term.getPtr());
        this.country=term.country;
        this.currency=term.currency;
        this.populationSize=term.populationSize;
    }

    public CityTerm (String cityName){
        super(cityName);
        try {
            //working with restcountries API
            URL url = new URL("https://restcountries.eu/rest/v2/capital/"+cityName.toLowerCase()+"/?fields=name;capital;currencies;population");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String input;
            StringBuffer answer = new StringBuffer();
            while ((input = bufferedReader.readLine()) != null)
                answer.append(input);
            bufferedReader.close();
            //System.out.println(answer.toString());
            connection.disconnect();
            //working with the answer of the API
            String[] answerAsArray = Main.split(answer.toString(), "{|[|,|\"|:|]|}");
            currency = answerAsArray[4];
            country = answerAsArray[8];
            populationSize = answerAsArray[12];
            //parsing the population size to the corpus format
            if (Float.valueOf(populationSize) >= 1000 && Float.valueOf(populationSize) < 1000000) {
                float number = Float.valueOf(populationSize);
                number = number / 1000;
                if ((number - (int) number) == 0)
                    populationSize = String.valueOf(((int) number)) + "K";
                else {
                    if (String.valueOf(number).indexOf(".")!= -1 && String.valueOf(number).length()>String.valueOf(number).indexOf(".")+2)
                        populationSize = String.valueOf(number).substring(0,String.valueOf(number).indexOf(".")+3 )+"K";
                    else
                        populationSize = String.valueOf(number) + "K";
                }
            }
            else if (Float.valueOf(populationSize) >= 1000000 && Float.valueOf(populationSize) < 1000000000) {
                float number = Float.valueOf(populationSize);
                number = number / 1000000;
                if ((number - (int) number) == 0)
                    populationSize = String.valueOf(((int) number)) + "M";
                else{
                    if (String.valueOf(number).indexOf(".")!= -1 && String.valueOf(number).length()>String.valueOf(number).indexOf(".")+2)
                        populationSize = String.valueOf(number).substring(0,String.valueOf(number).indexOf(".")+3 )+"M";
                    else
                        populationSize = String.valueOf(number) + "M";
                }
            }
            else if (Float.valueOf(populationSize) >= 1000000000) {
                float number = Float.valueOf(populationSize);
                number = number / 1000000000;
                if ((number - (int) number) == 0)
                    populationSize = String.valueOf(((int) number)) + "B";
                else{
                    if (String.valueOf(number).indexOf(".")!= -1 && String.valueOf(number).length()>String.valueOf(number).indexOf(".")+2)
                        populationSize = String.valueOf(number).substring(0,String.valueOf(number).indexOf(".")+3 )+"B";
                    else
                        populationSize = String.valueOf(number) + "B";
                }
            }
//            System.out.println(currency);
//            System.out.println(country);
//            System.out.println(populationSize);
        }
        catch (Exception e){
        }

    }

    //setter and getters

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPopulationSize() {
        return populationSize;
    }

    public void setPopulationSize(String populationSize) {
        this.populationSize = populationSize;
    }
}
