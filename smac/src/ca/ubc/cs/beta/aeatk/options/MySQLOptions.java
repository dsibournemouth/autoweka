package ca.ubc.cs.beta.aeatk.options;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

import ca.ubc.cs.beta.aeatk.misc.file.HomeFileUtils;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParameterFile;

@UsageTextField(title="MySQL Options", description="Options that control how to connect to the MySQL Server")
public class MySQLOptions extends AbstractOptions {

	@UsageTextField(defaultValues="~/.aeatk/mysql.opt")
	@Parameter(names={"--mysql-defaults-file","--mysqlDefaultsFile"}, description="file that contains default settings for MySQL")
	@ParameterFile(ignoreFileNotExists = true) 
	public File mysqlDefaults = HomeFileUtils.getHomeFile(".aeatk" + File.separator  + "mysql.opt");
	
	@Parameter(names={"--mysql-hostname","--mysql-host","--mysqlHostName"}, description="Hostname of database server" )
	public String host;
	
	
	@Parameter(names={"--mysql-password","--mysqlPassword"}, description="Password of database server" )
	public String password;
	
	@Parameter(names={"--mysql-database","--mysqlDatabase", "--mysqlDatabaseName"}, description="Name of Database" )
	public String databaseName = null;
	
	
	@Parameter(names={"--mysql-username","--mysqlUsername","--mysql_user"}, description="Username of the Database")
	public String username;
	
	@Parameter(names={ "--mysql-port","--mysqlPort",}, description="Port of database server")
	public int port = 3306;
	
	@Parameter(names={"--mysql-parameter-file","--mysqlParameterFile"}, description="MySQL Configuration Options")
	@ParameterFile
	public File mysqlParamFile = null;

	public String getJDBCString()
	{
		if(databaseName == null)
		{
			throw new ParameterException("Database name was not specified");
		} 
		
		if(port <=0 || port >= 65536)
		{
			throw new ParameterException("Port must be between [1,65535]");
		}
		
		if((host == null) || (host.trim().length() == 0))
		{
			throw new ParameterException("host must be specified, and a non-empty string");
		}
		return "jdbc:mysql://"+host + ":" + port + "/" + databaseName+"?username="+username + "&password="+password;
		
	}
	
	public Connection getConnection()
	{
		
		try {
			
			return DriverManager.getConnection(getJDBCString(),username, password);
			
		} catch (Exception e) {
			throw new IllegalStateException(e);
			
		}
		
		
	}
}
