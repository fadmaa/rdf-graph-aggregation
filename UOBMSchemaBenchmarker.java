package org.deri.rdfagg.sparql.junitbenchmarkers;

import java.io.IOException;
import java.io.InputStream;

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
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.sse.SSE;
import com.hp.hpl.jena.sparql.syntax.Template;

import static org.junit.Assert.*;

public class UOBMSchemaBenchmarker {
	@Rule
	public TestRule benchmarkRun = new BenchmarkRule();

	static Model m;
	static Query query;

	static org.deri.rdfagg.sparql.QueryExecution executor;
	static Template tmplt;
	static HypergraphBuilderOp hop;

	@BeforeClass
	public static void setUp() throws IOException {
		m = readModel(filename);
		query = QueryFactory.create(sparql);

		executor = new org.deri.rdfagg.sparql.QueryExecution();
		Op op = SSE.parseOp(sse);
		DimensionDef[] subDims = new DimensionDef[] { new DimensionDef(
				"http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "t1", "t1") };
		DimensionDef[] objDims = new DimensionDef[] { new DimensionDef(
				"http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "t2", "t2") };
		tmplt = buildTemplate(subDims, objDims);
		GraphAggregateVariables aggVars = new GraphAggregateVariables("p1",
				"p2", "prop", "prop", subDims, objDims);
		hop = new HypergraphBuilderOp(op, aggVars, AggregationFunc.COUNT_DISTINCT, AggregationFunc.COUNT_DISTINCT, AggregationFunc.COUNT);

	}

	@Test
	public void sparqlSchemaSummayr() {
		QueryExecution qExec = QueryExecutionFactory.create(query, m);
		Model res = qExec.execConstruct();
		assertFalse(res.isEmpty());
	}

	@Test
	public void graphAggSchemaSummary() {
		Model res = executor.exec(m, tmplt, hop);
		assertFalse(res.isEmpty());
	}
	
	static private Model readModel(String rdfFilename) throws IOException {
		Model m = ModelFactory.createDefaultModel();
		InputStream in = "".getClass().getResourceAsStream(rdfFilename);
		m.read(in, "", "TTL");
		in.close();
		return m;
	}

	static private Template buildTemplate(DimensionDef[] subDims,
			DimensionDef[] objDims) {
		BasicPattern ptrn = new BasicPattern();
		Node n1 = NodeFactory.createAnon();
		Node n2 = NodeFactory.createAnon();
		Node rel = NodeFactory.createAnon();
		ptrn.add(new Triple(n1, NodeFactory
				.createURI("http://example.org/count"), Var.alloc("subD_count")));
		for (DimensionDef subD : subDims) {
			ptrn.add(new Triple(n1, NodeFactory.createURI(subD.uri), Var
					.alloc(subD.outVarname)));
		}

		ptrn.add(new Triple(n2, NodeFactory
				.createURI("http://example.org/count"), Var.alloc("objD_count")));
		for (DimensionDef objD : objDims) {
			ptrn.add(new Triple(n2, NodeFactory.createURI(objD.uri), Var
					.alloc(objD.outVarname)));
		}

		ptrn.add(new Triple(
				rel,
				NodeFactory
						.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
				NodeFactory
						.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement")));
		ptrn.add(new Triple(
				rel,
				NodeFactory
						.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#subject"),
				n1));
		ptrn.add(new Triple(
				rel,
				NodeFactory
						.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#object"),
				n2));
		ptrn.add(new Triple(
				rel,
				NodeFactory
						.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate"),
				Var.alloc("prop")));
		ptrn.add(new Triple(rel, NodeFactory
				.createURI("http://example.org/count"), Var.alloc("prop_count")));
		Template tmplt = new Template(ptrn);
		return tmplt;
	}

	static String filename = "/benchmark/uobm/uobm_dataset_5.nt";
	static String sparql = "prefix : <http://example.org/> CONSTRUCT { _:b0 <http://example.org/count> ?subD_count . "
			+ "_:b0 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t1 . "
			+ "_:b1 <http://example.org/count> ?objD_count . "
			+ "_:b1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t2 . "
			+ "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement> . "
			+ "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject> _:b0 . "
			+ "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#object> _:b1 . "
			+ "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate> ?prop . "
			+ "_:b2 <http://example.org/count> ?prop_count . "
			+ "} WHERE {SELECT ?t1 ?subD_count ?t2 ?objD_count ?prop (COUNT(?prop) AS ?prop_count) { ?p1 a ?t1 . ?p2 a ?t2 . ?p1 ?prop ?p2 . { SELECT ?t1 (COUNT(DISTINCT ?p1) AS ?subD_count) WHERE { ?p1 a ?t1 . ?p2 a ?t2 . ?p1 ?prop ?p2 . } GROUP BY ?t1 } { SELECT ?t2 (COUNT(DISTINCT ?p2) AS ?objD_count) WHERE { ?p1 a ?t1 . ?p2 a ?t2 . ?p1 ?prop ?p2 . } GROUP BY ?t2 } } GROUP BY ?t1 ?t2 ?subD_count ?objD_count ?prop}";
	static String sse = "(prefix ((: <http://example.org/>)) (bgp (triple ?p1 ?prop ?p2) (triple ?p1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t1) (triple ?p2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?t2) ))";

}
