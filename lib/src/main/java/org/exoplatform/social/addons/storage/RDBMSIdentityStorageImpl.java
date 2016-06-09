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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.exoplatform.commons.api.persistence.DataInitializer;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.user.UserStateModel;
import org.exoplatform.services.user.UserStateService;
import org.exoplatform.social.addons.rest.IdentityAvatarRestService;
import org.exoplatform.social.addons.search.ExtendProfileFilter;
import org.exoplatform.social.addons.search.ProfileSearchConnector;
import org.exoplatform.social.addons.storage.dao.ActivityDAO;
import org.exoplatform.social.addons.storage.dao.IdentityDAO;
import org.exoplatform.social.addons.storage.dao.SpaceDAO;
import org.exoplatform.social.addons.storage.entity.ActivityEntity;
import org.exoplatform.social.addons.storage.entity.IdentityEntity;
import org.exoplatform.social.addons.storage.entity.ProfileEntity;
import org.exoplatform.social.addons.storage.entity.ProfileExperienceEntity;
import org.exoplatform.social.addons.storage.entity.SpaceEntity;
import org.exoplatform.social.core.identity.SpaceMemberFilterListAccess;
import org.exoplatform.social.core.identity.model.ActiveIdentityFilter;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.profile.ProfileLoader;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.IdentityStorageException;
import org.exoplatform.social.core.storage.impl.IdentityStorageImpl;
import org.exoplatform.social.core.storage.impl.StorageUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 5, 2015  
 */
public class RDBMSIdentityStorageImpl extends IdentityStorageImpl {

  private static final Log LOG = ExoLogger.getLogger(RDBMSIdentityStorageImpl.class);

  private final ActivityDAO activityDAO;
  private final IdentityDAO identityDAO;
  private final SpaceDAO spaceDAO;

  private final OrganizationService orgService;

  private ProfileSearchConnector profileSearchConnector;

  // TODO: This service is injected to be used to workaround issue COMMONS-478
  // We should remove this when the issue is fixed
  private final DataInitializer dataInitializer;
  private boolean isDataInitialized = false;

  public RDBMSIdentityStorageImpl(DataInitializer dataInitializer,
                                  IdentityDAO identityDAO,                                   
                                  SpaceDAO spaceDAO, ActivityDAO activityDAO,
                                  ProfileSearchConnector profileSearchConnector, OrganizationService orgService) {
    this.dataInitializer = dataInitializer;
    this.identityDAO = identityDAO;
    this.spaceDAO = spaceDAO;
    this.activityDAO = activityDAO;
    this.profileSearchConnector = profileSearchConnector;
    this.orgService = orgService;
  }

  // TODO: This method is introduced to workaround for issue COMMONS-478
  // We should remove it when this issue is fixed
  private IdentityDAO getIdentityDAO() {
    if (!isDataInitialized) {
      this.dataInitializer.initData();
      this.isDataInitialized = true;
    }
    return identityDAO;
  }

  public void setProfileSearchConnector(ProfileSearchConnector profileSearchConnector) {
    this.profileSearchConnector = profileSearchConnector;
  }

  private Identity convertToIdentity(IdentityEntity entity) {
    return convertToIdentity(entity, true);
  }
  private Identity convertToIdentity(IdentityEntity entity, boolean mapDeleted) {
    if (entity.isDeleted() && !mapDeleted) {
      return null;
    }

    Identity identity = new Identity(entity.getStringId());
    mapToIdentity(entity, identity);
    return identity;
  }
  private void mapToIdentity(IdentityEntity entity, Identity identity) {
    identity.setProviderId(entity.getProviderId());
    identity.setRemoteId(entity.getRemoteId());
    if (entity.getProfile() != null) {
      identity.setProfile(convertToProfile(entity.getProfile(), identity));
    } else {
      identity.setProfile(null);
      identity.setProfileLoader(new RDBMSProfileLoader(this, identity));
    }
    identity.setEnable(entity.isEnabled());
    identity.setDeleted(entity.isDeleted());
  }
  private Profile convertToProfile(ProfileEntity entity, Identity identity) {
    Profile p = new Profile(identity);
    p.setId(String.valueOf(identity.getId()));
    mapToProfile(entity, p);
    if (OrganizationIdentityProvider.NAME.equals(identity.getProviderId()) && p.getProperty(Profile.USERNAME) == null) {
      p.getProperties().put(Profile.USERNAME, identity.getRemoteId());
    }
    return p;
  }
  private void mapToProfile(ProfileEntity entity, Profile p) {
    Map<String, Object> props = p.getProperties();
    String providerId = entity.getIdentity().getProviderId();
    if (!OrganizationIdentityProvider.NAME.equals(providerId) && !SpaceIdentityProvider.NAME.equals(providerId)) {
      p.setUrl(entity.getUrl());
      p.setAvatarUrl(entity.getAvatarURL());
    } else {
      String remoteId = entity.getIdentity().getRemoteId();
      if (OrganizationIdentityProvider.NAME.equals(providerId)) {
        p.setUrl(LinkProvider.getUserProfileUri(remoteId));

      } else if (SpaceIdentityProvider.NAME.equals(providerId)) {
        if (spaceDAO.getSpaceByPrettyName(remoteId) != null) {
          p.setUrl(LinkProvider.getSpaceUri(remoteId));
        }
      }

      if (entity.getAvatarMimeType() != null && !entity.getAvatarMimeType().isEmpty() && entity.getAvatarImage() != null && entity.getAvatarImage().length > 0) {
        Identity identity = p.getIdentity();
        p.setAvatarUrl(IdentityAvatarRestService.buildAvatarURL(identity.getProviderId(), identity.getRemoteId()));
      }
    }

    List<ProfileExperienceEntity> experiences = entity.getExperiences();
    if (experiences != null && experiences.size() > 0) {
      List<Map<String, Object>> xpData = new ArrayList<>();
      for (ProfileExperienceEntity exp : experiences){
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
      props.put(Profile.EXPERIENCES, xpData);
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

          props.put(key, list);

        } else {
          props.put(key, value);
        }
      }
    }

    p.setCreatedTime(entity.getCreatedDate().getTime());
    p.setLastLoaded(System.currentTimeMillis());
  }

  private void mapToEntity(Identity identity, IdentityEntity entity) {
    entity.setProviderId(identity.getProviderId());
    entity.setRemoteId(identity.getRemoteId());
    entity.setEnabled(identity.isEnable());
    entity.setDeleted(identity.isDeleted());
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
        List<ProfileExperienceEntity> list = new ArrayList<>();

        for (Map<String, String> exp : exps) {
          ProfileExperienceEntity ex = new ProfileExperienceEntity();
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
          entityProperties.put(e.getKey(), String.valueOf(val));
        }
      }
    }

    entity.setProperties(entityProperties);

    Date created = profile.getCreatedTime() <= 0 ? new Date() : new Date(profile.getCreatedTime());
    entity.setCreatedDate(created);
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
    } else {
      entity = getIdentityDAO().findByProviderAndRemoteId(identity.getProviderId(), identity.getRemoteId());
    }

    if (entity == null) {
      entity = new IdentityEntity();
    }
    mapToEntity(identity, entity);

    if (entity.getId() > 0) {
      getIdentityDAO().update(entity);
    } else {
      entity = getIdentityDAO().create(entity);
    }
    identity.setId(entity.getStringId());
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

    mapToEntity(identity, entity);
    entity = getIdentityDAO().update(entity);

    return convertToIdentity(entity, true);
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
  @ExoTransactional
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
    this.hardDeleteIdentity(identity);
  }

  /**
   * Hard delete an identity from JCR
   *
   * @param identity
   * @throws IdentityStorageException
   */
  @ExoTransactional
  public void hardDeleteIdentity(final Identity identity) throws IdentityStorageException {
    long id = parseId(identity.getId());
    String username = identity.getRemoteId();
    String provider = identity.getProviderId();

    IdentityEntity entity = getIdentityDAO().find(id);
    if (entity != null) {
      ProfileEntity profileEntity = entity.getProfile();
      if (profileEntity != null) {
        profileEntity.getProperties().put(Profile.DELETED, "true");
      }      
      entity.setDeleted(true);
      getIdentityDAO().update(entity);
    }

    EntityManager em = CommonsUtils.getService(EntityManagerService.class).getEntityManager();
    Query query;

    // Delete all connection
    query = em.createNamedQuery("SocConnection.deleteConnectionByIdentity");
    query.setParameter("identityId", id);
    query.executeUpdate();

    if(OrganizationIdentityProvider.NAME.equals(provider)) {
      // Delete space-member
      query = em.createNamedQuery("SpaceMember.deleteByUsername");
      query.setParameter("username", username);
      query.executeUpdate();
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
    ProfileEntity entity = identityDAO.findByIdentityId(identityId);

    if (entity == null) {
      createProfile(profile);
      entity = identityDAO.findByIdentityId(identityId);
    }

    if (entity == null) {
      return null;
    } else {
      profile.setId(String.valueOf(entity.getIdentity().getId()));
      mapToProfile(entity, profile);
      profile.clearHasChanged();
      return profile;
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
    ProfileEntity entity = (id == 0 ? null : identityDAO.findByIdentityId(id));
    if (entity == null) {
      createProfile(profile);
    } else {
      mapToProfileEntity(profile, entity);
      identityDAO.update(entity.getIdentity());
    }
    profile.clearHasChanged();
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

    // Find by identity to avoid create 2 profile map to 1 identity (exception will be throw because the unique constraint)
    ProfileEntity entity = identityDAO.findByIdentityId(identityId);
    if (entity == null) {
      entity = new ProfileEntity();
      entity.setIdentity(identityEntity);
    }

    mapToProfileEntity(profile, entity);

    entity.setCreatedDate(new Date());

    identityDAO.update(identityEntity);
    profile.setId(String.valueOf(identityId));
  }

  /**
   * Updates profile.
   *
   * @param profile the profile
   * @throws IdentityStorageException
   * @since 1.2.0-GA
   */
  public void updateProfile(final Profile profile) throws IdentityStorageException {
    if (profile.getId() == null) {
      createProfile(profile);
    } else {
      long id = parseId(profile.getId());
      ProfileEntity entity = identityDAO.findByIdentityId(id);
      if (entity == null) {
        throw new IdentityStorageException(IdentityStorageException.Type.FAIL_TO_UPDATE_PROFILE, "Profile does not exist on RDBMS");
      } else {
        mapToProfileEntity(profile, entity);
        identityDAO.update(entity.getIdentity());
      }
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
    String t = "SPACE_ACTIVITY";
    if (type == Profile.AttachedActivityType.USER) {
      t = "USER_PROFILE_ACTIVITY";
    } else if (type == Profile.AttachedActivityType.RELATIONSHIP) {
      t = "USER_ACTIVITIES_FOR_RELATIONSHIP";
    }
    List<ActivityEntity> activities = activityDAO.getActivitiesByPoster(profile.getIdentity(), 0, 1, t);
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
    Set<String> activeUsers = new HashSet<String>();
    //by userGroups
    if (filter.getUserGroups() != null) {
      StringTokenizer stringToken = new StringTokenizer(filter.getUserGroups(), ActiveIdentityFilter.COMMA_SEPARATOR);
      try {
        while(stringToken.hasMoreTokens()) {
          try {
            ListAccess<User> listAccess = orgService.getUserHandler().findUsersByGroupId(stringToken.nextToken().trim());
            User[] users = listAccess.load(0, listAccess.getSize());
            //
            for(User u : users) {
              activeUsers.add(u.getUserName());
            }
          } catch (Exception e) {
            LOG.error(e.getMessage(), e);
          }
        }
      } catch (Exception e) {
        LOG.error(e.getMessage());
      }
    }

    //by N days
    if (filter.getDays() > 0) {
      activeUsers = StorageUtils.getLastLogin(filter.getDays());
    }

    //Gets online users and push to activate users
    if (CommonsUtils.getService(UserStateService.class) != null) {
      List<UserStateModel> onlines = CommonsUtils.getService(UserStateService.class).online();
      for (UserStateModel user : onlines) {
        activeUsers.add(user.getUserId());
      }
    }


    return activeUsers;
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
    entity.setEnabled(isEnable);
    getIdentityDAO().update(entity);
  }

  @Override
  public List<Identity> getIdentitiesByFirstCharacterOfName(String providerId,
                                                            ProfileFilter profileFilter,
                                                            long offset,
                                                            long limit,
                                                            boolean forceLoadOrReloadProfile) throws IdentityStorageException {
    return getIdentitiesByProfileFilter(providerId, profileFilter, offset, limit, forceLoadOrReloadProfile);
  }

  @Override
  public List<Identity> getIdentitiesForMentions(String providerId,
                                                 ProfileFilter profileFilter,
                                                 long offset,
                                                 long limit,
                                                 boolean forceLoadOrReloadProfile) throws IdentityStorageException {
    return getIdentitiesByProfileFilter(providerId, profileFilter, offset, limit, forceLoadOrReloadProfile);
  }

  @Override
  public int getIdentitiesByProfileFilterCount(String providerId, ProfileFilter profileFilter) throws IdentityStorageException {
    ExtendProfileFilter xFilter = new ExtendProfileFilter(profileFilter);
    ListAccess<IdentityEntity> list = getIdentityDAO().findIdentities(xFilter);

    try {
      return list.getSize();
    } catch (Exception e) {
      return 0;
    }
  }

  @Override
  public int getIdentitiesByFirstCharacterOfNameCount(String providerId, ProfileFilter profileFilter) throws IdentityStorageException {
    ExtendProfileFilter xFilter = new ExtendProfileFilter(profileFilter);
    xFilter.setProviderId(providerId);

    ListAccess<IdentityEntity> list = getIdentityDAO().findIdentities(xFilter);
    try {
      return list.getSize();
    } catch (Exception ex) {
      return 0;
    }
  }

  public List<Identity> getIdentitiesForUnifiedSearch(final String providerId,
                                                      final ProfileFilter profileFilter,
                                                      long offset, long limit) throws IdentityStorageException {
    return profileSearchConnector.search(null, profileFilter, null, offset, limit);
  }

  public List<Identity> getSpaceMemberIdentitiesByProfileFilter(final Space space,
                                                                final ProfileFilter profileFilter,
                                                                SpaceMemberFilterListAccess.Type type,
                                                                long offset, long limit) throws IdentityStorageException {

    List<Long> relations = new ArrayList<>();
    if (space != null) {
      try {
        SpaceEntity gotSpace = spaceDAO.find(Long.parseLong(space.getId()));
        String[] members = null;
        switch (type) {
          case MEMBER:
            members = gotSpace.getMembersId();
            break;
          case MANAGER:
            members = gotSpace.getManagerMembersId();
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

        for (int i = 0; i < members.length; i++) {
          Identity identity = findIdentity(OrganizationIdentityProvider.NAME, members[i]);
          if (identity != null) {
            relations.add(parseId(identity.getId()));
          }
        }
      } catch (IdentityStorageException e) {
        throw new IdentityStorageException(IdentityStorageException.Type.FAIL_TO_FIND_IDENTITY);
      }
      if (relations.isEmpty()) {
        relations.add(-1L);
      }
    }

    ExtendProfileFilter xFilter = new ExtendProfileFilter(profileFilter);
    xFilter.setIdentityIds(relations);
    ListAccess<IdentityEntity> list = getIdentityDAO().findIdentities(xFilter);
    return convertToIdentities(list, offset, limit);
  }

  public List<Identity> getIdentitiesByProfileFilter(final String providerId,
                                                     final ProfileFilter profileFilter, long offset, long limit,
                                                     boolean forceLoadOrReloadProfile)  throws IdentityStorageException {
    ExtendProfileFilter xFilter = new ExtendProfileFilter(profileFilter);
    xFilter.setProviderId(providerId);
    xFilter.setForceLoadProfile(forceLoadOrReloadProfile);

    ListAccess<IdentityEntity> list = getIdentityDAO().findIdentities(xFilter);

    return convertToIdentities(list, offset, limit);
  }

  public ListAccess<Identity> findByFilter(ExtendProfileFilter filter) {
    final ListAccess<IdentityEntity> list = getIdentityDAO().findIdentities(filter);

    return new ListAccess<Identity>() {
      @Override
      public Identity[] load(int offset, int size) throws Exception, IllegalArgumentException {
        IdentityEntity[] entities = list.load(offset, size);
        if (entities == null || entities.length == 0) {
          return new Identity[0];
        } else {
          Identity[] identities = new Identity[entities.length];
          for (int i = 0; i < entities.length; i++) {
            identities[i] = convertToIdentity(entities[i]);
          }
          return identities;
        }
      }

      @Override
      public int getSize() throws Exception {
        return list.getSize();
      }
    };
  }

  /**
   * This method is introduced to clean totally identity from database
   * It's used in unit test
   * @param identity
   */
  @ExoTransactional
  public void removeIdentity(Identity identity) {
    long id = parseId(identity.getId());
    String username = identity.getRemoteId();
    String provider = identity.getProviderId();

    IdentityEntity entity = getIdentityDAO().find(id);

    EntityManager em = CommonsUtils.getService(EntityManagerService.class).getEntityManager();
    Query query;

    // Delete all connection
    query = em.createNamedQuery("SocConnection.deleteConnectionByIdentity");
    query.setParameter("identityId", id);
    query.executeUpdate();

    if(OrganizationIdentityProvider.NAME.equals(provider)) {
      // Delete space-member
      query = em.createNamedQuery("SpaceMember.deleteByUsername");
      query.setParameter("username", username);
      query.executeUpdate();
    }

    if (entity != null) {
      getIdentityDAO().delete(entity);
    }
  }

  private List<Identity> convertToIdentities(ListAccess<IdentityEntity> list, long offset, long limit) {
    try {
      return convertToIdentities(list.load((int)offset, (int)limit));
    } catch (Exception ex) {
      return Collections.emptyList();
    }
  }

  private List<Identity> convertToIdentities(IdentityEntity[] entities) {
    if (entities == null || entities.length == 0) {
      return Collections.emptyList();
    }

    List<Identity> result = new ArrayList<>(entities.length);
    for (IdentityEntity entity : entities) {
      Identity idt = convertToIdentity(entity);
      if (idt != null) {
        result.add(idt);
      }
    }
    return result;
  }

  public static class RDBMSProfileLoader implements ProfileLoader {
    final RDBMSIdentityStorageImpl storage;
    final Identity identity;

    public RDBMSProfileLoader(RDBMSIdentityStorageImpl storage, Identity identity) {
      this.storage = storage;
      this.identity = identity;
    }

    @Override
    public Profile load() throws IdentityStorageException {
      Profile p = new Profile(identity);
      return storage.loadProfile(p);
    }
  }
}
