package generalConvertor;

import java.io.File;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

public class Ontology2Graph {

	private OWLOntology ontology;
	private OWLOntologyManager manager;
	private OWLReasoner reasoner;
	private OWLReasonerFactory reasonerFactory;
	private Transaction graphTx;

	private File ontologyFile;
	private File graphFile;
	private Logger logger;
	private FileHandler fh;

	private GraphDatabaseFactory dbFactory;
	private GraphDatabaseService dbDataService;

	private String classString;

	public static String ROOT_URI = "owl:thing";

	Ontology2Graph() {
		try {
			logger = Logger.getLogger(Ontology2Graph.class.getName());
			fh = new FileHandler("./mylog.log");
			logger.addHandler(fh);
			logger.setUseParentHandlers(false);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method gets the ontology path file as input (argument 1) and load
	 * the owl file to import the ontology with the use of Hermit reasoner.
	 * 
	 * @param path
	 * @return
	 */

	public boolean importOntology(String ontologyPath) {
		try {
			ontologyFile = new File(ontologyPath);
			manager = OWLManager.createOWLOntologyManager();
			ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
			reasonerFactory = new Reasoner.ReasonerFactory();
			reasoner = reasonerFactory.createReasoner(ontology);
			return true;
		} catch (OWLOntologyCreationException e) {
			logger.severe("Ontology Cannot be imported");
			return false;
		}

	}

	/**
	 * Two Enum Datatypes for assigning Labels and Relationships for the Neo4j
	 * graph.
	 */

	public enum GraphLabels implements Label {
		Class, Individual, Relationship, Property;
	}

	public enum GraphRelationships implements RelationshipType {
		is_a;
	}

	/**
	 * This method gets the graph path as input ((args[0]) and remodel/convert
	 * the ontology components to Neo4j graph entities.
	 * 
	 * @param graphPath
	 */

	@SuppressWarnings("deprecation")
	public boolean remodelToGraph(String graphPath) {

		graphFile = new File(graphPath);
		dbFactory = new GraphDatabaseFactory();
		dbDataService = dbFactory.newEmbeddedDatabase(graphFile);
		graphTx = dbDataService.beginTx();

		try {
            // Source for the coding idea: https://neo4j.com/blog/using-owl-with-neo4j/
			// Creating owl:thing node (root class)
			Node thingNode = dbDataService.createNode();
			thingNode.addLabel(GraphLabels.Class);
			thingNode.setProperty("name", ROOT_URI);

			// Iterating and converting all the classes from ontology to graph
			for (OWLClass c : ontology.getClassesInSignature(true)) {
				classString = c.toString();
				if (classString.contains("#")) {
					classString = classString.substring(classString.indexOf("#") + 1, classString.lastIndexOf(">"));
				}
				Node classNode = dbDataService.createNode();
				classNode.setProperty("name", classString);

				// Distinguishing direct superclasses.
				NodeSet<OWLClass> superclasses = reasoner.getSuperClasses(c, true);
				if (superclasses.isEmpty()) {
					classNode.createRelationshipTo(thingNode, GraphRelationships.is_a);
				} else {
					for (org.semanticweb.owlapi.reasoner.Node<OWLClass> parentOWLNode : superclasses) {
						OWLClassExpression parent = parentOWLNode.getRepresentativeElement();
						String parentString = parent.toString();
						if (parentString.contains("#")) {
							parentString = parentString.substring(parentString.indexOf("#") + 1,
									parentString.lastIndexOf(">"));
						}
						// Creating is_a relationship between classes and superclasses.
						Node parentNode = dbDataService.createNode();
						parentNode.addLabel(GraphLabels.Class);						
						// Alternative: More complex relationships can be created, e.g. rdf:type, rdfs:subClassOf
						classNode.createRelationshipTo(parentNode, GraphRelationships.is_a);
						parentNode.setProperty("name", parentString);
					}
				}
				// Iterating and converting all individuals for each class from ontology to graph.
				for (org.semanticweb.owlapi.reasoner.Node<OWLNamedIndividual> in : reasoner.getInstances(c, true)) {
					OWLNamedIndividual i = in.getRepresentativeElement();
					String indString = i.toString();
					if (indString.contains("#")) {
						indString = indString.substring(indString.indexOf("#") + 1, indString.lastIndexOf(">"));
					}
					Node individualNode = dbDataService.createNode();
					individualNode.createRelationshipTo(classNode, GraphRelationships.is_a);
					individualNode.addLabel(GraphLabels.Individual);
					individualNode.setProperty("name", indString);

					// Iterating and converting all object properties and data properties for each individual from ontology to graph.
					for (OWLObjectPropertyExpression objectProperty : ontology.getObjectPropertiesInSignature()) {
						for (org.semanticweb.owlapi.reasoner.Node<OWLNamedIndividual> object : reasoner
								.getObjectPropertyValues(i, objectProperty)) {
							String reltype = objectProperty.toString();
							reltype = reltype.substring(reltype.indexOf("#") + 1, reltype.lastIndexOf(">"));
							String s = object.getRepresentativeElement().toString();
							s = s.substring(s.indexOf("#") + 1, s.lastIndexOf(">"));
							Node objectNode = dbDataService.createNode();
							objectNode.addLabel(GraphLabels.Property);
							individualNode.createRelationshipTo(objectNode, DynamicRelationshipType.withName(reltype));
						}
						// Iterating and converting all data properties for each individual from ontology to graph.
						for (OWLDataPropertyExpression dataProperty : ontology.getDataPropertiesInSignature()) {
							for (OWLLiteral object : reasoner.getDataPropertyValues(i,
									dataProperty.asOWLDataProperty())) {
								String reltype = dataProperty.asOWLDataProperty().toString();
								reltype = reltype.substring(reltype.indexOf("#") + 1, reltype.lastIndexOf(">"));
								String s = object.toString();
								individualNode.setProperty(reltype, s);
							}
						}
					}
				}
			}
			// Graph Transaction is completed.
			graphTx.success();
			logger.info("Graph transaction is successfully completed.");
			return true;
		}
		catch (Exception e) {
			logger.severe("Graph transaction is failed.");
			graphTx.failure();
			return false;
		} finally {
			graphTx.terminate();
			dbDataService.shutdown();
			fh.close();
		}
	}

}
