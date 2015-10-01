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

import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.addons.search.ProfileIndexingServiceConnector;
import org.exoplatform.social.core.relationship.RelationshipEvent;
import org.exoplatform.social.core.relationship.RelationshipListenerPlugin;
import org.exoplatform.social.core.relationship.model.Relationship;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 29, 2015  
 */
public class ConnectionESListenerImpl extends RelationshipListenerPlugin {

  @Override
  public void requested(RelationshipEvent event) {
    reindexRelationship(event.getPayload());
  }

  @Override
  public void denied(RelationshipEvent event) {
    reindexRelationship(event.getPayload());
  }

  @Override
  public void confirmed(RelationshipEvent event) {
    reindexRelationship(event.getPayload());
  }

  @Override
  public void ignored(RelationshipEvent event) {
    reindexRelationship(event.getPayload());
  }

  @Override
  public void removed(RelationshipEvent event) {
    reindexRelationship(event.getPayload());
  }
  
  private void reindexRelationship(Relationship relationship) {
    IndexingService indexingService  = CommonsUtils.getService(IndexingService.class);
    indexingService.reindex(ProfileIndexingServiceConnector.TYPE, relationship.getReceiver().getId());
    indexingService.reindex(ProfileIndexingServiceConnector.TYPE, relationship.getSender().getId());
  }
  
}
