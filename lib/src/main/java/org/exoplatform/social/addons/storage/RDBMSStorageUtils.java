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
package org.exoplatform.social.addons.storage;

import java.util.ArrayList;
import java.util.List;

import org.chromattic.api.ChromatticSession;
import org.chromattic.api.query.Ordering;
import org.chromattic.api.query.QueryBuilder;
import org.chromattic.api.query.QueryResult;
import org.chromattic.core.query.QueryImpl;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.common.lifecycle.SocialChromatticLifeCycle;
import org.exoplatform.social.core.chromattic.entity.DisabledEntity;
import org.exoplatform.social.core.chromattic.entity.HidableEntity;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.entity.ProfileEntity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.impl.StorageUtils;
import org.exoplatform.social.core.storage.query.WhereExpression;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 8, 2015  
 */
public class RDBMSStorageUtils {

  public static List<Identity> getRelationshipsByFilter(List<Identity> relations, ProfileFilter filter, long offset, final long limit) {
    if (relations.isEmpty()) return new ArrayList<Identity>();
    //
    List<Identity> found = new ArrayList<Identity>();
    if(relations.isEmpty()) return found ;
    QueryBuilder<ProfileEntity> builder = getChromatticSession().createQueryBuilder(ProfileEntity.class);
    WhereExpression whereExpression = new WhereExpression();
    StorageUtils.applyWhereFromIdentity(whereExpression, relations);
    //
    StorageUtils.applyFilter(whereExpression, filter);
    //
    builder.where(whereExpression.toString()).orderBy(ProfileEntity.fullName.getName(), Ordering.ASC);
    
    QueryImpl<ProfileEntity> queryImpl = (QueryImpl<ProfileEntity>) builder.get();
    ((org.exoplatform.services.jcr.impl.core.query.QueryImpl) queryImpl.getNativeQuery()).setCaseInsensitiveOrder(true);
    QueryResult<ProfileEntity> result = queryImpl.objects(offset, limit);
    
    IdentityStorage identityStorage = CommonsUtils.getService(IdentityStorage.class);
    while(result.hasNext()) {
      IdentityEntity current = result.next().getIdentity();
      if (_getMixin(current, DisabledEntity.class, false) != null) {
        continue;
      }
      Identity i = new Identity(current.getProviderId(), current.getRemoteId());
      i.setId(current.getId());
      i.setProfile(identityStorage.loadProfile(i.getProfile()));
      found.add(i);
    }
    //
    return found;
  }
  
  public static int getRelationshipsCountByFilter(final List<Identity> relations, final ProfileFilter filter) {
    if (relations.size() == 0) {
      return 0;
    }
    //
    QueryBuilder<ProfileEntity> builder = getChromatticSession().createQueryBuilder(ProfileEntity.class);
    //
    WhereExpression whereExpression = new WhereExpression();
    StorageUtils.applyWhereFromIdentity(whereExpression, relations);
    //
    StorageUtils.applyFilter(whereExpression, filter);
    //
    QueryResult<ProfileEntity> result = builder.where(whereExpression.toString()).get().objects();
    int number = 0;
    while (result.hasNext()) {
      IdentityEntity current = result.next().getIdentity();
      if (_getMixin(current, DisabledEntity.class, false) == null) {
        ++number;
      }
    }
    //
    return number;
  }
  
  private static ChromatticSession getChromatticSession() {
    ChromatticManager manager = CommonsUtils.getService(ChromatticManager.class);
    SocialChromatticLifeCycle lifeCycle = (SocialChromatticLifeCycle)
                                          manager.getLifeCycle(SocialChromatticLifeCycle.SOCIAL_LIFECYCLE_NAME);
    return lifeCycle.getSession();
  }
  
  private static <M> M _getMixin(Object o, Class<M> mixinType, boolean create) {
    M mixin = getChromatticSession().getEmbedded(o, mixinType);
    if (mixin == null && create) {
      mixin = getChromatticSession().create(mixinType);
      getChromatticSession().setEmbedded(o, mixinType, mixin);
    }
    //Fix for case old activity node without mixinType Hidable.
    if (mixin != null && mixinType.equals(HidableEntity.class)) {
      HidableEntity hidableEntity = (HidableEntity) mixin;
      if (hidableEntity.getHidden() == null) {
        hidableEntity.setHidden(false);
        getChromatticSession().save();
      }
    }
    return mixin;
  }
}
