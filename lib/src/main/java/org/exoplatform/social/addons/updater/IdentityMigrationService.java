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

package org.exoplatform.social.addons.updater;

import org.chromattic.ext.ntdef.NTFile;
import org.chromattic.ext.ntdef.Resource;
import org.exoplatform.addons.es.index.IndexingService;
import org.exoplatform.commons.api.event.EventManager;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.social.addons.search.ProfileIndexingServiceConnector;
import org.exoplatform.social.addons.storage.RDBMSIdentityStorageImpl;
import org.exoplatform.social.addons.updater.utils.IdentityUtil;
import org.exoplatform.social.core.chromattic.entity.DisabledEntity;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.entity.ProfileEntity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.social.core.storage.exception.NodeNotFoundException;
import org.exoplatform.social.core.storage.impl.IdentityStorageImpl;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
@Managed
@ManagedDescription("Social migration Identities from JCR to RDBMS.")
@NameTemplate({@Property(key = "service", value = "social"), @Property(key = "view", value = "migration-identities") })
public class IdentityMigrationService extends AbstractMigrationService<Identity> {

  public static final String EVENT_LISTENER_KEY = "SOC_IDENTITY_MIGRATION";

  protected final static String REMOVE_LIMIT_THRESHOLD_KEY = "REMOVE_LIMIT_THRESHOLD";

  private int REMOVE_LIMIT_THRESHOLD = 20;

  private long totalNumberIdentites = 0;

  private final RDBMSIdentityStorageImpl identityStorage;
  private final IdentityStorageImpl jcrIdentityStorage;

  private String identityQuery;

  private long numberIdentities = 0;
  private Set<String> identitiesMigrateFailed = new HashSet<>();
  private Set<String> identitiesCleanupFailed = new HashSet<>();

  public IdentityMigrationService(InitParams initParams,
                                  RDBMSIdentityStorageImpl identityStorage,
                                  IdentityStorageImpl jcrIdentityStorage,
                                  EventManager<Identity, String> eventManager, EntityManagerService entityManagerService) {
    super(initParams, identityStorage, eventManager, entityManagerService);
    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 200);
    this.REMOVE_LIMIT_THRESHOLD = getInteger(initParams, REMOVE_LIMIT_THRESHOLD_KEY, 20);
    this.identityStorage = identityStorage;
    this.jcrIdentityStorage = jcrIdentityStorage;
  }

  @Override
  protected void beforeMigration() throws Exception {
    MigrationContext.setIdentityDone(false);
    identitiesMigrateFailed = new HashSet<>();
    numberIdentities = 0;
  }

  @Override
  @Managed
  @ManagedDescription("Manual to start run migration data of identities from JCR to RDBMS.")
  public void doMigration() throws Exception {
    long t = System.currentTimeMillis();

    long totalIdentities = getTotalNumberIdentities();

    //endTx(begunTx);

    LOG.info("|\\ START::Identity migration ---------------------------------");

    RequestLifeCycle.end();

    long offset = 0;
    boolean cont = true;
    boolean begunTx = false;
    List<String> transactionList = new ArrayList<>();

    long numberSuccessful = 0;
    long batchSize = 0;

    while(cont && !forkStop) {
      try {

        try {

          RequestLifeCycle.begin(PortalContainer.getInstance());
          begunTx = startTx();
          transactionList = new ArrayList<>();
          NodeIterator nodeIter = getIdentityNodes(offset, LIMIT_THRESHOLD);
          batchSize = nodeIter.getSize();

          if (nodeIter == null || batchSize == 0) {
            cont = false;

          } else {

            while (nodeIter.hasNext() && !forkStop) {
              offset++;
              Node identityNode = nodeIter.nextNode();
              String identityName = identityNode.getName();
              String jcrid = identityNode.getUUID();
              transactionList.add(identityName);

              LOG.info("|  \\ START::identity number: {}/{} node_name={} uuid={}", offset, totalIdentities, identityName, jcrid);
              long t1 = System.currentTimeMillis();

              try {
                Identity identity = migrateIdentity(identityNode, jcrid);

                if (identity != null) {
                  String newId = identity.getId();
                  identity.setId(jcrid);
                  broadcastListener(identity, newId);
                  LOG.info("|      Identity migrated jcr_uid={} id={}", jcrid, newId);
                }
                numberSuccessful++;
              } catch (Exception ex) {
                identitiesMigrateFailed.add(identityName);
                LOG.error(String.format("Error during migration of identity node_name=%s uuid=%s", identityName, jcrid), ex);
              }
              LOG.info("|  / END::identity number {} node_name={} duration_ms={}", offset, identityNode.getName(), System.currentTimeMillis() - t1);
            }
          }

        } finally {
          try {
            endTx(begunTx);
          } catch (Exception ex) {
            // If commit was failed, all identities are failed also
            identitiesMigrateFailed.addAll(transactionList);
            numberSuccessful -= batchSize;
          }
          RequestLifeCycle.end();
        }
      } catch (Throwable ex) {
        LOG.error(ex);
      }
    }

    numberIdentities = offset;
    if (identitiesMigrateFailed.size() > 0) {
      LOG.info("| / END::Identity migration failed for ({}) identity(s)", identitiesMigrateFailed.size());
    }
    LOG.info("|// END::Identity migration for ({}) identity(s) duration_ms={}", numberSuccessful, System.currentTimeMillis() - t);

    LOG.info("| \\ START::Re-indexing identity(s) ---------------------------------");
    IndexingService indexingService = CommonsUtils.getService(IndexingService.class);
    indexingService.reindexAll(ProfileIndexingServiceConnector.TYPE);
    LOG.info("| / END::Re-indexing identity(s) ---------------------------------");

    RequestLifeCycle.begin(PortalContainer.getInstance());
  }

  @Override
  protected void afterMigration() throws Exception {
    MigrationContext.setIdentitiesMigrateFailed(identitiesMigrateFailed);
    if (!forkStop && identitiesMigrateFailed.isEmpty()) {
      MigrationContext.setIdentityDone(true);
    }
  }

  @Override
  public void doRemove() throws Exception {
    identitiesCleanupFailed = new HashSet<>();

    long totalIdentities = getTotalNumberIdentities();

    LOG.info("| \\ START::cleanup Identities ---------------------------------");
    long t = System.currentTimeMillis();
    long timePerIdentity = System.currentTimeMillis();
    int offset = 0;
    long failed = 0;
    List<String> transactionList = new ArrayList<>();

    try {
      boolean cont = true;
      while (cont) {
        try {

          RequestLifeCycle.begin(PortalContainer.getInstance());
          failed = identitiesCleanupFailed.size();
          transactionList = new ArrayList<>();

          NodeIterator nodeIter  = getIdentityNodes(failed, REMOVE_LIMIT_THRESHOLD);
          if(nodeIter == null || nodeIter.getSize() == 0) {
            cont = false;

          } else {

            while (nodeIter.hasNext()) {
              offset++;
              Node node = nodeIter.nextNode();
              timePerIdentity = System.currentTimeMillis();
              LOG.info("|  \\ START::cleanup Identity number: {}/{} name={}", offset, totalIdentities, node.getName());

              String name = node.getName();
              if (!MigrationContext.isForceCleanup() && (MigrationContext.getIdentitiesCleanupConnectionFailed().contains(name)
                      || MigrationContext.getIdentitiesCleanupActivityFailed().contains(name))) {
                identitiesCleanupFailed.add(name);
                LOG.warn("Will not remove this identity because the cleanup connection or activities were failed for it");
                continue;
              }

              //transactionList.add(name);

              try {
                PropertyIterator pit = node.getReferences();
                if (pit != null && pit.getSize() > 0) {
                  int num = 0;
                  while (pit.hasNext()) {
                    num++;
                    pit.nextProperty().remove();
                    if (num % REMOVE_LIMIT_THRESHOLD == 0) {
                      getSession().save();
                    }
                  }
                  getSession().save();
                }
                node.remove();
                getSession().save();
              } catch (Exception ex) {
                LOG.error("Error when cleanup the identity: " + name, ex);
                identitiesCleanupFailed.add(name);
                // Discard all change if there is any error
                getSession().getJCRSession().refresh(false);
              }

              LOG.info("|  / END::cleanup Identity ({}) identities duration_ms={}", node.getName(), System.currentTimeMillis() - timePerIdentity);
            }
          }

        } finally {
          RequestLifeCycle.end();
        }
      }

    } finally {
      MigrationContext.setIdentitiesCleanupFailed(identitiesCleanupFailed);
      if (identitiesCleanupFailed.size() > 0) {
        LOG.warn("Cleanup failed for {} identities", identitiesCleanupFailed.size());
      }
      LOG.info("| / END::cleanup ({}) identities duration_ms={}", offset, System.currentTimeMillis() - t);
    }
  }

  private Identity migrateIdentity(Node node, String jcrId) throws Exception {
    String providerId = node.getProperty("soc:providerId").getString();
    // The node name is the identity id.
    // Node name is soc:<name>, only the <name> is relevant
    String name = IdentityUtil.getIdentityName(node.getName());

    Identity identity = identityStorage.findIdentity(providerId, name);
    if (identity != null) {
      LOG.info("Identity with provider_id={} and remote_id={} has already been migrated. Identity id={} will be used.", identity.getProviderId(), identity.getRemoteId(), identity.getId());
      return identity;
    }

    identity = new Identity(providerId, name);
    identity.setDeleted(node.getProperty("soc:isDeleted").getBoolean());

    if (node.isNodeType("soc:isDisabled")) {
      identity.setEnable(false);
    }

    identityStorage.saveIdentity(identity);

    //
    String id = identity.getId();
    identity.setId(jcrId);

    // Migrate profile
    //TODO: please check the way to load profile data from JCR
    Profile profile = new Profile(identity);
    jcrIdentityStorage.loadProfile(profile);
    String oldProfileId = profile.getId();
    profile.setId("0");
    identity.setId(id);

    // Process profile
    ProfileEntity entity = _findById(ProfileEntity.class, oldProfileId);
    NTFile avatar = entity.getAvatar();
    if (avatar != null) {
      Resource resource = avatar.getContentResource();
      AvatarAttachment attachment = new AvatarAttachment();
      attachment.setMimeType(resource.getMimeType());
      attachment.setInputStream(new ByteArrayInputStream(resource.getData()));

      profile.setProperty(Profile.AVATAR, attachment);
    }


    identityStorage.saveProfile(profile);

    identity.setProfile(profile);

    return identity;
  }

  public Identity migrateIdentity(String oldId) {
    boolean begun = false;
    try {
      RequestLifeCycle.begin(PortalContainer.getInstance());
      begun = startTx();
      IdentityEntity jcrEntity = _findById(IdentityEntity.class, oldId);

      String providerId = jcrEntity.getProviderId();
      String remoteId = jcrEntity.getRemoteId();

      Identity identity = identityStorage.findIdentity(providerId, remoteId);

      if (identity == null) {
        identity = new Identity(providerId, remoteId);
        identity.setDeleted(jcrEntity.isDeleted());
        identity.setEnable(_getMixin(jcrEntity, DisabledEntity.class, false) == null);

        identityStorage.saveIdentity(identity);

        //
        String id = identity.getId();
        identity.setId(oldId);

        // Migrate profile
        Profile profile = new Profile(identity);
        jcrIdentityStorage.loadProfile(profile);
        String oldProfileId = profile.getId();
        profile.setId("0");
        identity.setId(id);

        // Process profile
        ProfileEntity entity = _findById(ProfileEntity.class, oldProfileId);
        NTFile avatar = entity.getAvatar();
        if (avatar != null) {
          Resource resource = avatar.getContentResource();
          AvatarAttachment attachment = new AvatarAttachment();
          attachment.setMimeType(resource.getMimeType());
          attachment.setInputStream(new ByteArrayInputStream(resource.getData()));

          profile.setProperty(Profile.AVATAR, attachment);
        }


        identityStorage.saveProfile(profile);

        identity.setProfile(profile);

      }

      if (identity != null) {
        String newId = identity.getId();
        identity.setId(oldId);
        broadcastListener(identity, newId);
      }

      return identity;
    } catch (NodeNotFoundException ex) {
      LOG.error("Can not find indentity with oldId: " + oldId, ex);
      return null;
    } catch (Exception ex) {
      LOG.error("Exception while migrate identity with oldId: " + oldId, ex);
      return null;
    } finally {
      try {
        endTx(begun);
      } catch (Exception ex) {
        LOG.error("Error while commit transaction", ex);
      }
      RequestLifeCycle.end();
    }
  }

  private NodeIterator getIdentityNodes(long offset, int limit) {
    if(identityQuery == null) {
      identityQuery = new StringBuilder().append("SELECT * FROM soc:identitydefinition").toString();
    }
    return nodes(identityQuery, offset, limit);
  }

  private long getTotalNumberIdentities() {
    if (this.totalNumberIdentites == 0) {
      if(identityQuery == null) {
        identityQuery = new StringBuilder().append("SELECT * FROM soc:identitydefinition").toString();
      }
      this.totalNumberIdentites = nodes(identityQuery).getSize();
    }
    return this.totalNumberIdentites;
  }


  @Override
  @Managed
  @ManagedDescription("Manual to stop run migration data of identities from JCR to RDBMS.")
  public void stop() {
    super.stop();
  }

  @Override
  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
}
