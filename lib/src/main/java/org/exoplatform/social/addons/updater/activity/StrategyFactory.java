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
package org.exoplatform.social.addons.updater.activity;

import org.exoplatform.social.core.chromattic.entity.IdentityEntity;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 22, 2015  
 */
public class StrategyFactory {
  enum CLEAN_TYPE {
    DAY("DAY", DayCleanup.activityDayCleanup, DayCleanup.activityDayRefCleanup),
    MONTH("MONTH", MonthCleanup.activityMonthCleanup, MonthCleanup.activityMonthRefCleanup),
    YEAR("YEAR", YearCleanup.activityYearCleanup, YearCleanup.activityYearRefCleanup);

    private AbstractStrategy<IdentityEntity> refClean, activityClean;
    private String type;

    private CLEAN_TYPE(String type, AbstractStrategy<IdentityEntity> activityClean, AbstractStrategy<IdentityEntity> refClean) {
      this.activityClean = activityClean;
      this.refClean = refClean;
      this.type = type;
    }

    public AbstractStrategy<IdentityEntity> getRefClean() {
      return refClean;
    }

    public void setRefClean(AbstractStrategy<IdentityEntity> refClean) {
      this.refClean = refClean;
    }

    public AbstractStrategy<IdentityEntity> getActivityClean() {
      return activityClean;
    }

    public void setActivityClean(AbstractStrategy<IdentityEntity> activityClean) {
      this.activityClean = activityClean;
    }

    public static CLEAN_TYPE getCleanType(String type) {
      for (CLEAN_TYPE cleanType : values()) {
        if (cleanType.type.equalsIgnoreCase(type)) {
          return cleanType;
        }
      }
      return DAY;
    }
  }

  public static AbstractStrategy<IdentityEntity> getActivityCleanupStrategy(String strategyName) {
    return CLEAN_TYPE.getCleanType(strategyName).getRefClean();
  }
  
  public static AbstractStrategy<IdentityEntity> getActivityRefCleanupStrategy(String strategyName) {
    return CLEAN_TYPE.getCleanType(strategyName).getActivityClean();
  }
}
