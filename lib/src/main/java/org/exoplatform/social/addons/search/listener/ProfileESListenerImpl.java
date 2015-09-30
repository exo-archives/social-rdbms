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
package org.exoplatform.social.addons.search.listener;

import org.exoplatform.addons.es.domain.OperationType;
import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.profile.ProfileLifeCycleEvent;
import org.exoplatform.social.core.profile.ProfileListenerPlugin;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 29, 2015  
 */
public class ProfileESListenerImpl extends ProfileListenerPlugin {
  
  private static final Log LOG = ExoLogger.getLogger(ProfileESListenerImpl.class);

  @Override
  public void avatarUpdated(ProfileLifeCycleEvent event) {
    IndexingService indexingService = CommonsUtils.getService(IndexingService.class);
    //TODO : do not hard code
    indexingService.addToIndexingQueue("profile", event.getProfile().getIdentity().getId(), OperationType.UPDATE);
    LOG.info("Handled the updated profile avatar!");
    
  }

  @Override
  public void contactSectionUpdated(ProfileLifeCycleEvent event) {
    IndexingService indexingService = CommonsUtils.getService(IndexingService.class);
    //TODO : do not hard code
    indexingService.addToIndexingQueue("profile", event.getProfile().getIdentity().getId(), OperationType.UPDATE);
    LOG.info("Handled the updated profile contact!");
  }

  @Override
  public void experienceSectionUpdated(ProfileLifeCycleEvent event) {
    LOG.info("Handled the updated profile experience!");
  }

  @Override
  public void createProfile(ProfileLifeCycleEvent event) {
    IndexingService indexingService = CommonsUtils.getService(IndexingService.class);
    //TODO : do not hard code
    indexingService.addToIndexingQueue("profile", event.getProfile().getIdentity().getId(), OperationType.CREATE);
    LOG.info("Handled the created profile!");
  }

}
