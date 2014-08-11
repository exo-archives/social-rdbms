package org.exoplatform.social.core.mysql.storage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.mysql.MysqlDBConnect;
import org.picocontainer.Startable;

public class MysqlJNDIConnection implements Startable, MysqlDBConnect {
  private static final Log LOG = ExoLogger.getLogger(MysqlJNDIConnection.class);
  private static final String SCRIPT_FILE_PATH = "/conf/mysqlDB_script.sql";
  private static final String RESOURCE_NAME = "java:/comp/env/exo-mysql-activity";
  
  @Override
  public Connection getDBConnection() {
    try {
      Context initContext = new InitialContext();
      DataSource datasource = (DataSource) initContext.lookup(RESOURCE_NAME);
      return datasource.getConnection();
    } catch (NamingException ex) {
      LOG.error("Cannot get connection: ", ex);
    } catch (SQLException e) {
      LOG.error("Failed to connect mysql with Resource name " + RESOURCE_NAME + " : ", e);
    }
    return null;
  }

  public void start() {
    initTableValue();
  }

  public void stop() {
  }

  private void initTableValue() {
    //
    String s = new String();
    StringBuffer sb = new StringBuffer();
    try {
      InputStream in = getClass().getResourceAsStream(SCRIPT_FILE_PATH);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));

      while ((s = br.readLine()) != null) {
        sb.append(s);
      }
      br.close();

      Statement statement = getDBConnection().createStatement();
      //
      String[] inst = sb.toString().split(";");
      for (int i = 0; i < inst.length; i++) {
        if (!inst[i].trim().equals("")) {
          statement.execute(inst[i]);
        }
      }
    } catch (Exception e) {
      LOG.error("Failed in executing mysql script.", e);
    }
  }
}