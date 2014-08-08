package org.exoplatform.social.core.mysql;

import java.sql.Connection;

public interface MysqlDBConnect {
  public static final String USER = "root";
  public static final String PASS = "root";
  public static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
  public static final String DB_URL = "jdbc:mysql://localhost:3306/social";
  public static final String DATASOURCE_CONTEXT = "exo-mysql-activity";
  
  Connection getDBConnection();
}
