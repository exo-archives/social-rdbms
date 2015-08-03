/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.addons.updater;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 30, 2015  
 */
public final class MigrationContext {
  public static final String SOC_RDBMS_MIGRATION_STATUS_KEY = "SOC_RDBMS_MIGRATION_DONE";
  public static final String SOC_RDBMS_ACTIVITY_MIGRATION_KEY = "SOC_RDBMS_ACTIVITY_MIGRATION_DONE";
  public static final String SOC_RDBMS_ACTIVITY_CLEANUP_KEY = "SOC_RDBMS_ACTIVITY_CLEANUP_DONE";
  public static final String SOC_RDBMS_CONNECTION_MIGRATION_KEY = "SOC_RDBMS_CONNECTION_MIGRATION_DONE";
  public static final String SOC_RDBMS_CONNECTION_CLEANUP_KEY = "SOC_RDBMS_CONNECTION_CLEANUP_DONE";
  //
  private static boolean isDone = false;
  private static boolean isActivityDone = false;
  private static boolean isActivityCleanupDone = false;
  private static boolean isConnectionDone = false;
  private static boolean isConnectionCleanupDone = false;

  public static boolean isDone() {
    return isDone;
  }

  public static void setDone(boolean isDoneArg) {
    isDone = isDoneArg;
  }

  public static boolean isActivityDone() {
    return isActivityDone;
  }

  public static void setActivityDone(boolean isActivityDoneArg) {
    isActivityDone = isActivityDoneArg;
  }

  public static boolean isConnectionDone() {
    return isConnectionDone;
  }

  public static void setConnectionDone(boolean isConnectionDoneArg) {
    isConnectionDone = isConnectionDoneArg;
  }

  public static boolean isActivityCleanupDone() {
    return isActivityCleanupDone;
  }

  public static void setActivityCleanupDone(boolean isActivityCleanupDone) {
    MigrationContext.isActivityCleanupDone = isActivityCleanupDone;
  }

  public static boolean isConnectionCleanupDone() {
    return isConnectionCleanupDone;
  }

  public static void setConnectionCleanupDone(boolean isConnectionCleanupDone) {
    MigrationContext.isConnectionCleanupDone = isConnectionCleanupDone;
  }

}
