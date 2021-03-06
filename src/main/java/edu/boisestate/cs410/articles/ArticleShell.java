package edu.boisestate.cs410.articles;

import com.budhash.cliche.Command;
import com.budhash.cliche.ShellFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.sql.*;

@CommandLine.Command(name="shell")
public class ArticleShell implements Runnable {
    @CommandLine.ParentCommand
    private ArticleMain main;

    private Connection db;

    public ArticleShell() {}

    public ArticleShell(Connection cxn) {
        db = cxn;
    }

    @Command
    public void topAuthors() throws SQLException {
        String query = "SELECT author_id, author_name, COUNT(article_id) AS article_count" +
                " FROM author JOIN article_author USING (author_id)" +
                " GROUP BY author_id" +
                " ORDER BY article_count DESC LIMIT 10";
        System.out.println("Top Authors by Publication Count:");
        try (Statement stmt = db.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String name = rs.getString("author_name");
                int count = rs.getInt("article_count");
                System.out.format("  %s (with %d pubs)\n", name, count);
            }
        }
    }

    @Command
    public void conferenceTopAuthors(String series) throws SQLException {
        String query = "SELECT author_id, author_name, COUNT(article_id) AS article_count" +
                " FROM author JOIN article_author USING (author_id)" +
                " JOIN article USING (article_id)" +
                " JOIN proceedings USING (pub_id)" +
                " JOIN conf_series USING (cs_id)" +
                " WHERE cs_hb_key = ?" +
                " GROUP BY author_id" +
                " ORDER BY article_count DESC LIMIT 10";
        System.out.format("Top Authors in %s by Publication Count:%n", series);
        try (PreparedStatement stmt = db.prepareStatement(query)) {
            // Set the first parameter (query key) to the series
            stmt.setString(1, series);
            // once parameters are bound we can run!
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("author_name");
                    int count = rs.getInt("article_count");
                    System.out.format("  %s (with %d pubs)\n", name, count);
                }
            }
        }
    }

    /**
     * Search titles and abstracts by text.
     */
    @Command
    public void search(String query) throws SQLException {
        String sql = "SELECT article_id, title" +
                " FROM article" +
                " WHERE MATCH (title, abstract) AGAINST (?)";
        System.out.format("Articles matching %s:%n", query);
        try (PreparedStatement stmt = db.prepareStatement(sql)) {
            // Set the first parameter (query key) to the series
            stmt.setString(1, query);
            // once parameters are bound we can run!
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("article_id");
                    String title = rs.getString("title");
                    System.out.format("%d\t%s%n", id, title);
                }
            }
        }
    }

    @Override
    public void run() {
        try (Connection cxn = main.openDatabase()) {
            db = cxn;
            ShellFactory.createConsoleShell("article", "", this)
                        .commandLoop();
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
