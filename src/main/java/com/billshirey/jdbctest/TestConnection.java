package com.billshirey.jdbctest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Properties;


/**
 * Test a connection and query to a database via JDBC
 * 
 * To use this test class copy jdbctest.props.sample to jdbctest.props and edit
 * to match your database information.  All parameters in the jdbctest.props
 * are required except "sid" and "db.url".  If db.url is not provided then it
 * is assumed you are connecting to Oracle and a url will be generated using
 * the sid (which is then required).
 * 
 * A jdbc library matching your db system must be included in the classpath when
 * running. 
 * 
 * @author shirey
 *
 */
public class TestConnection
{
	private static final String PROP_FILE_NAME = "jdbctest.props";
	
	private static String serverName;
	private static String portStr;
	private static String driverClass;
	private static String username;
	private static String password;
	private static String sid;
	private static Integer port;
	private static String testQuery;
	private static String dbUrl;
	
	public static void main(String [] args)
	{
		//load and check the required properties from the file jdbctest.props
		loadProperties();
		System.out.println("Properties file " + PROP_FILE_NAME + " successfully loaded");
		
		//check the connection to the server and port, with a direct socket connection
		//this will timeout after 2 seconds if a connection can't be made
		if(! checkConnection())
		{
			System.err.println("Unable to connect to port " + portStr + " at " + serverName);
			System.exit(1);
		}
		System.out.println("Connection test to " + serverName + ":" + portStr + " successful.");
		
		//connect via jdbc and run the query specified in the properties file
		runQuery();
	}
	
	/**
	 * Connect to the database via JDBC and run the query specifed in the properties file
	 */
	private static void runQuery()
	{
		//test loading the jdbc class first
		try{Class.forName(driverClass);}
		catch(Throwable t)
		{
			System.err.println("Unable to load driver class " + driverClass);
			System.err.println("Check to make sure the correct driver is specifed in the -cp argument in run.sh");
			System.exit(1);
		}
		System.out.println("Driver " + driverClass + " Successfully loaded.");
		System.out.println("Using the following url to connect to the database: " + dbUrl);
		//create a connection
		Connection conn = null;
		try{conn = DriverManager.getConnection(dbUrl, username, password);}
		catch(Throwable t)
		{
			System.err.println("Unable to connect to " + dbUrl);
			System.err.println("Username: " + username);
			t.printStackTrace(System.err);
			try{if(conn != null) conn.close();}
			catch(Throwable tt){System.err.println("Error closing connection."); tt.printStackTrace(System.err);}
			System.exit(1);
		}
		
		//remove a trailing semi-colon from the query- jdbc doesn't like this..
		String sql = (testQuery.endsWith(";"))?testQuery.substring(0, testQuery.length() - 1): testQuery;		
		try
		{
			//run the query
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(sql);
			ResultSetMetaData meta = rs.getMetaData();
			int nCols = meta.getColumnCount();
			//print the resulting data
			while(rs.next())
			{
				for(int i = 1; i <= nCols; i++)
					System.out.print(meta.getColumnName(i) + ":" + rs.getString(i));
				System.out.println("");
			}
		}
		catch(Throwable t)
		{
			System.err.println("Error while executing query " + testQuery);
			t.printStackTrace(System.err);
			System.exit(1);
		}
		
		System.out.println("Success running query.");
	}
	
	/**
	 * Check a TCP connection.  Try to connect to a port,
	 * if it times out after two seconds fail
	 */
	public static boolean checkConnection()
	{
		try{
			Socket s = new Socket();
			s.connect(new InetSocketAddress(serverName, port), 2000);
			try{s.close();}catch(Throwable t){}
			return true;
	    } catch (Exception ex) {
	        ex.printStackTrace(System.err);
	    }
	    return false;
	}	
	
	private static void loadProperties()
	{
		File f = new File(PROP_FILE_NAME);
		if(! f.exists())
		{
			System.err.println("file " + PROP_FILE_NAME +" does not exist.");
			System.exit(1);
		}
		if(! f.canRead())
		{
			System.err.println("file " + PROP_FILE_NAME + " is not readable.");
			System.exit(1);
		}
		InputStream is = null;
		try{is = new FileInputStream(f);}
		catch(FileNotFoundException fnf)
		{
			System.err.println("file " + PROP_FILE_NAME + " was not found.");
			fnf.printStackTrace(System.err);
			System.exit(1);
		}
		Properties props = new Properties();
		try{props.load(is);}
		catch(Throwable t)
		{
			System.err.println("unable to load " + PROP_FILE_NAME);
			t.printStackTrace(System.err);
			System.exit(1);
		}
		
		serverName = props.getProperty("db.server.name");
		portStr = props.getProperty("db.port");
		sid = props.getProperty("sid");
		username = props.getProperty("db.username");
		password = props.getProperty("db.password");
		driverClass = props.getProperty("driver.class");
		testQuery = props.getProperty("test.query");
		dbUrl = props.getProperty("db.url");
		
		testProperty(serverName, "db.server.name");
		testProperty(portStr, "db.port");
		testProperty(username, "db.username");
		testProperty(password, "db.password");
		testProperty(driverClass, "driver.class");
		testProperty(testQuery, "test.query");
		
		portStr = portStr.trim();
		serverName = serverName.trim();
		username = username.trim();
		password = password.trim();
		driverClass = driverClass.trim();
		testQuery = testQuery.trim();
		
		try{port = Integer.parseInt(portStr);}
		catch(Throwable t)
		{
			System.err.println("Specified port " + portStr + " could not be parsed as a valid integer.");
			System.exit(1);
		}

		if(isEmpty(dbUrl))
		{
			if(isEmpty(sid))
			{
				System.err.println("Both the db.url and sid properties are empty in " + PROP_FILE_NAME + " at least one must be provided.");
				System.exit(1);
			}
			System.out.println("db.url not provided, generating one for Oracle.");
			dbUrl = "jdbc:oracle:thin:@" + serverName + ":" + portStr + ":" + sid.trim();
		}
		dbUrl = dbUrl.trim();
	}	
	
	private static void testProperty(String val, String propName)
	{
		if(isEmpty(val))
		{
			System.err.println("Required property " + propName + " is not set in " + PROP_FILE_NAME);
			System.exit(1);
		}
	}
	private static boolean isEmpty(String val)
	{
		if(val == null) return(true);
		return(val.trim().equals(""));
	}
	
}
