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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.exoplatform.commons.api.persistence.DataInitializer;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.social.addons.search.ProfileSearchConnector;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.dao.IdentityDAO;
import org.exoplatform.social.addons.storage.dao.ProfileDAO;
import org.exoplatform.social.addons.storage.entity.Activity;
import org.exoplatform.social.addons.storage.entity.IdentityEntity;
import org.exoplatform.social.addons.storage.entity.ProfileEntity;
import org.exoplatform.social.addons.storage.entity.ProfileExperience;
import org.exoplatform.social.core.identity.SpaceMemberFilterListAccess;
import org.exoplatform.social.core.identity.model.ActiveIdentityFilter;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.IdentityStorageException;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.storage.impl.IdentityStorageImpl;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 5, 2015  
 */
public class RDBMSIdentityStorageImpl extends IdentityStorageImpl {

  private ActivityDAO activityDAO = null;
  private IdentityDAO identityDAO = null;
  private ProfileDAO profileDAO = null;
  private SpaceStorage spaceStorage = null;

  private final DataInitializer dataInitializer;

  public RDBMSIdentityStorageImpl(DataInitializer dataInitializer) {
    System.out.println("Initial here");
    this.dataInitializer = dataInitializer;
  }

  private ActivityDAO getActivityDAO() {
    if (activityDAO == null) {
      activityDAO = CommonsUtils.getService(ActivityDAO.class);
    }
    return activityDAO;
  }

  private IdentityDAO getIdentityDAO() {
    if (identityDAO == null) {
      identityDAO = CommonsUtils.getService(IdentityDAO.class);

      //TODO: This is temporary to workaround, please consider
      this.dataInitializer.initData();
    }
    return identityDAO;
  }

  private ProfileDAO getProfileDAO() {
    if (profileDAO == null) {
      profileDAO = CommonsUtils.getService(ProfileDAO.class);
    }
    return profileDAO;
  }

  private SpaceStorage getSpaceStorage() {
    if (spaceStorage == null) {
      spaceStorage = PortalContainer.getInstance().getComponentInstanceOfType(SpaceStorage.class);
    }
    return spaceStorage;
  }

  private Identity convertToIdentity(IdentityEntity entity) {
    Identity identity = new Identity(String.valueOf(entity.getId()));
    mapToIdentity(entity, identity);
    return identity;
  }
  private void mapToIdentity(IdentityEntity entity, Identity identity) {
    identity.setProviderId(entity.getProviderId());
    identity.setRemoteId(entity.getRemoteId());
    if (entity.getProfile() != null) {
      identity.setProfile(convertToProfile(entity.getProfile(), identity));
    } else {
      identity.setProfile(new Profile(identity));
    }
    identity.setEnable(entity.isEnable());
    identity.setDeleted(entity.isDeleted());
  }
  private Profile convertToProfile(ProfileEntity entity, Identity identity) {
    Profile p = new Profile(identity);
    p.setId(String.valueOf(entity.getId()));
    mapToProfile(entity, p);
    return p;
  }
  private void mapToProfile(ProfileEntity entity, Profile p) {
    String providerId = entity.getIdentity().getProviderId();
    if (!OrganizationIdentityProvider.NAME.equals(providerId) && !SpaceIdentityProvider.NAME.equals(providerId)) {
      p.setUrl(entity.getUrl());
      p.setAvatarUrl(entity.getAvatarURL());
    } else {
      String remoteId = entity.getIdentity().getRemoteId();
      if (OrganizationIdentityProvider.NAME.equals(providerId)) {
        p.setUrl(LinkProvider.getUserProfileUri(remoteId));

      } else if (SpaceIdentityProvider.NAME.equals(providerId)) {
        spaceStorage = getSpaceStorage();
        if (spaceStorage.getSpaceByPrettyName(remoteId) != null) {
          p.setUrl(LinkProvider.getSpaceUri(remoteId));
        }
      }

      if (entity.getAvatarMimeType() != null && entity.getAvatarMimeType().isEmpty() && entity.getAvatarImage().length > 0) {
        //TODO: calculate the avatar URL
        p.setAvatarUrl("/identity/avatar");
      }
    }

    List<ProfileExperience> experiences = entity.getExperiences();
    if (experiences != null && experiences.size() > 0) {
      List<Map<String, Object>> xpData = new ArrayList<>();
      for (ProfileExperience exp : experiences){
        Map<String, Object> xpMap = new HashMap<String, Object>();
        xpMap.put(Profile.EXPERIENCES_SKILLS, exp.getSkills());
        xpMap.put(Profile.EXPERIENCES_POSITION, exp.getPosition());
        xpMap.put(Profile.EXPERIENCES_START_DATE, exp.getStartDate());
        xpMap.put(Profile.EXPERIENCES_END_DATE, exp.getEndDate());
        xpMap.put(Profile.EXPERIENCES_COMPANY, exp.getCompany());
        xpMap.put(Profile.EXPERIENCES_DESCRIPTION, exp.getDescription());
        xpMap.put(Profile.EXPERIENCES_IS_CURRENT, exp.isCurrent());
        xpData.add(xpMap);
      }
      p.setProperty(Profile.EXPERIENCES, xpData);
    }

    Map<String, String> properties = entity.getProperties();
    if (properties != null && properties.size() > 0) {
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();

        if (Profile.CONTACT_IMS.equals(key) || Profile.CONTACT_PHONES.equals(key) || Profile.CONTACT_URLS.equals(key)) {
          List<Map<String, String>> list = new ArrayList<>();
          try {
            JSONArray arr = new JSONArray(value);
            for (int i = 0 ; i < arr.length(); i++) {
              Map<String, String> map = new HashMap<>();
              JSONObject json = arr.getJSONObject(i);
              Iterator<String> keys = json.keys();
              while (keys.hasNext()) {
                String k = keys.next();
                map.put(k, json.optString(k));
              }
              list.add(map);
            }
          } catch (JSONException ex) {
            // Ignore this exception
          }

          p.setProperty(key, list);

        } else {
          p.setProperty(key, value);
        }
      }
    }

    p.setCreatedTime(entity.getCreatedTime());
    p.setLastLoaded(System.currentTimeMillis());
  }

  private IdentityEntity convertToEntity(Identity identity) {
    IdentityEntity entity = new IdentityEntity();
    entity.setId(parseId(identity.getId()));
    mapToEntity(identity, entity);
    return entity;
  }
  private void mapToEntity(Identity identity, IdentityEntity entity) {
    entity.setProviderId(identity.getProviderId());
    entity.setRemoteId(identity.getRemoteId());
    entity.setEnable(identity.isEnable());
    entity.setDeleted(identity.isDeleted());
  }
  private ProfileEntity convertToProfileEntity(Profile profile) {
    ProfileEntity entity = new ProfileEntity();
    entity.setId(parseId(profile.getId()));
    mapToProfileEntity(profile, entity);
    return entity;
  }

  private void mapToProfileEntity(Profile profile, ProfileEntity entity) {
    String providerId = profile.getIdentity().getProviderId();
    if (!OrganizationIdentityProvider.NAME.equals(providerId) && !SpaceIdentityProvider.NAME.equals(providerId)) {
      entity.setUrl(profile.getUrl());
      entity.setAvatarURL(profile.getAvatarUrl());
    }

    Map<String, String> entityProperties = entity.getProperties();
    if (entityProperties == null) {
      entityProperties = new HashMap<>();
    }

    Map<String, Object> properties = profile.getProperties();
    for (Map.Entry<String, Object> e : properties.entrySet()) {
      if (Profile.AVATAR.equalsIgnoreCase(e.getKey())) {
        AvatarAttachment attachment = (AvatarAttachment) e.getValue();
        entity.setAvatarImage(attachment.getImageBytes());
        entity.setAvatarMimeType(attachment.getMimeType());

      } else if (Profile.EXPERIENCES.equalsIgnoreCase(e.getKey())){

        List<Map<String, String>> exps = (List<Map<String, String>>)e.getValue();
        List<ProfileExperience> list = new ArrayList<>();

        for (Map<String, String> exp : exps) {
          ProfileExperience ex = new ProfileExperience();
          ex.setCompany(exp.get(Profile.EXPERIENCES_COMPANY));
          ex.setPosition(exp.get(Profile.EXPERIENCES_POSITION));
          ex.setStartDate(exp.get(Profile.EXPERIENCES_START_DATE));
          ex.setEndDate(exp.get(Profile.EXPERIENCES_END_DATE));
          ex.setSkills(exp.get(Profile.EXPERIENCES_SKILLS));
          ex.setDescription(exp.get(Profile.EXPERIENCES_DESCRIPTION));

          list.add(ex);
        }

        entity.setExperiences(list);

      } else if (Profile.CONTACT_IMS.equals(e.getKey())
              || Profile.CONTACT_PHONES.equals(e.getKey())
              || Profile.CONTACT_URLS.equals(e.getKey())) {

        List<Map<String, String>> list = (List<Map<String, String>>) e.getValue();
        JSONArray arr = new JSONArray();
        for (Map<String, String> map : list) {
          JSONObject json = new JSONObject(map);
          arr.put(json);
        }

        entityProperties.put(e.getKey(), arr.toString());

      } else if (!Profile.EXPERIENCES_SKILLS.equals(e.getKey())) {
        Object val = e.getValue();
        if (val != null) {
          entityProperties.put(e.getKey(), (String)val);
        }
      }
    }

    entity.setProperties(entityProperties);

    entity.setCreatedTime(profile.getCreatedTime());
  }

  private long parseId(String id) {
    try {
      return Long.parseLong(id);
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  /**
   * Saves identity.
   *
   * @param identity the identity
   * @throws IdentityStorageException
   */
  public void saveIdentity(final Identity identity) throws IdentityStorageException {
    long id = parseId(identity.getId());

    IdentityEntity entity = null;
    if (id > 0) {
      entity = getIdentityDAO().find(id);
    }

    if (entity == null) {
      entity = new IdentityEntity();
    }
    mapToEntity(identity, entity);

    if (entity.getId() > 0) {
      getIdentityDAO().update(entity);
    } else {
      entity = getIdentityDAO().create(entity);
      identity.setId(String.valueOf(entity.getId()));
    }
  }

  /**
   * Updates existing identity's properties.
   *
   * @param identity the identity to be updated.
   * @return the updated identity.
   * @throws IdentityStorageException
   * @since  1.2.0-GA
   */
  public Identity updateIdentity(final Identity identity) throws IdentityStorageException {
    long id = parseId(identity.getId());

    IdentityEntity entity = null;
    if (id > 0) {
      entity = getIdentityDAO().find(id);
    }

    if (entity == null) {
      throw new IdentityStorageException(IdentityStorageException.Type.FAIL_TO_UPDATE_IDENTITY, "The identity does not exist on DB");
    }

    entity = getIdentityDAO().update(entity);

    return convertToIdentity(entity);
  }

  /**
   * Updates existing identity's membership in OrganizationService.
   *
   * @param remoteId the remoteId to be updated membership.
   * @return the updated identity.
   * @throws IdentityStorageException
   * @since  4.0.0
   */
  public void updateIdentityMembership(final String remoteId) throws IdentityStorageException {
    // We do not need to implement this method,
    // only clear Identity Caching when user updated Group, what raised by Organization Service
  }

  /**
   * Gets the identity by his id.
   *
   * @param nodeId the id of identity
   * @return the identity
   * @throws IdentityStorageException
   */
  public Identity findIdentityById(final String nodeId) throws IdentityStorageException {
    long id = parseId(nodeId);
    IdentityEntity entity = getIdentityDAO().find(id);

    if (entity != null) {
      return convertToIdentity(entity);
    } else {
      return null;
    }
  }

  /**
   * Deletes an identity from JCR
   *
   * @param identity
   * @throws IdentityStorageException
   */
  public void deleteIdentity(final Identity identity) throws IdentityStorageException {
    long id = parseId(identity.getId());
    IdentityEntity entity = getIdentityDAO().find(id);
    if (entity != null) {
      entity.setDeleted(true);
      getIdentityDAO().update(entity);
    }
  }

  /**
   * Hard delete an identity from JCR
   *
   * @param identity
   * @throws IdentityStorageException
   */
  public void hardDeleteIdentity(final Identity identity) throws IdentityStorageException {
    long id = parseId(identity.getId());
    IdentityEntity entity = getIdentityDAO().find(id);
    if (entity != null) {
      getIdentityDAO().delete(entity);
    }
  }

  /**
   * Load profile.
   *
   * @param profile the profile
   * @throws IdentityStorageException
   */
  @ExoTransactional
  public Profile loadProfile(Profile profile) throws IdentityStorageException {
    long identityId = parseId(profile.getIdentity().getId());
    ProfileEntity entity = getProfileDAO().findByIdentityId(identityId);

    if (entity == null) {
      createProfile(profile);
      entity = getProfileDAO().findByIdentityId(identityId);
    }

    if (entity == null) {
      return null;
    } else {
      return convertToProfile(entity, profile.getIdentity());
    }
  }


  /**
   * Gets the identity by remote id.
   *
   * @param providerId the identity provider
   * @param remoteId   the id
   * @return the identity by remote id
   * @throws IdentityStorageException
   */
  @ExoTransactional
  public Identity findIdentity(final String providerId, final String remoteId) throws IdentityStorageException {
    try {
      IdentityEntity entity = getIdentityDAO().findByProviderAndRemoteId(providerId, remoteId);
      if (entity == null) {
        return null;
      }

      return convertToIdentity(entity);

    } catch (Exception ex) {

      throw new IdentityStorageException(IdentityStorageException.Type.FAIL_TO_FIND_IDENTITY, "Can not load identity", ex);
    }
  }

  /**
   * Saves profile.
   *
   * @param profile the profile
   * @throws IdentityStorageException
   */
  public void saveProfile(final Profile profile) throws IdentityStorageException {
    long id = parseId(profile.getId());
    ProfileEntity entity = getProfileDAO().find(id);
    if (entity == null) {
      createProfile(profile);
    } else {
      mapToProfileEntity(profile, entity);
      getProfileDAO().update(entity);
    }
  }

  private void createProfile(final Profile profile) {
    // Create profile for identity
    if (profile.getIdentity().getId() == null) {
      throw new IllegalArgumentException();
    }
    long identityId = parseId(profile.getIdentity().getId());

    IdentityEntity identityEntity = getIdentityDAO().find(identityId);
    if (identityEntity == null) {
      throw new IdentityStorageException(IdentityStorageException.Type.FAIL_TO_FIND_IDENTITY);
    }

    ProfileEntity entity = new ProfileEntity();
    entity.setIdentity(identityEntity);
    entity.setCreatedTime(System.currentTimeMillis());

    entity = getProfileDAO().create(entity);
    profile.setId(String.valueOf(entity.getId()));
  }

  /**
   * Updates profile.
   *
   * @param profile the profile
   * @throws IdentityStorageException
   * @since 1.2.0-GA
   */
  public void updateProfile(final Profile profile) throws IdentityStorageException {
    long id = parseId(profile.getId());
    ProfileEntity entity = getProfileDAO().find(id);
    if (entity == null) {
      throw new IdentityStorageException(IdentityStorageException.Type.FAIL_TO_UPDATE_PROFILE, "Profile does not exist on RDBMS");
    } else {
      mapToProfileEntity(profile, entity);
      getProfileDAO().update(entity);
    }
  }

  /**
   * Gets total number of identities in storage depend on providerId.
   * @throws IdentityStorageException
   */
  public int getIdentitiesCount (final String providerId) throws IdentityStorageException {
    return (int)getIdentityDAO().countIdentityByProvider(providerId);
  }

  /**
   * Gets the type.
   *
   * @param nodetype the nodetype
   * @param property the property
   * @return the type
   * @throws IdentityStorageException
   */
  public String getType(final String nodetype, final String property) {
    // This is not JCR implementation, so nodetype does not exist.
    return "undefined";
  }

  /**
   * Add or modify properties of profile and persist to JCR. Profile parameter is a lightweight that
   * contains only the property that you want to add or modify. NOTE: The method will
   * not delete the properties on old profile when the param profile have not those keys.
   *
   * @param profile
   * @throws IdentityStorageException
   */
  public void addOrModifyProfileProperties(final Profile profile) throws IdentityStorageException {
    updateProfile(profile);
  }



  /**
   * Updates profile activity id by type.
   *
   * @param identity
   * @param activityId
   * @param type Type of activity id to get.
   * @since 4.0.0.Alpha1
   */
  public void updateProfileActivityId(Identity identity, String activityId, Profile.AttachedActivityType type) {
    // Do not need to update in this case
    //super.updateProfileActivityId(identity, activityId, type);
  }

  /**
   * Gets profile activity id by type.
   *
   * @param profile
   * @param type Type of activity id to get.
   * @return Profile activity id.
   * @since 4.0.0.Alpha1
   */
  public String getProfileActivityId(Profile profile, Profile.AttachedActivityType type) {
    List<Activity> activities = getActivityDAO().getActivitiesByPoster(profile.getIdentity(), 0, 1, "USER_PROFILE_ACTIVITY");
    if (activities != null && activities.size() > 0) {
      return String.valueOf(activities.get(0).getId());
    } else {
      return null;
    }
  }

  /**
   * Gets the active user list base on the given ActiveIdentityFilter.
   * 1. N days who last login less than N days.
   * 2. UserGroup who belongs to this group.
   *
   * @param filter
   * @return
   * @since 4.1.0
   */
  public Set<String> getActiveUsers(ActiveIdentityFilter filter) {
    //TODO: just copy code form Old implement to here
    return super.getActiveUsers(filter);
  }

  /**
   * Process enable/disable Identity
   *
   * @param identity The Identity enable
   * @param isEnable true if the user is enable, false if not
   * @since 4.2.x
   */
  public void processEnabledIdentity(Identity identity, boolean isEnable) {
    long id = parseId(identity.getId());
    IdentityEntity entity = getIdentityDAO().find(id);
    if (entity == null) {
      throw new IllegalArgumentException("Identity does not exists");
    }
    entity.setEnable(isEnable);
    getIdentityDAO().update(entity);
  }


  @Override
  public List<Identity> getIdentitiesByFirstCharacterOfName(String providerId,
                                                            ProfileFilter profileFilter,
                                                            long offset,
                                                            long limit,
                                                            boolean forceLoadOrReloadProfile) throws IdentityStorageException {
    ProfileSearchConnector connector = CommonsUtils.getService(ProfileSearchConnector.class);
    return connector.search(null, profileFilter, null, offset, limit);
  }
  
  @Override
  public List<Identity> getIdentitiesForMentions(String providerId,
                                                 ProfileFilter profileFilter,
                                                 long offset,
                                                 long limit,
                                                 boolean forceLoadOrReloadProfile) throws IdentityStorageException {
    ProfileSearchConnector connector = CommonsUtils.getService(ProfileSearchConnector.class);
    return connector.search(null, profileFilter, null, offset, limit);
  }
  
  @Override
  public int getIdentitiesByProfileFilterCount(String providerId, ProfileFilter profileFilter) throws IdentityStorageException {
    ProfileSearchConnector connector = CommonsUtils.getService(ProfileSearchConnector.class);
    return connector.count(null, profileFilter, null);
  }
  
  @Override
  public int getIdentitiesByFirstCharacterOfNameCount(String providerId, ProfileFilter profileFilter) throws IdentityStorageException {
    ProfileSearchConnector connector = CommonsUtils.getService(ProfileSearchConnector.class);
    return connector.count(null, profileFilter, null);
  }

  //TODO: maybe need improve the search method of ProfileSearchConnector
  public List<Identity> getIdentitiesForUnifiedSearch(final String providerId,
                                                      final ProfileFilter profileFilter,
                                                      long offset, long limit) throws IdentityStorageException {
    ProfileSearchConnector connector = CommonsUtils.getService(ProfileSearchConnector.class);
    return connector.search(null, profileFilter, null, offset, limit);
  }

  public List<Identity> getSpaceMemberIdentitiesByProfileFilter(final Space space,
                                                                final ProfileFilter profileFilter,
                                                                SpaceMemberFilterListAccess.Type type,
                                                                long offset, long limit) throws IdentityStorageException {

    List<Long> relations = new ArrayList<>();
    try {
      Space gotSpace = getSpaceStorage().getSpaceById(space.getId());
      String[] members = null;
      switch (type) {
        case MEMBER:
          members = gotSpace.getMembers();
          break;
        case MANAGER:
          members = gotSpace.getManagers();
          List<String> wildcardUsers = SpaceUtils.findMembershipUsersByGroupAndTypes(space
                  .getGroupId(), MembershipTypeHandler.ANY_MEMBERSHIP_TYPE);

          for (String remoteId : wildcardUsers) {
            Identity id = findIdentity(OrganizationIdentityProvider.NAME, remoteId);
            if (id != null) {
              relations.add(parseId(id.getId()));
            }
          }
          break;
      }

      for (int i = 0; i <  members.length; i++){
        Identity identity = findIdentity(OrganizationIdentityProvider.NAME, members[i]);
        if (identity != null) {
          relations.add(parseId(identity.getId()));
        }
      }
    } catch (IdentityStorageException e){
      throw new IdentityStorageException(IdentityStorageException.Type.FAIL_TO_FIND_IDENTITY);
    }

    List<Identity> result = new ArrayList<>();
    List<IdentityEntity> entities = getIdentityDAO().findIdentityByProfileFilter(relations, profileFilter, offset, limit);
    if (entities != null || !entities.isEmpty()) {
      for (IdentityEntity entity : entities) {
        result.add(convertToIdentity(entity));
      }
    }
    return result;
  }

  public List<Identity> getIdentitiesByProfileFilter(final String providerId,
                                                     final ProfileFilter profileFilter, long offset, long limit,
                                                     boolean forceLoadOrReloadProfile)  throws IdentityStorageException {
    ProfileSearchConnector connector = CommonsUtils.getService(ProfileSearchConnector.class);
    return connector.search(null, profileFilter, null, offset, limit);
  }
}
