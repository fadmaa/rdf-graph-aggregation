package org.deri.rdfagg.sparql.junitbenchmarkers;

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
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.sse.SSE;
import com.hp.hpl.jena.sparql.syntax.Template;

import static org.junit.Assert.*;

public class BSBMBenchmarker {
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
		query = QueryFactory.create(Util.SPARQL_AGG_QUERY);
		simpleQuery = QueryFactory.create(Util.SIMPLE_SPARQL_AGG_QUERY);

		
		sparqlSubCountQuery = QueryFactory.create(Util.SPARQL_COUNT_SUBS);
		sparqlObjCountQuery = QueryFactory.create(Util.SPARQL_COUNT_OBS);
		sparqlPropCountQuery = QueryFactory.create(Util.SPARQL_COUNT_PROPS);
		
		executor = new org.deri.rdfagg.sparql.QueryExecution();
		Op op = SSE.parseOp(Util.AGG_SSE);
		DimensionDef[] subDims = new DimensionDef[] { new DimensionDef(
				"http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "t1", "t1") };
		DimensionDef[] objDims = new DimensionDef[] { new DimensionDef(
				"http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "t2", "t2") };
		tmplt = Util.buildTemplate(subDims, objDims);
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
	public void simpleSparqlSchemaSummayr() {
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
	
	static String filename = "/benchmark/bsbm/dataset_500.nt.gz";
}
