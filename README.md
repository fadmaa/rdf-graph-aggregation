This project documents the SPARQL queries used in benchmarking _Gagg_, an RDF graph aggregation operator. The code provided is not meant to run. This is only the part of the code responsible for benchmarking. The actual code that evaluates the queries is not open-sourced (yet). Example queries are shown below but futher queries can be seen in the code provided. Notice, that [Jena SSE Syntax](https://jena.apache.org/documentation/notes/sse.html) is used in the code while a tentative SPARQL syntax is provided below.

## BSBM type Summary
### SPARQL
```
PREFIX : <http://example.org/> 
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>

CONSTRUCT { 
    _:b0 a ?t1; :count ?subD_count .
    _:b1 a ?t2; :count ?objD_count .
    _:b2 a rdf:Statement; rdf:predicate ?p; rdf:subject _:b0; rdf:object _:b1; :count ?prop_count 
} WHERE {
    SELECT ?t1 ?subD_count ?t2 ?objD_count ?p (COUNT(*) AS ?prop_count){
        {
            SELECT ?t1 (COUNT(?s) AS ?subD_count)
            WHERE{
                ?s a ?t1 . ?s ?p ?o . ?o a ?t2 .
            } GROUP BY ?t1
        }
        {
            SELECT ?t2 (COUNT(?o) AS ?objD_count)
            WHERE{
                ?s a ?t1 . ?s ?p ?o . ?o a ?t2 .
            } GROUP BY ?t2
        }
        ?s ?p ?o; a ?t1 . ?o a ?t2 . 
    } GROUP BY ?t1 ?t2 ?subD_count ?objD_count ?p
}
```

### Gagg Syntax
```
PREFIX : <http://example.org/> 
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>

CONSTRUCT { 
    _:b0 a ?t1; :count COUNT(?sub.s) .
    _:b1 a ?t2; :count COUNT(?obj.o).
    _:b2 a rdf:Statement; rdf:predicate ?p; rdf:subject _:b0; rdf:object _:b1; :count ?prop_count 
} WHERE {
    GRAPH_AGGREGATION  {
        ?s ?p ?o
    	{?s a ?t1} GROUP BY ?t1 AS ?sub
    	{?o a ?t2} GROUP BY ?t2 AS ?obj
    }
}
```

## SP2B Bibliography data

### Co-authorship summary
Getting the full results required nesting three sub-queries, to count subjects, objects and properties.

#### SPARQL
```
PREFIX swrc:<http://swrc.ontoware.org/ontology#> 
PREFIX foaf: <http://xmlns.com/foaf/0.1/> 
PREFIX : <http://example.org/> 

CONSTRUCT { 
    ?auth1 <http://example.org/papers> ?subD_count .
    ?auth2 <http://example.org/papers> ?objD_count .
    _:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> :Coauthorship .
    _:b2 :author ?auth1 .
    _:b2 :author ?auth2 .
    _:b2 <http://example.org/count> ?prop_count .
} WHERE {
    SELECT ?auth1 ?subD_count ?auth2 ?objD_count (COUNT(*) AS ?prop_count){
        {
            SELECT ?auth1 (COUNT(?s) AS ?subD_count)
            WHERE{
                ?s a swrc:InProceedings; foaf:maker ?auth1, ?auth2 .
                FILTER (str(?auth1) < str(?auth2))
            } GROUP BY ?auth1
        }
        {
            SELECT ?auth2 (COUNT(?s) AS ?objD_count)
            WHERE{
                ?s a swrc:InProceedings; foaf:maker ?auth1, ?auth2 .
                FILTER (str(?auth1) < str(?auth2))
            } GROUP BY ?auth2
        }
        ?s a swrc:InProceedings; foaf:maker ?auth1, ?auth2 . 
        FILTER (str(?auth1) < str(?auth2))
    } GROUP BY ?auth1 ?auth2 ?subD_count ?objD_count
}
```

#### Gagg Syntax
```
PREFIX swrc:<http://swrc.ontoware.org/ontology#> 
PREFIX foaf: <http://xmlns.com/foaf/0.1/> 
PREFIX : <http://example.org/> 

CONSTRUCT { 
    ?auth1 <http://example.org/papers> COUNT(?sub.auth1) .
    ?auth2 <http://example.org/papers> COUNT(?obj.auth2) .
    _:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> :Coauthorship .
    _:b2 :author ?auth1 .
    _:b2 :author ?auth2 .
    _:b2 <http://example.org/count> ?prop_count .
} WHERE {
  GRAPH_AGGREGATION  {
    ?s a swrc:InProceedings .
    {?s foaf:maker ?auth1} GROUP BY ?auth1 AS ?sub 
    {?s foaf:maker ?auth2} GROUP BY ?auth2 AS ?obj
    FILTER (str(?auth1) < str(?auth2))
  }
}
```

Subresults counting *only* relations (i.e., not counting subjects and objects) can be achieved via a simpler SPARQL.

```
PREFIX swrc:<http://swrc.ontoware.org/ontology#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/> 
PREFIX : <http://example.org/> CONSTRUCT {
    _:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> :Coauthorship .
    _:b2 :author ?auth1 .
    _:b2 :author ?auth2 .
    _:b2 <http://example.org/count> ?prop_count .
} WHERE {
    SELECT ?auth1  ?auth2 (COUNT(DISTINCT ?s) AS ?prop_count){
        ?s a swrc:InProceedings; foaf:maker ?auth1, ?auth2 . 
	FILTER (str(?auth1) < str(?auth2)) 
    } GROUP BY ?auth1 ?auth2 
}
```
## LOD Cloud Summarising

