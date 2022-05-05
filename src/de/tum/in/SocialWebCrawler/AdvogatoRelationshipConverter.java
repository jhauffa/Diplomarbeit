package de.tum.in.SocialWebCrawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class AdvogatoRelationshipConverter {

	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("not enough arguments");
			System.exit(1);
		}

		String outRelDbName = args[0];
		outRelDbName = outRelDbName.substring(0, outRelDbName.lastIndexOf('.'));
		outRelDbName += ".csv";
		RelationshipDatabase relDb = new RelationshipDatabase(
				new File(outRelDbName));

		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(args[0]));
			boolean insideGraphDecl = false;
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.contains("{"))
					insideGraphDecl = true;
				else if (line.contains("}"))
					insideGraphDecl = false;
				else if (insideGraphDecl)
				{
					line = line.trim();
					if (!line.startsWith("/*"))
					{
						String[] parts = line.split(" -> ");
						if (parts.length == 2)
						{
							String to = parts[1].substring(0,
									parts[1].indexOf(' '));
							String type = parts[1].substring(
									parts[1].indexOf('\"') + 1,
									parts[1].lastIndexOf('\"'));
							relDb.append(new Relationship(parts[0], to, type));
						}
					}
				}
			}

			relDb.write();
		}
		catch (IOException ex)
		{
		}
	}

}
