package org.deri.rdfagg.sparql.junitbenchmarkers;

import static org.junit.Assert.*;

import java.io.IOException;

import org.deri.rdfagg.sparql.algebra.HypergraphBuilderOp;
import org.deri.rdfagg.sparql.algebra.HypergraphBuilderOp.DimensionDef;
import org.deri.rdfagg.sparql.algebra.HypergraphBuilderOp.GraphAggregateVariables;
import org.deri.rdfagg.sparql.algebra.accumulators.AggregationFunc;
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

public class SWDFCoauthorSummaryBenchmarker {
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
		m = Util.readTurtleModel(filename);
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

	//@Test
	public void sparqlSchemaSummayr() {
		QueryExecution qExec = QueryExecutionFactory.create(query, m);
		Model res = qExec.execConstruct();
		assertFalse(res.isEmpty());
	}
	
	//@Test
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
		ptrn.add(new Triple(Var.alloc(subVarname), NodeFactory
				.createURI("http://example.org/papers"), Var.alloc("subD_count")));
		ptrn.add(new Triple(Var.alloc(objVarname), NodeFactory
				.createURI("http://example.org/papers"), Var.alloc("objD_count")));
		ptrn.add(new Triple(rel, NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
				NodeFactory.createURI("http://example.org/Coauthorship")));
		ptrn.add(new Triple(rel, NodeFactory.createURI("http://example.org/author"), Var.alloc(subVarname)));
		ptrn.add(new Triple(rel, NodeFactory.createURI("http://example.org/author"), Var.alloc(objVarname)));
		ptrn.add(new Triple(rel, NodeFactory.createURI("http://example.org/papers"), Var.alloc("prop_count")));
		
		Template tmplt = new Template(ptrn);
		return tmplt;
	}

	static String filename = //"/benchmark/swdf/swdf.nt.gz";
			"/benchmark/swdf/sample.nt";
	
	
	private static String sse = "(prefix ((: <http://example.org/>) (swrc: <http://swrc.ontoware.org/ontology#>) (foaf: <http://xmlns.com/foaf/0.1/>)) "
			+ "(filter (< (str ?auth1) (str ?auth2) ) "
			+ "(extend ( (?prop (iri \"http://example.org/rel\"))) "
			+ "(bgp (triple ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> swrc:InProceedings) "
			+ "(triple ?s foaf:maker ?auth1) (triple ?s foaf:maker ?auth2) ))))";
	
	static String sparql = 
			"PREFIX swrc:<http://swrc.ontoware.org/ontology#> "+
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/> "+
			"PREFIX : <http://example.org/> CONSTRUCT { ?auth1 <http://example.org/papers> ?subD_count . "
			+ "?auth2 <http://example.org/papers> ?objD_count . "
			+ "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> :Coauthorship . " 
			+ "_:b2 :author ?auth1 . "
			+ "_:b2 :author ?auth2 . "
			+ "_:b2 <http://example.org/count> ?prop_count . "
			+"} WHERE { "
			+ "SELECT ?auth1 ?subD_count ?auth2 ?objD_count (COUNT(*) AS ?prop_count){"+
			
				"{ " +
				"SELECT ?auth1 (COUNT(?s) AS ?subD_count)" +
				"WHERE{" +
					"?s a swrc:InProceedings; foaf:maker ?auth1, ?auth2 . "+
					"FILTER (str(?auth1) < str(?auth2)) "+
				"} GROUP BY ?auth1 " +
				"}" +
				"{ " +
				"SELECT ?auth2 (COUNT(?s) AS ?objD_count)" +
				"WHERE{" +
					"?s a swrc:InProceedings; foaf:maker ?auth1, ?auth2 . "+
					"FILTER (str(?auth1) < str(?auth2)) "+
				"} GROUP BY ?auth2 " +
				"}" +
				"?s a swrc:InProceedings; foaf:maker ?auth1, ?auth2 . "+
				"FILTER (str(?auth1) < str(?auth2)) "+
				"} GROUP BY ?auth1 ?auth2 ?subD_count ?objD_count"
  			+ "}";
	
	 static String sparql_count_obs = 
			"PREFIX swrc:<http://swrc.ontoware.org/ontology#> "+
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/> "+
			"PREFIX : <http://example.org/> " +
			"CONSTRUCT{ " +
			 "?auth2 :papers ?cnt . " +
			"} " +
			"WHERE{ " +
			  "SELECT ?auth2 (COUNT(DISTINCT ?s) AS ?cnt)" +
				"WHERE{" +
					"?s a swrc:InProceedings; foaf:maker ?auth1, ?auth2 . "+
					"FILTER (str(?auth1) < str(?auth2)) "+
				"} GROUP BY ?auth2 " +
			"}"; 
	 static String sparql_count_subs = 
			 "PREFIX swrc:<http://swrc.ontoware.org/ontology#> "+
						"PREFIX foaf: <http://xmlns.com/foaf/0.1/> "+
						"PREFIX : <http://example.org/> " +
				"CONSTRUCT{ " +
				 "?auth1 :papers ?cnt . " +
				"} " +
				"WHERE{ " +
				  "SELECT ?auth1 (COUNT(DISTINCT ?s) AS ?cnt)" +
					"WHERE{" +
						"?s a swrc:InProceedings; foaf:maker ?auth1, ?auth2 . "+
						"FILTER (str(?auth1) < str(?auth2)) "+
					"} GROUP BY ?auth1 " +
				"}"; 
	 static String sparql_count_props = 
				"PREFIX swrc:<http://swrc.ontoware.org/ontology#> "+
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/> "+
				"PREFIX : <http://example.org/> CONSTRUCT { "
				+ "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> :Coauthorship . " 
				+ "_:b2 :author ?auth1 . "
				+ "_:b2 :author ?auth2 . "
				+ "_:b2 <http://example.org/papers> ?prop_count . "
				+"} WHERE { "
				+"SELECT ?auth1 ?auth2  (COUNT(DISTINCT ?s) AS ?prop_count) " +
				  "WHERE{ " +
				    "?s a swrc:InProceedings; foaf:maker ?auth1, ?auth2 . "+
					"FILTER (str(?auth1) < str(?auth2)) "
				+ "} GROUP BY ?auth1 ?auth2"
				+ "}"
				;
	 
	 static String simple_sparql = 
				"PREFIX swrc:<http://swrc.ontoware.org/ontology#> "+
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/> "+
				"PREFIX : <http://example.org/> CONSTRUCT { "
				+ "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> :Coauthorship . " 
				+ "_:b2 :author ?auth1 . "
				+ "_:b2 :author ?auth2 . "
				+ "_:b2 <http://example.org/count> ?prop_count . "
				+"} WHERE { "
				+ "SELECT ?auth1  ?auth2 (COUNT(DISTINCT ?s) AS ?prop_count){"+
				
					"?s a swrc:InProceedings; foaf:maker ?auth1, ?auth2 . "+
					"FILTER (str(?auth1) < str(?auth2)) "+
					"} GROUP BY ?auth1 ?auth2 "
	  			+ "}";
}