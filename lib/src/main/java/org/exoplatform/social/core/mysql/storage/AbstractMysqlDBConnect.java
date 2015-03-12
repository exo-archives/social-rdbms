package org.exoplatform.social.core.mysql.storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Statement;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.mysql.MysqlDBConnect;
import org.exoplatform.social.core.storage.impl.AbstractStorage;
import org.picocontainer.Startable;

public abstract class AbstractMysqlDBConnect extends AbstractStorage implements Startable, MysqlDBConnect {
  private static final Log LOG  = ExoLogger.getLogger(AbstractMysqlDBConnect.class);
  private static final String SCRIPT_FILE_PATH = "/conf/mysqlDB_script.sql";

  protected static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
  
  public void start() {
    initTableValue();
  }

  public void stop() {
  }

  private void initTableValue() {
    //
    try {
      String[] inst = getConfigFileContent(SCRIPT_FILE_PATH).split(";");
      Statement statement = getDBConnection().createStatement();
      //
      for (int i = 0; i < inst.length; i++) {
        if (!inst[i].trim().isEmpty()) {
          statement.executeUpdate(inst[i]);
        }
      }
    } catch (Exception e) {
      LOG.error("Failed in executing mysql script.", e);
    }
  }

  protected String getConfigFileContent(String fileConfig) throws IOException {
    BufferedReader br = null;
    try {
      InputStream in = getClass().getResourceAsStream(fileConfig);
      br = new BufferedReader(new InputStreamReader(in));
      String s = "";
      StringBuffer sb = new StringBuffer();
      while ((s = br.readLine()) != null) {
        sb.append(s);
      }
      br.close();
      return sb.toString();
    } finally {
      if (br != null) {
        br.close();
      }
    }
  }
}
