package de.tum.in.SocialWebCrawler;

public class Comment {

	public int id;
	public int parentId;
	public String title;
	public int score;
	public String scoreType;
	public String author;
	public String date;
	public String body;

	public Comment(int id)
	{
		this.id = id;
		this.title = "";
		this.scoreType = "";
		this.author = "";
		this.date = "";
		this.body = "";
	}

}
