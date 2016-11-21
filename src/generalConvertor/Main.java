package generalConvertor;

public class Main {
	public static void main(String[] args) {
		Ontology2Graph ontologyRemodeller = new Ontology2Graph();
		if (args.length == 2) 
		{
			if (ontologyRemodeller.importOntology(args[0])) 
			{
				System.out.println("The ontology is successfully imported.");
				if (ontologyRemodeller.remodelToGraph(args[1])) 
				{
					System.out.println("The ontology is successfully remodelled to Neo4j Graph.");
				}
			}

		} 
		else 
		{
			System.out.println("Usage --  ontologyFilePath  graphFilePath");
			System.exit(1);
		}
	}

}
