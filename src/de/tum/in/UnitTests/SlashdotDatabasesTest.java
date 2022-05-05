package de.tum.in.UnitTests;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.tum.in.SocialWebCrawler.Comment;
import de.tum.in.SocialWebCrawler.CommentDatabase;
import de.tum.in.SocialWebCrawler.Relationship;
import de.tum.in.SocialWebCrawler.RelationshipDatabase;


public class SlashdotDatabasesTest extends TestCase {

	public void testRelationshipDatabase()
	{
		RelationshipDatabase db = new RelationshipDatabase("tmp");
		db.append(new Relationship("a", "b", "friend"));
		db.append(new Relationship("a", "c", "foe"));
		db.append(new Relationship("b", "a", "friend"));
		db.appendEmpty("x");

		List<Relationship> l = db.query("a");
		assertNotNull(l);
		assertEquals(2, l.size());
		l = db.query("b");
		assertNotNull(l);
		assertEquals(1, l.size());
		l = db.query("x");
		assertNotNull(l);
		assertEquals(0, l.size());
		l = db.query("foo");
		assertNull(l);

		try
		{
			db.write();
		}
		catch (IOException ex)
		{
			fail(ex.getMessage());
		}
		File dbFile = new File(db.getFileName());
		assertTrue(dbFile.exists());

		db = new RelationshipDatabase(dbFile);
		l = db.query("b");
		assertNotNull(l);
		assertEquals(1, l.size());
		Relationship r = l.get(0);
		assertNotNull(r);
		assertEquals("b", r.from);
		assertEquals("a", r.to);
		assertEquals("friend", r.type);
		l = db.query("x");
		assertNotNull(l);
		assertEquals(0, l.size());

		dbFile.deleteOnExit();
	}

	public void testCommentDatabase()
	{
		Comment c1 = new Comment(1234);
		c1.parentId = 4004;
		c1.title = "longcat is long";
		c1.score = 5;
		c1.scoreType = "Insightful";
		c1.author = "dude1";
		c1.date = "Today, 13:37";
		c1.body = "foo bar baz\nfoooooo!";

		Comment c2 = new Comment(5678);
		c2.parentId = 8008;
		c2.title = "blasdfsd;dsfdf;;dfdfdsf";
		c2.score = -1;
		c2.scoreType = "Insightful";
		c2.author = "dude2";
		c2.date = "Today, 14:37";
		c2.body = "foo bar; baz\nfoooooo!;;sdas;%;sdsd%%sdsd\n\n";

		CommentDatabase db = new CommentDatabase("tmp");
		db.append(c1);
		db.append(c2);

		try
		{
			db.write();
		}
		catch (IOException ex)
		{
			fail(ex.getMessage());
		}
		File dbFile = new File(db.getFileName());
		assertTrue(dbFile.exists());

		db = new CommentDatabase(dbFile);
		Comment c = db.query(1234);
		assertNotNull(c);
		assertEquals(c1.id, c.id);
		assertEquals(c1.parentId, c.parentId);
		assertEquals(c1.title, c.title);
		assertEquals(c1.score, c.score);
		assertEquals(c1.scoreType, c.scoreType);
		assertEquals(c1.author, c.author);
		assertEquals(c1.date, c.date);
		assertEquals(c1.body, c.body);
		c = db.query(5678);
		assertNotNull(c);
		assertEquals(c2.id, c.id);
		assertEquals(c2.parentId, c.parentId);
		assertEquals(c2.title, c.title);
		assertEquals(c2.score, c.score);
		assertEquals(c2.scoreType, c.scoreType);
		assertEquals(c2.author, c.author);
		assertEquals(c2.date, c.date);
		assertEquals(c2.body, c.body);

		dbFile.deleteOnExit();		
	}
}
