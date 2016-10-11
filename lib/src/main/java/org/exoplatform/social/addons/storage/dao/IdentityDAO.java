/*
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.social.addons.storage.dao;

import java.util.List;

import org.exoplatform.commons.api.persistence.GenericDAO;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.social.addons.search.ExtendProfileFilter;
import org.exoplatform.social.addons.storage.entity.IdentityEntity;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public interface IdentityDAO extends GenericDAO<IdentityEntity, Long> {
  IdentityEntity findByProviderAndRemoteId(String providerId, String remoteId);
  long countIdentityByProvider(String providerId);

  ListAccess<IdentityEntity> findIdentities(ExtendProfileFilter filter);

  List<Long> getAllIds(int offset, int limit);  

  List<Long> getAllIdsByProvider(String providerId, int offset, int limit);
}
