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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.addons.updater.activity;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.updater.ActivityMigrationService;
import org.exoplatform.social.common.service.utils.TraceElement;
import org.exoplatform.social.core.chromattic.entity.ActivityListEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityRefListEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityRefYearEntity;
import org.exoplatform.social.core.chromattic.entity.ActivityYearEntity;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.storage.impl.AbstractStorage;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          tuvd@exoplatform.com
 * Jun 22, 2015  
 */
public class YearCleanup {
  /**
   * Cleanup the activity from YearNode
   */
  public static AbstractStrategy<IdentityEntity> activityYearRefCleanup = new AbstractStrategy<IdentityEntity>() {
    private final Log LOG = ExoLogger.getLogger("ActivityRefYearCleanupStrategy");
    private final String PROCESS_NAME = "ACTIVITY_REF_YEAR_CLEANUP_PROCESSING";
    public boolean process(IdentityEntity entity) throws RuntimeException {

      TraceElement trace = tracer.addElement(ActivityMigrationService.EVENT_LISTENER_KEY, PROCESS_NAME);
      AtomicInteger size = new AtomicInteger(0);
      try {
        trace.start();
        //feed
        RefYearIterator dayIter = new RefYearIterator(entity.getStreams().getAll());
        size.addAndGet(removeElement(dayIter));
        //connections
        dayIter = new RefYearIterator(entity.getStreams().getConnections());
        size.addAndGet(removeElement(dayIter));
        //my spaces
        dayIter = new RefYearIterator(entity.getStreams().getMySpaces());
        size.addAndGet(removeElement(dayIter));

        //my activities
        dayIter = new RefYearIterator(entity.getStreams().getOwner());
        size.addAndGet(removeElement(dayIter));

        //my activities
        dayIter = new RefYearIterator(entity.getStreams().getSpace());
        size.addAndGet(removeElement(dayIter));

        trace.end();
        return true;
      } catch (RuntimeException e) {
        LOG.error(e.getMessage(), e);
        return false;
      } finally {
        LOG.info(trace.toString());
        LOG.info(PROCESS_NAME + "::removed size:: " + size.intValue());
      }
    };
  };

  /**
   * Cleanup the activity from YearNode
   */
  public static AbstractStrategy<IdentityEntity> activityYearCleanup = new AbstractStrategy<IdentityEntity>() {
    private final Log LOG = ExoLogger.getLogger("ActivityYearCleanupStrategy");
    private final String PROCESS_NAME = "ACTIVITY_YEAR_CLEANUP_PROCESSING";
    @Override
    public boolean process(IdentityEntity entity) throws RuntimeException {
      TraceElement trace = tracer.addElement(ActivityMigrationService.EVENT_LISTENER_KEY, PROCESS_NAME);
      int size = 0;
      try {
        trace.start();
        YearIterator dayIter = new YearIterator(entity.getActivityList());
        size = removeElement(dayIter);
        trace.end();
        return true;
      } catch (RuntimeException e) {
        return false;
      } finally {
        LOG.info(trace.toString());
        LOG.info(PROCESS_NAME + "::removed size:: " + size);
      }
    };
  };

  /**
   * The RefYearIterator over a ActivityRefYear collection.
   * The underlying resources implemented by org.chromattic.core.mapper.onetomany.hierarchical.AnyChildList
   * The invocation removes the element will be invoked the method in org.chromattic.core.mapper.onetomany.hierarchical.AnyChildList#remove
   * 
   * @author tuvd
   *
   */
  public static class RefYearIterator implements Iterator<ActivityRefYearEntity> {

    private Iterator<ActivityRefYearEntity> yearIterator;
    
    RefYearIterator(final ActivityRefListEntity listEntity) {
      this.yearIterator = listEntity.getYears().values().iterator();
    }

    @Override
    public boolean hasNext() {
      if (yearIterator != null && yearIterator.hasNext()) {
        return true;
      }
      return false;
    }

    @Override
    public ActivityRefYearEntity next() {
      if (hasNext()) {
        return yearIterator.next();
      } else {
        throw new RuntimeException();
      }
    }

    @Override
    public void remove() {
      if (hasNext()) {
        AbstractStorage.lifecycleLookup().getSession().remove(this.next());
      }
    }
  }

  /**
   * The YearIterator over a ActivityYear collection.
   * The underlying implemented by org.chromattic.core.mapper.onetomany.hierarchical.AnyChildList
   * The invocation removes the element will be invoked the method in org.chromattic.core.mapper.onetomany.hierarchical.AnyChildList#remove
   * 
   * @author thanhvc
   *
   */
  public static class YearIterator implements Iterator<ActivityYearEntity> {

    private Iterator<ActivityYearEntity> yearIterator;
    
    YearIterator(final ActivityListEntity listEntity) {
      this.yearIterator = listEntity.getYears().values().iterator();
    }

    @Override
    public boolean hasNext() {
      if (yearIterator != null && yearIterator.hasNext()) {
        return true;
      }
      return false;
    }

    @Override
    public ActivityYearEntity next() {
      if (hasNext()) {
        return yearIterator.next();
      } else {
        throw new RuntimeException();
      }
    }

    @Override
    public void remove() {
      if (hasNext()) {
        AbstractStorage.lifecycleLookup().getSession().remove(this.next());
      }
    }
  }
}
