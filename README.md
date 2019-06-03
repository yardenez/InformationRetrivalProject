# IR_Project

Hi there! Before using the app, here are some points worth mentioned:

indexing:

1. Before choosing one of the following options: Activate program, Show dictionary, Reset and Load dictionary, you must enter the
following information:
* In the first text field please enter the path to the your corpus, which must include a list of stop words 'stop_words.txt'.
[you can use the given miniCorpus as a small example]
* In the second text field please enter a path to a location to which all postings files and dictionaries will be writen.
* In the 'use stemmer' check box, you can decide whether to use stemming during the parse or not. By deafult, stemming in not used.
In order to change it, mark the checkbox and load your choices.

Please notice that the information you provided is saved in the system memory and will not changed until the button "load your choices!" 
will be pressed again.

2. At the end of "Activate program" proccess, you will see in the path you have provided for writing the following:
* posting files: a...z,0...9 and chars written in the form: "term; Doc_no:location1,location2*tf Doc_no..."
* terms dictionary in the form: term;df|tf|pointer to relevant posting. In cities dictionary we will also see: country|currency|populationSize
* documnets- containg all the data about documents in the form of:"doc_name:title|date|city|max_tf|numOfUniqueTerms|length|entitiy1*tf,entity2*tf..|language
".

3. If you wish to load your own dictionary, please add it under the name "terms_dictionary" or "terms_dictionary_stemmed".

4. When clicking useStemmer, you will see all the files mentioned in point2 just that they wll end with "_stemmeed".

5. Reset- when clicking the reset button, all files in the path you have specified for writing will be deleted. Be carful when using it.

6. Load and show dictionary will be applied according to the value of "use stemmer".

Search:

now that you have created an inverted index, you can retrive documnets for your queries.
Before clicking on the "Run" button,please do the following:

1. load your choices once again- meaning, the data set and the path in which the posting files are saved.

2. load the dictionaries before using! this action will load all the relevant information to the main memory- terms 
dictionary,cities dictionary, documents dictionary, list of languages and posting files. 
Before doing so, choose whether to use stemming or not. This will effect the files we will pull to main memory, and effect
your query and search.

3. Choose whether to enter a single query for search or a queries file by clicking on the corresponding checkbox.
In case you have chosen to enter a file, please enter the path in which the file is located by clicking on the "Browse" button.
[you can use the given queriesExample file as an exapmle]

4. Choose whether you wish to actiave a semantic treatment while searching the query by clicking on "Semantic treatment".
By deafult,semantic treatment is not added.

5. You may choose to filter the results by cities and languages. You can do so by pressing "ctrl" and right click on the requested
cities/ languages. Please notice that by the retrived results will contain only documents containing the cities or languages you have 
selected. If no option was selected, no filter will be made.

by clicking run, your query will be proccessed. 
At the end, all your queries will be shown so you can see the results for each and one of your queries 
by clicking "show results". For each document you can see the five most dominant entities.

good luck!

