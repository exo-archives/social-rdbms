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

package org.exoplatform.social.addons.concurrency;

import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.addons.rest.IdentityAvatarRestService;
import org.exoplatform.social.addons.storage.RDBMSActivityStorageImpl;
import org.exoplatform.social.addons.storage.RDBMSIdentityStorageImpl;
import org.exoplatform.social.addons.storage.RDBMSSpaceStorageImpl;
import org.exoplatform.social.addons.test.BaseCoreTest;
import org.exoplatform.social.addons.test.QueryNumberTest;
import org.exoplatform.social.addons.updater.ActivityMigrationService;
import org.exoplatform.social.addons.updater.IdentityMigrationService;
import org.exoplatform.social.addons.updater.RDBMSMigrationManager;
import org.exoplatform.social.addons.updater.RelationshipMigrationService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.IdentityManagerImpl;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;
import org.exoplatform.social.core.storage.impl.IdentityStorageImpl;
import org.exoplatform.social.core.storage.impl.RelationshipStorageImpl;
import org.jboss.byteman.contrib.bmunit.BMUnit;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/component.search.configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.social.component.core.test.configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.social.test.jcr-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.social.test.portal-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.social.component.migrate.test.configuration.xml")
})
public class MigrationTest extends BaseCoreTest {
  protected final Log LOG = ExoLogger.getLogger(AsynMigrationTest.class);
  private ActivityStorageImpl activityJCRStorage;
  private IdentityStorageImpl identityJCRStorage;
  private RelationshipStorageImpl relationshipJCRStorage;

  private RDBMSIdentityStorageImpl identityJPAStorage;

  protected ActivityStorage activityStorage;
  private SpaceStorage spaceStorage;

  private IdentityMigrationService identityMigrationService;
  private ActivityMigrationService activityMigration;
  private RelationshipMigrationService relationshipMigration;
  private RDBMSMigrationManager rdbmsMigrationManager;

  private List<ExoSocialActivity> activitiesToDelete = new ArrayList<>();

  @Override
  public void setUp() throws Exception {
    begin();
    // If is query number test, init byteman
    hasByteMan = getClass().isAnnotationPresent(QueryNumberTest.class);
    if (hasByteMan) {
      count = 0;
      maxQuery = 0;
      BMUnit.loadScriptFile(getClass(), "queryBaseCount", "src/test/resources");
    }

    identityJPAStorage = getService(RDBMSIdentityStorageImpl.class);

    activityStorage = getService(ActivityStorage.class);
    spaceStorage = getService(SpaceStorage.class);

    identityJCRStorage = getService(IdentityStorageImpl.class);
    activityJCRStorage = getService(ActivityStorageImpl.class);
    relationshipJCRStorage = getService(RelationshipStorageImpl.class);


    identityManager = getService(IdentityManager.class);
    activityManager =  getService(ActivityManager.class);
    relationshipManager = getService(RelationshipManager.class);

    spaceService = getService(SpaceService.class);

    entityManagerService = getService(EntityManagerService.class);

    //


    identityMigrationService = getService(IdentityMigrationService.class);
    activityMigration = getService(ActivityMigrationService.class);
    relationshipMigration = getService(RelationshipMigrationService.class);

    activitiesToDelete = new ArrayList<>();
  }

  @Override
  public void tearDown() throws Exception {
    //super.tearDown();

    for (ExoSocialActivity activity : activitiesToDelete) {
      activityStorage.deleteActivity(activity.getId());
    }

    end();
  }

  public void testMigrateIdentityWithAvatar() throws Exception {
    // create jcr data
    rootIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", false);
    Profile rootProfile = rootIdentity.getProfile();

    InputStream inputStream = getClass().getResourceAsStream("/eXo-Social.png");
    AvatarAttachment avatarAttachment = new AvatarAttachment(null, "avatar", "png", inputStream, null, System.currentTimeMillis());
    rootProfile.setProperty(Profile.AVATAR, avatarAttachment);

    identityManager.updateProfile(rootProfile);

    end();
    begin();
    switchToUseJPAStorage();

    identityMigrationService.start();

    rootIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", false);
    rootProfile = rootIdentity.getProfile();

    assertNotNull(rootProfile.getAvatarUrl());
    assertEquals(IdentityAvatarRestService.buildAvatarURL(OrganizationIdentityProvider.NAME, "root"), rootProfile.getAvatarUrl());
  }

  public void testMigrateActivityWithMention() throws Exception {
    rootIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", false);
    johnIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "john", false);
    johnIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "demo", false);
    johnIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "mary", false);

    Map<String, String> params = new HashMap<String, String>();
    params.put("MESSAGE", "activity message for here");
    final ExoSocialActivity activity = ActivityBuilder.getInstance()
            .posterId(rootIdentity.getId())
            .title("activity title @root @mary")
            .body("activity body")
            .titleId("titleId")
            .isComment(false)
            .take();

    activityJCRStorage.setInjectStreams(false);
    activityJCRStorage.saveActivity(johnIdentity, activity);
    activityJCRStorage.setInjectStreams(true);

    end();



    activityMigration.addMigrationListener(new Listener<ExoSocialActivity, String>() {
      @Override
      public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
        String newId = event.getData();
        if (event.getSource().getId().equals(activity.getId())) {
          activity.setId(newId);
        }
      }
    });

    switchToUseJPAStorage();
    identityMigrationService.start();
    activityMigration.start();


    begin();

    ExoSocialActivity migrated = activityStorage.getActivity(activity.getId());
    activitiesToDelete.add(migrated);
    assertNotNull(migrated);
    assertEquals(2, migrated.getMentionedIds().length);
  }

  protected void switchToUseJPAStorage() {
    // Swith to use RDBMSIdentityStorage
    ((IdentityManagerImpl)identityManager).setIdentityStorage(identityJPAStorage);
    if (spaceStorage instanceof RDBMSSpaceStorageImpl) {
      ((RDBMSSpaceStorageImpl)spaceStorage).setIdentityStorage(identityJPAStorage);
    }
    if (activityStorage instanceof RDBMSActivityStorageImpl) {
      ((RDBMSActivityStorageImpl)activityStorage).setIdentityStorage(identityJPAStorage);
    }
  }
}
