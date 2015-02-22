package org.deri.rdfagg.sparql.junitbenchmarkers;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.deri.rdfagg.sparql.algebra.HypergraphBuilderOp.DimensionDef;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.syntax.Template;

public class Util {
	
	public static Model readTurtleModel(String rdfFilename) throws IOException {
		Model m = ModelFactory.createDefaultModel();
		InputStream in = rdfFilename.getClass().getResourceAsStream(rdfFilename);
		m.read(in, "", "TTL");
		in.close();
		return m;
	}
	
	public static Model execConstruct(Model m, Query query){
		QueryExecution qExec = QueryExecutionFactory.create(query, m);
		Model res = qExec.execConstruct();
		return res;
	}
	
	public static Model execConstruct(Model m, String query){
		QueryExecution qExec = QueryExecutionFactory.create(query, m);
		Model res = qExec.execConstruct();
		return res;
	}
	
	static public Model readModel(String rdfFilename) throws IOException {
		Model m = ModelFactory.createDefaultModel();
		InputStream in = "".getClass().getResourceAsStream(rdfFilename);
        in = new GZIPInputStream(in);
		m.read(in, "", "TTL");
		in.close();
		return m;
	}

	static public Template buildTemplate(DimensionDef[] subDims, DimensionDef[] objDims) {
		BasicPattern ptrn = new BasicPattern();
		Node n1 = NodeFactory.createAnon();
		Node n2 = NodeFactory.createAnon();
		Node rel = NodeFactory.createAnon();
		ptrn.add(new Triple(n1, NodeFactory.createURI("http://example.org/count"), Var.alloc("subD_count")));
		for (DimensionDef subD : subDims) {
			ptrn.add(new Triple(n1, NodeFactory.createURI(subD.uri), Var.alloc(subD.outVarname)));
		}

		ptrn.add(new Triple(n2, NodeFactory.createURI("http://example.org/count"), Var.alloc("objD_count")));
		for (DimensionDef objD : objDims) {
			ptrn.add(new Triple(n2, NodeFactory.createURI(objD.uri), Var.alloc(objD.outVarname)));
		}

		ptrn.add(new Triple(rel, NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
				NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement")));
		ptrn.add(new Triple(rel, NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#subject"), n1));
		ptrn.add(new Triple(rel, NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#object"), n2));
		ptrn.add(new Triple(rel, NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate"), Var.alloc("prop")));
		ptrn.add(new Triple(rel, NodeFactory.createURI("http://example.org/count"), Var.alloc("prop_count")));
		Template tmplt = new Template(ptrn);
		return tmplt;
	}

	
	public static final String SPARQL_COUNT_PROPS = "PREFIX :<http://example.org/> " +
			"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			"CONSTRUCT{ " +
			 "[ a rdf:Statement; rdf:subject ?t1x; rdf:object ?t2x; rdf:predicate ?p; :count ?cnt ] . " +
			  "?t1x a ?t1 . " +
			  "?t2x a ?t2 . " +
			"} " +
			"WHERE{ " +
			  "SELECT ?p ?t1 (IRI(CONCAT (STR(?t1), \"_sss\")) AS ?t1x) ?t2 (IRI(CONCAT (STR(?t2), \"_ooo\")) AS ?t2x)  ?cnt " +
			  "WHERE{ " +
			    "SELECT ?p ?t1 ?t2 (COUNT(*) AS ?cnt) " +
			    "WHERE{ " +
			      "?s ?p ?o; a ?t1 . " +
			      "?o a ?t2 . " +
			    "} GROUP BY ?t1 ?t2 ?p " +
			  "} " +
			"}";
	
	public static final String SPARQL_COUNT_SUBS = "PREFIX :<http://example.org/> " +
			"CONSTRUCT{ " +
			 "?t1x :count ?cnt . " +
			"} " +
			"WHERE{ " +
			  "SELECT ?t1 (IRI(CONCAT (STR(?t1), \"_sss\")) AS ?t1x) ?cnt " +
			  "WHERE{ " +
			    "SELECT ?t1 (COUNT(DISTINCT ?s) AS ?cnt) " +
			    "WHERE{ " +
			      "?s ?p ?o; a ?t1 . " +
			      "?o a ?t2 . " +
			    "} GROUP BY ?t1  " +
			  "} " +
			"}";
	public static final String SPARQL_COUNT_OBS = "PREFIX :<http://example.org/> " +
			"CONSTRUCT{ " +
			 "?t2x :count ?cnt . " +
			"} " +
			"WHERE{ " +
			  "SELECT ?t2 (IRI(CONCAT (STR(?t2), \"_ooo\")) AS ?t2x) ?cnt " +
			  "WHERE{ " +
			    "SELECT ?t2 (COUNT(DISTINCT ?o) AS ?cnt) " +
			    "WHERE{ " +
			      "?s ?p ?o; a ?t1 . " +
			      "?o a ?t2 . " +
			    "} GROUP BY ?t2 " +
			  "} " +
			"}"; 

	public static final String SPARQL_AGG_QUERY = "prefix : <http://example.org/> CONSTRUCT { _:b0 <http://example.org/count> ?subD_count . "
			+ "_:b0 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t1 . "
			+ "_:b1 <http://example.org/count> ?objD_count . "
			+ "_:b1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t2 . "
			+ "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement> . "
			+ "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject> _:b0 . "
			+ "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#object> _:b1 . "
			+ "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> ?prop . "
			+ "_:b2 <http://example.org/count> ?prop_count . "
			+ "} WHERE {SELECT ?t1 ?subD_count ?t2 ?objD_count ?prop (COUNT(?prop) AS ?prop_count) { ?p1 a ?t1 . ?p2 a ?t2 . ?p1 ?prop ?p2 . { SELECT ?t1 (COUNT(DISTINCT ?s1) AS ?subD_count) WHERE { ?s1 a ?t1 . ?s2 a ?t2 . ?s1 ?prop ?s2 . } GROUP BY ?t1 } { SELECT ?t2 (COUNT(DISTINCT ?v2) AS ?objD_count) WHERE { ?v1 a ?t1 . ?v2 a ?t2 . ?v1 ?prop ?v2 . } GROUP BY ?t2 } } GROUP BY ?t1 ?t2 ?subD_count ?objD_count ?prop}";
	public static final String AGG_SSE = "(prefix ((: <http://example.org/>)) "
			+ "(bgp (triple ?p1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t1) "
			+ "(triple ?p2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t2) (triple ?p1 ?prop ?p2) ))";
	
	public static final String SIMPLE_SPARQL_AGG_QUERY = "prefix : <http://example.org/> CONSTRUCT { "
			+ "_:b0 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t1 . "
			+ "_:b1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t2 . "
			+ "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement> . "
			+ "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject> _:b0 . "
			+ "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#object> _:b1 . "
			+ "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> ?prop . "
			+ "_:b2 <http://example.org/count> ?prop_count . "
			+ "} WHERE {SELECT ?t1 ?t2 ?prop (COUNT(?prop) AS ?prop_count) { ?p1 a ?t1 . ?p2 a ?t2 . ?p1 ?prop ?p2 .  } GROUP BY ?t1 ?t2 ?prop}";
}
