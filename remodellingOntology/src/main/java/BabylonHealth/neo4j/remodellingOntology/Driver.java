package BabylonHealth.neo4j.remodellingOntology;

import java.io.File;

import org.neo4j.driver.v1.Transaction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

public class Driver {

	public static void main(String[] args) {

		OWLOntology ontology = null;

		File myfile = new File("pizza.owl");

		OWLReasonerConfiguration config;

		ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
		config = new SimpleConfiguration(progressMonitor);

		try {

			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

			ontology = manager.loadOntologyFromOntologyDocument(myfile);

			OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
			OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
			reasoner.precomputeInferences();

			// This part was inspired by:
			// http://neo4j.com/blog/and-now-for-something-completely-different-using-owl-with-neo4j/

		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
