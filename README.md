This project documents the SPARQL queries used in benchmarking _Gagg_, an RDF graph aggregation operator.

## BSBM type Summary

## SP2B Bibliography data

### Co-authorship summary
Getting the full results required nesting three sub-queries, to count subjects, objects and properties.

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

