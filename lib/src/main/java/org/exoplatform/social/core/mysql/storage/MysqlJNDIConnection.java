package org.exoplatform.social.core.mysql.storage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.mysql.MysqlDBConnect;
import org.picocontainer.Startable;

public class MysqlJNDIConnection implements Startable, MysqlDBConnect {
  private static final Log LOG = ExoLogger.getLogger(MysqlJNDIConnection.class);
  private static final String SCRIPT_FILE_PATH = "/conf/mysqlDB_script.sql";
  private static final String DATASOURCE = "java:/comp/env/exo-mysql-activity";
  private static final String ACTIVITY_TABLE = "activity";
  
  private Statement statement;
  @Override
  public Connection getDBConnection() {
    Connection result = null;
    try {
      InitialContext ctx = new InitialContext();
      DataSource dts = (DataSource) ctx.lookup(DATASOURCE);
      result = dts.getConnection();
    } catch (Exception ex) {
      LOG.error("Cannot get connection: ", ex);
    } 
    return result;
  }

  public void start() {
    initDB();
  }

  public void stop() {
  }

  private void initDB() {
    if (isDataNotExisted()) {
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
  
        String[] inst = sb.toString().split(";");
  
        for (int i = 0; i < inst.length; i++) {
          if (!inst[i].trim().equals("")) {
            getStatement().executeUpdate(inst[i]);
          }
        }
  
      } catch (Exception e) {
        LOG.error("Failed in executing mysql script.", e);
      }
    }
  }

  private boolean isDataNotExisted() {
    try {
      DatabaseMetaData dbm = getDBConnection().getMetaData();
      ResultSet tables = dbm.getTables(null, null, ACTIVITY_TABLE, null);
      if (tables.next()) {
        return false;
      } else {
        return true;
      }
    } catch (Exception e) {
      return true;
    }
  }
  
  private Statement getStatement() {
    try {
      if (statement == null) {
        this.statement = getDBConnection().createStatement();
      }
      return statement;
    } catch (Exception e) {
      LOG.error("Failed in getting connection.", e);
    }
    return null;
  }
}
