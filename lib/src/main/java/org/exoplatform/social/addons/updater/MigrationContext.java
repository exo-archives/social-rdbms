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
  private static boolean isDone = false;
  
  private static boolean isActivityDone = false;
  private static boolean isProfileDone = false;
  private static boolean isConnectionDone = false;

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

  public static boolean isProfileDone() {
    return isProfileDone;
  }

  public static void setProfileDone(boolean isProfileDoneArg) {
    isProfileDone = isProfileDoneArg;
  }

  public static boolean isConnectionDone() {
    return isConnectionDone;
  }

  public static void setConnectionDone(boolean isConnectionDoneArg) {
    isConnectionDone = isConnectionDoneArg;
  }

}
