# IR_Project

Hi there! Before using the app, here are some points worth mentioining:

1. Before choosing one of the following options: Activate program, Show dictionary, Reset and Load dictionary, you must enter the
following information:
* In the first text field please enter the path to the your corpus, which must include a list of stop words 'stop_words.txt'.
* In the second text field please enter a path to a location to which all postings files and dictionaries will be writen.
* In the 'use stemmer' check box, you can decide whether to use stemming during the parse or not. By deafult, stemming in not used.
In order to change it, mark the checkbox and load your choices.

Please notice that the information you provided is saved in the system memory and will not changed until the button "load your choices!" 
will be pressed again.

2. At the end of "Activate program" proccess, you will see in the path you have provided for writing the following:
* posting files: a...z,0...9 and chars written in the form: "term; Doc_no:location1,location2*tf Doc_no..."
* terms dictionary in the form: term;df|tf|pointer to relevant posting. In cities dictionary we will also see: country|currency|populationSize
* documnets- containg all the data about documents in the form of:"doc_name:title|date|city|max_tf|numOfUniqueTerms|length".

3. if you wish to load your own dictionary, please add it under the name "terms_dictionary" or "terms_dictionary_stemmed".

4. When clicking useStemmer, you will see all the files mentioned in point2 just that they wll end with "_stemmeed".

5. reset- when clicking the reset button, all files in the path you have specified for writing will be deleted. Be carful when using it.

6. load and show dictionary will be applied according to the value of "use stemmer".

good luck!
