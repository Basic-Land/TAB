package me.neznamy.tab.shared;

import me.neznamy.tab.shared.config.mysql.MySQL;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BasicLandHandler {

    private static boolean database = false;
    private static MySQL mySQL;

    public static void databaseConnect(MySQL mysql) {

        database = true;
        mySQL = mysql;

        try {
            mySQL.execute("CREATE TABLE IF NOT EXISTS GroupsServer(SortingID int NOT NULL PRIMARY KEY, Perm varchar(99) NULL, Skupina varchar(99) NOT NULL, TabPrefix varchar(99) NULL, TabSuffix varchar(99) NULL, TabSuffixShort varchar(99) NULL, TagPrefix varchar(99) NULL, TagSuffix varchar(99) NULL);");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public static List<String> getSortedGroups() {
        try {
            CachedRowSet crs = mySQL.getCRS("SELECT * FROM GroupsServer ORDER BY SortingID;");

            List<String> ls = new ArrayList<>();
            try {
                while(crs.next()) {
                    String val = crs.getString("Skupina");
                    ls.add(val);
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

            return ls;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getInfo(String groupName, String infoName) {
        try {
            CachedRowSet crs = mySQL.getCRS("select " + infoName + " from GroupsServer where Skupina='" + groupName + "'");
            try {
                if (crs.next()) {
                    String value = crs.getString(infoName);
                    return value.equals("") ? null : value;
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static void setTabPrefix(String groupName, String value) {
        try {
            TAB.getInstance().getConfiguration().getMysql().execute("INSERT INTO GroupsServer(Skupina,tabPrefix,tabSuffix,tagPrefix,tagSuffix) VALUES ('"+ groupName + "','"+value+"','null','null','null') ON DUPLICATE KEY UPDATE tabPrefix='"+value+"';");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static void setTabSuffix(String groupName, String value) {
        try {
            TAB.getInstance().getConfiguration().getMysql().execute("INSERT INTO GroupsServer(Skupina,tabPrefix,tabSuffix,tagPrefix,tagSuffix) VALUES ('"+ groupName + "','null','"+value+"','null','null') ON DUPLICATE KEY UPDATE tabSuffix='"+value+"';");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static void setTagPrefix(String groupName, String value) {
        try {
            TAB.getInstance().getConfiguration().getMysql().execute("INSERT INTO GroupsServer(Skupina,tabPrefix,tabSuffix,tagPrefix,tagSuffix) VALUES ('"+ groupName + "','null','null','"+value+"','null') ON DUPLICATE KEY UPDATE tagPrefix='"+value+"';");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static void setTagSuffix(String groupName, String value) {
        try {
            TAB.getInstance().getConfiguration().getMysql().execute("INSERT INTO GroupsServer(Skupina,tabPrefix,tabSuffix,tagPrefix,tagSuffix) VALUES ('"+ groupName + "','null','null','null','"+value+"') ON DUPLICATE KEY UPDATE tagSuffix='"+value+"';");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean use() {
        return database;
    }

}
