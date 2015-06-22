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

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.exoplatform.social.common.service.TraceFactory;
import org.exoplatform.social.common.service.utils.TraceList;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 22, 2015  
 */
public abstract class AbstractStrategy<T> {
  protected final TraceList tracer;
  
  public AbstractStrategy(TraceList tracer) {
    this.tracer = tracer;
  }
  
  public AbstractStrategy() {
    tracer = TraceFactory.defaultFactory.make();
  }

  /**
   * Cleanup the entity by the strategy
   * @param node
   */
  public abstract boolean process(T entity) throws RuntimeException;
  
  /**
   * Removes the element inside the Iterator
   * @param it
   */
  protected <E> int removeElement(Iterator<E> it) {
    AtomicInteger count = new AtomicInteger(0);
    while(it.hasNext()) {
      it.remove();
      count.incrementAndGet();
    }
    return count.intValue();
  }
  
}
