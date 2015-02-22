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
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.sse.SSE;
import com.hp.hpl.jena.sparql.syntax.Template;

public class SP2BAuthorCitationSummaryBenchmarker {
	//@Rule
	//public TestRule benchmarkRun = new BenchmarkRule();
	
	
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
		GraphAggregateVariables aggVars = new GraphAggregateVariables("p1", 
				"p2", "prop", "prop", subDims, objDims);
		hop = new HypergraphBuilderOp(op, aggVars, AggregationFunc.COUNT_DISTINCT, AggregationFunc.COUNT_DISTINCT, AggregationFunc.COUNT);
	}

	//@Test
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
		ptrn.add(new Triple(subNode, NodeFactory
				.createURI("http://xmlns.com/foaf/0.1/name"), Var.alloc(subVarname)));
		
		ptrn.add(new Triple(objNode, NodeFactory
				.createURI("http://example.org/papers"), Var.alloc("objD_count")));
		ptrn.add(new Triple(objNode, NodeFactory
				.createURI("http://xmlns.com/foaf/0.1/name"), Var.alloc(objVarname)));
		
		ptrn.add(new Triple(rel, NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
				NodeFactory.createURI("http://example.org/Citation")));
		ptrn.add(new Triple(rel, NodeFactory.createURI("http://example.org/from"), subNode));
		ptrn.add(new Triple(rel, NodeFactory.createURI("http://example.org/to"), objNode));
		ptrn.add(new Triple(rel, NodeFactory.createURI("http://example.org/papers"), Var.alloc("prop_count")));
		
		Template tmplt = new Template(ptrn);
		return tmplt;
	}

	static String filename = "/benchmark/sp2b/sp2b-1000000.n3.gz";
	
	
	private static String sse = "(prefix ((: <http://example.org/>) (spb: <http://localhost/vocabulary/bench/>) "
			+ "(dc: <http://purl.org/dc/elements/1.1/>) (foaf: <http://xmlns.com/foaf/0.1/>) "
			+ "(dct: <http://purl.org/dc/terms/>) )"
			+ "(extend ( (?prop (iri \"http://example.org/cite\"))) "
			+ "(bgp (triple ?p1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> spb:Inproceedings) "
			+ "(triple ?p1 dc:creator ?b0) (triple ?p2 dc:creator ?b1) "
			+ "(triple ?p2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> spb:Inproceedings) "
			+ "(triple ?b0 foaf:name ?auth1) (triple ?b1 foaf:name ?auth2) (triple ?p1 dct:references ?refs) "
			+ "(triple ?refs ?p ?p2) )))";
	
	static String sparql = 
		"PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
		"PREFIX spb: <http://localhost/vocabulary/bench/> " +
		"PREFIX dc: <http://purl.org/dc/elements/1.1/> " +
		"PREFIX dct:<http://purl.org/dc/terms/> " +
		"PREFIX : <http://example.org/> CONSTRUCT { " +
			"_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> :Citation . " + 
			"_:b2 :from ?b0 ."
			+ "?b0 :papers ?sub_papers ."
			+ "?b0 foaf:name ?name1 ." +
			"_:b2 :to ?b1 . " +
			"?b1 :papers ?obj_papers ." +
			 "?b1 foaf:name ?name2 ." +
			"_:b2 <http://example.org/count> ?prop_count . " +
		"} WHERE { " +
			 "SELECT ?b0 ?name1 ?b1 ?name2 (COUNT(*) AS ?prop_count)" +
			 "WHERE{ " +
				"?p1 dc:creator ?b0;  a spb:Inproceedings; dct:references ?refs . " +
				"?b0 foaf:name ?name1 . " +
				"?p2 dc:creator ?b1;  a spb:Inproceedings . " +
				"?b1 foaf:name ?name2 . " +
				"?refs ?p ?p2 . " +
				"{" +
					"SELECT ?b1 ?name2 (COUNT(DISTINCT ?p2) AS ?obj_papers)" +
			     	"WHERE{ " +
						"?p1 dc:creator ?b0;  a spb:Inproceedings; dct:references ?refs . " +
						"?b0 foaf:name ?name1 . " +
						"?p2 dc:creator ?b1;  a spb:Inproceedings . " +
						"?b1 foaf:name ?name2 . " +
						"?refs ?p ?p2 . " + 
					"} GROUP BY ?b1 ?name2 " +
				"}" +
				"{" +
					"SELECT ?b0 ?name1 (COUNT(DISTINCT ?p1) AS ?sub_papers)" +
					"WHERE{ " +
						"?p1 dc:creator ?b0;  a spb:Inproceedings; dct:references ?refs . " +
						"?b0 foaf:name ?name1 . " +
						"?p2 dc:creator ?b1;  a spb:Inproceedings . " +
						"?b1 foaf:name ?name2 . " +
						"?refs ?p ?p2 . " + 
					"} GROUP BY ?b0 ?name1 " +
				"}" +
			"} GROUP BY ?b0 ?sub_papers ?b1 ?obj_papers ?name1 ?name2" +
		"}"
		;
			
	 static String sparql_count_obs = 
			 		 "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
					 "PREFIX spb: <http://localhost/vocabulary/bench/> " +
					 "PREFIX dc: <http://purl.org/dc/elements/1.1/> " +
					 "PREFIX dct:<http://purl.org/dc/terms/> " +
					 "PREFIX : <http://example.org/> CONSTRUCT { " +
					 "?b1 foaf:name ?name1 ." +
					 "?b1 <http://example.org/papers> ?papers . " +
					 "} WHERE { " +
					     "SELECT ?b1 ?name2 (COUNT(DISTINCT ?p2) AS ?papers)" +
					     "WHERE{ " +
							"?p1 dc:creator ?b0;  a spb:Inproceedings; dct:references ?refs . " +
							"?b0 foaf:name ?name1 . " +
							"?p2 dc:creator ?b1;  a spb:Inproceedings . " +
							"?b1 foaf:name ?name2 . " +
							"?refs ?p ?p2 . " + 
					 	"} GROUP BY ?b1 ?name2 " +
					 "}"
					 ;
	 static String sparql_count_subs = 
			 		 "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
					 "PREFIX spb: <http://localhost/vocabulary/bench/> " +
					 "PREFIX dc: <http://purl.org/dc/elements/1.1/> " +
					 "PREFIX dct:<http://purl.org/dc/terms/> " +
					 "PREFIX : <http://example.org/> CONSTRUCT { " +
					 "?b0 foaf:name ?name1 ." +
					 "?b0 <http://example.org/papers> ?papers . " +
					 "} WHERE { " +
					     "SELECT ?b0 ?name1 (COUNT(DISTINCT ?p1) AS ?papers)" +
					     "WHERE{ " +
							"?p1 dc:creator ?b0;  a spb:Inproceedings; dct:references ?refs . " +
							"?b0 foaf:name ?name1 . " +
							"?p2 dc:creator ?b1;  a spb:Inproceedings . " +
							"?b1 foaf:name ?name2 . " +
							"?refs ?p ?p2 . " + 
					 	"} GROUP BY ?b0 ?name1 " +
					 "}"
				;
	 
	 static String simple_sparql = 
			 "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
			 "PREFIX spb: <http://localhost/vocabulary/bench/> " +
			 "PREFIX dc: <http://purl.org/dc/elements/1.1/> " +
			 "PREFIX dct:<http://purl.org/dc/terms/> " +
			 "PREFIX : <http://example.org/> CONSTRUCT { " +
			 "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> :Citation . " + 
			 "_:b2 :from ?b0 ."
			 + "?b0 foaf:name ?name1 ." +
			 "_:b2 :to ?b1 . " +
			  "?b1 foaf:name ?name2 ." +
			 "_:b2 <http://example.org/count> ?prop_count . " +
			 "} WHERE { " +
			     "SELECT ?b0 ?name1 ?b1 ?name2 (COUNT(*) AS ?prop_count)" +
			     "WHERE{ " +
					"?p1 dc:creator ?b0;  a spb:Inproceedings; dct:references ?refs . " +
					"?b0 foaf:name ?name1 . " +
					"?p2 dc:creator ?b1;  a spb:Inproceedings . " +
					"?b1 foaf:name ?name2 . " +
					"?refs ?p ?p2 . " + 
			 	"} GROUP BY ?b0 ?b1 ?name1 ?name2" +
			 "}"
			 ;
	
	 static String sparql_count_props = simple_sparql;
}
