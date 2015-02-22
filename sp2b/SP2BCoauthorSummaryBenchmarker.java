package org.deri.rdfagg.sparql.junitbenchmarkers.sp2b;

import static org.junit.Assert.*;

import java.io.IOException;

import org.deri.rdfagg.sparql.algebra.HypergraphBuilderOp;
import org.deri.rdfagg.sparql.algebra.HypergraphBuilderOp.DimensionDef;
import org.deri.rdfagg.sparql.algebra.HypergraphBuilderOp.GraphAggregateVariables;
import org.deri.rdfagg.sparql.algebra.accumulators.AggregationFunc;
import org.deri.rdfagg.sparql.junitbenchmarkers.Util;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.sse.SSE;
import com.hp.hpl.jena.sparql.syntax.Template;

public class SP2BCoauthorSummaryBenchmarker {
	@Rule
	public TestRule benchmarkRun = new BenchmarkRule();
	
	
	static Model m;
	static Query query;
	static Query simpleQuery;
	
	static Query sparqlSubCountQuery, sparqlObjCountQuery, sparqlPropCountQuery;

	static org.deri.rdfagg.sparql.QueryExecution executor;
	static Template tmplt;
	static HypergraphBuilderOp hop;

	@BeforeClass
	public static void setUp() throws IOException {
		m = Util.readModel(filename);
		query = QueryFactory.create(sparql);
		simpleQuery = QueryFactory.create(simple_sparql);

		
		sparqlSubCountQuery = QueryFactory.create(sparql_count_subs);
		sparqlObjCountQuery = QueryFactory.create(sparql_count_obs);
		sparqlPropCountQuery = QueryFactory.create(sparql_count_props);
		
		executor = new org.deri.rdfagg.sparql.QueryExecution();
		Op op = SSE.parseOp(sse);
		DimensionDef[] subDims = new DimensionDef[] { new DimensionDef(
				"http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "auth1", "auth1") };
		DimensionDef[] objDims = new DimensionDef[] { new DimensionDef(
				"http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "auth2", "auth2") };
		tmplt = buildTemplate("auth1", "auth2");
		GraphAggregateVariables aggVars = new GraphAggregateVariables("s", 
				"s", "prop", "s", subDims, objDims);
		hop = new HypergraphBuilderOp(op, aggVars, AggregationFunc.COUNT_DISTINCT, AggregationFunc.COUNT_DISTINCT, AggregationFunc.COUNT_DISTINCT);
	}

	@Test
	public void sparqlSchemaSummayr() {
		QueryExecution qExec = QueryExecutionFactory.create(query, m);
		Model res = qExec.execConstruct();
		assertFalse(res.isEmpty());
	}
	
	@Test
	public void simpleSparqlSchemaSummary() {
		QueryExecution qExec = QueryExecutionFactory.create(simpleQuery, m);
		Model res = qExec.execConstruct();
		assertFalse(res.isEmpty());
	}

	@Test
	public void graphAggSchemaSummary() {
		Model res = executor.exec(m, tmplt, hop);
		assertFalse(res.isEmpty());
	}

	@Test
	public void threeSparqlQueries() {
		QueryExecution qExec = QueryExecutionFactory.create(sparqlSubCountQuery, m);
		Model res1 = qExec.execConstruct();
		assertFalse(res1.isEmpty());
		
		qExec = QueryExecutionFactory.create(sparqlObjCountQuery, m);
		Model res2 = qExec.execConstruct();
		assertFalse(res2.isEmpty());
		
		qExec = QueryExecutionFactory.create(sparqlPropCountQuery, m);
		Model res3 = qExec.execConstruct();
		assertFalse(res3.isEmpty());
	}
	
	static private Template buildTemplate(String subVarname, String objVarname) {
		BasicPattern ptrn = new BasicPattern();
		Node rel = NodeFactory.createAnon();
		Node subNode = NodeFactory.createAnon();
		Node objNode = NodeFactory.createAnon();
		ptrn.add(new Triple(subNode, NodeFactory
				.createURI("http://example.org/papers"), Var.alloc("subD_count")));
		ptrn.add(new Triple(objNode, NodeFactory
				.createURI("http://xmlns.com/foaf/0.1/name"), Var.alloc(subVarname)));
		ptrn.add(new Triple(Var.alloc(objVarname), NodeFactory
				.createURI("http://example.org/papers"), Var.alloc("objD_count")));
		ptrn.add(new Triple(objNode, NodeFactory
				.createURI("http://xmlns.com/foaf/0.1/name"), Var.alloc(objVarname)));
		
		ptrn.add(new Triple(rel, NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
				NodeFactory.createURI("http://example.org/Coauthorship")));
		ptrn.add(new Triple(rel, NodeFactory.createURI("http://example.org/author"), Var.alloc(subVarname)));
		ptrn.add(new Triple(rel, NodeFactory.createURI("http://example.org/author"), Var.alloc(objVarname)));
		ptrn.add(new Triple(rel, NodeFactory.createURI("http://example.org/papers"), Var.alloc("prop_count")));
		
		Template tmplt = new Template(ptrn);
		return tmplt;
	}

	static String filename = "/benchmark/sp2b/sp2b-50000.n3.gz";
	
	
	private static String sse = "(prefix ((: <http://example.org/>) (spb: <http://localhost/vocabulary/bench/>) "
			+ "(dc: <http://purl.org/dc/elements/1.1/>) (foaf: <http://xmlns.com/foaf/0.1/>)) "
			+ "(filter (< ?auth1 ?auth2 ) "
			+ "(extend ( (?prop (iri \"http://example.org/rel\"))) "
			+ "(bgp (triple ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> spb:Inproceedings) "
			+ "(triple ?s dc:creator ?b0) (triple ?s dc:creator ?b1) "
			+ "(triple ?b0 foaf:name ?auth1) (triple ?b1 foaf:name ?auth2)  ))))";
	
	static String sparql =
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
					 "PREFIX spb: <http://localhost/vocabulary/bench/> " +
					 "PREFIX dc: <http://purl.org/dc/elements/1.1/> " +
					 "PREFIX : <http://example.org/> CONSTRUCT { " +
					 "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> :Coauthorship . " + 
					 "_:b2 :author ?auth1 . "
					 + "?auth1 <http://example.org/papers> ?subcnt ." 
					 + "?auth2 <http://example.org/papers> ?objcnt ." +
					 "_:b2 :author ?auth2 . " +
					 "_:b2 <http://example.org/count> ?prop_count . " +
					 "} WHERE { " +
					     "SELECT ?auth1 ?subcnt ?auth2 ?objcnt (COUNT(DISTINCT ?s) AS ?prop_count)" +
					 
			"{" +
				"?s dc:creator ?b0;  a spb:Inproceedings; dc:creator ?b1 ." 
				+ "?b0 foaf:name ?name1 ."
				+ "?b1 foaf:name ?name2 ." 
				+ "BIND (IRI(STR(?name1)) AS ?auth1) "
				+ "FILTER (?name1 < ?name2) " +
				"{" +
					"SELECT ?auth1 (COUNT(DISTINCT ?s) AS ?subcnt) " +
					"WHERE{" +
					    "?s dc:creator ?b0;  a spb:Inproceedings; dc:creator ?b1 ." 
					    + "?b0 foaf:name ?name1 ."
					    + "?b1 foaf:name ?name2 ." 
					    + "BIND (IRI(STR(?name1)) AS ?auth1) "
						+ "FILTER (?name1 < ?name2) " +
					"} GROUP BY ?auth1 "
				+ "}"
				+ "{" +
					"SELECT ?auth2 (COUNT(DISTINCT ?s) AS ?objcnt) " +
					"WHERE{" +
					    "?s dc:creator ?b0;  a spb:Inproceedings; dc:creator ?b1 ." 
					    + "?b0 foaf:name ?name1 ."
					    + "?b1 foaf:name ?name2 ." 
					    + "BIND (IRI(STR(?name2)) AS ?auth2)" +
						"FILTER (?name1 < ?name2) "+
					"} GROUP BY ?auth2 "
				+ "}"
			+ "} GROUP BY ?auth1 ?auth2 ?subcnt ?objcnt"
			+ "}";
			
	 static String sparql_count_obs = 
			 "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " + 
			 "PREFIX spb: <http://localhost/vocabulary/bench/> " +
			"PREFIX dc: <http://purl.org/dc/elements/1.1/> " +
			"PREFIX : <http://example.org/> " +
			"CONSTRUCT{ " +
			 "?auth2 :papers ?cnt . " +
			"} " +
			"WHERE{ " +
			  "SELECT ?auth2 (COUNT(DISTINCT ?s) AS ?cnt) " +
				"WHERE{" +
				    "?s dc:creator ?b0;  a spb:Inproceedings; dc:creator ?b1 ." 
				    + "?b0 foaf:name ?name1 ."
				    + "?b1 foaf:name ?name2 ." 
				    + "BIND (IRI(STR(?name1)) AS ?auth1) "
				    + "BIND (IRI(STR(?name2)) AS ?auth2)" +
					"FILTER (?name1 < ?name2) "+
				"} GROUP BY ?auth2 " +
			"}"; 
	 static String sparql_count_subs = 
			 "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " + 
			 "PREFIX spb: <http://localhost/vocabulary/bench/> " +
			 "PREFIX dc: <http://purl.org/dc/elements/1.1/> " +
			 "PREFIX : <http://example.org/> " +
				"CONSTRUCT{ " +
				 "?auth1 :papers ?cnt . " +
				"} " +
				"WHERE{ " +
				  "SELECT ?auth1 (COUNT(DISTINCT ?s) AS ?cnt) " +
					"WHERE{" +
					    "?s dc:creator ?b0;  a spb:Inproceedings; dc:creator ?b1 ." 
					    + "?b0 foaf:name ?name1 ."
					    + "?b1 foaf:name ?name2 ." 
					    + "BIND (IRI(STR(?name1)) AS ?auth1) "
					    + "BIND (IRI(STR(?name2)) AS ?auth2)" +
						"FILTER (?name1 < ?name2) "+
					"} GROUP BY ?auth1 " +
				"}"; 
	 static String sparql_count_props = 
			 "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " + 
			 	"PREFIX spb: <http://localhost/vocabulary/bench/> " +
				"PREFIX dc: <http://purl.org/dc/elements/1.1/> " +
				"PREFIX : <http://example.org/> CONSTRUCT { "
				+ "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> :Coauthorship . " 
				+ "_:b2 :author ?auth1 . "
				+ "_:b2 :author ?auth2 . "
				+ "_:b2 <http://example.org/papers> ?prop_count . "
				+"} WHERE { "
				+"SELECT ?auth1 ?auth2  (COUNT(DISTINCT ?s) AS ?prop_count) " +
				  "WHERE{ " +
				    "?s dc:creator ?b0;  a spb:Inproceedings; dc:creator ?b1 ."
				    + "?b0 foaf:name ?name1 ."
				    + "?b1 foaf:name ?name2 ." 
				    + "BIND (IRI(STR(?name1)) AS ?auth1) "
				    + "BIND (IRI(STR(?name2)) AS ?auth2)" +
					"FILTER (?name1 < ?name2) "
				+ "} GROUP BY ?auth1 ?auth2"
				+ "}"
				;
	 
	 static String simple_sparql = 
			 "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
			 "PREFIX spb: <http://localhost/vocabulary/bench/> " +
			 "PREFIX dc: <http://purl.org/dc/elements/1.1/> " +
			 "PREFIX : <http://example.org/> CONSTRUCT { " +
			 "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> :Coauthorship . " + 
			 "_:b2 :author ?auth1 . " +
			 "_:b2 :author ?auth2 . " +
			 "_:b2 <http://example.org/count> ?prop_count . " +
			 "} WHERE { " +
			     "SELECT ?auth1  ?auth2 (COUNT(DISTINCT ?s) AS ?prop_count)" +
			     "WHERE{ " +
			       "?s dc:creator ?b0;  a spb:Inproceedings; dc:creator ?b1 ." 
			       + "?b0 foaf:name ?name1 ."
				    + "?b1 foaf:name ?name2 ."
				    + "BIND (IRI(STR(?name1)) AS ?auth1) "
				    + "BIND (IRI(STR(?name2)) AS ?auth2)" +
			  		"FILTER (?name1 < ?name2) " +
			 	"} GROUP BY ?auth1 ?auth2" +
			 "}"
			 ;
}
