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
import org.exoplatform.social.addons.search.ProfileIndexingServiceConnector;
import org.exoplatform.social.addons.storage.RDBMSIdentityStorageImpl;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.entity.ProfileEntity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.social.core.storage.impl.IdentityStorageImpl;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import java.io.ByteArrayInputStream;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
@Managed
@ManagedDescription("Social migration Identities from JCR to RDBMS.")
@NameTemplate({@Property(key = "service", value = "social"), @Property(key = "view", value = "migration-identities") })
public class IdentityMigrationService extends AbstractMigrationService<Identity> {

  public static final String EVENT_LISTENER_KEY = "SOC_IDENTITY_MIGRATION";

  private final RDBMSIdentityStorageImpl identityStorage;
  private final IdentityStorageImpl jcrIdentityStorage;

  private String identityQuery;

  private long numberFailed = 0;

  public IdentityMigrationService(InitParams initParams,
                                  RDBMSIdentityStorageImpl identityStorage,
                                  IdentityStorageImpl jcrIdentityStorage,
                                  EventManager<Identity, String> eventManager, EntityManagerService entityManagerService) {
    super(initParams, identityStorage, eventManager, entityManagerService);
    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 200);
    this.identityStorage = identityStorage;
    this.jcrIdentityStorage = jcrIdentityStorage;
  }

  @Override
  protected void beforeMigration() throws Exception {
    MigrationContext.setIdentityDone(false);
    numberFailed = 0;
  }

  @Override
  @Managed
  @ManagedDescription("Manual to start run migration data of identities from JCR to RDBMS.")
  public void doMigration() throws Exception {
    long t = System.currentTimeMillis();

    //endTx(begunTx);

    LOG.info("|\\ START::Identity migration ---------------------------------");

    RequestLifeCycle.end();

    long offset = 0;
    boolean cont = true;
    boolean begunTx = false;

    long numberSuccessful = 0;
    long batchSize = 0;

    while(cont && !forkStop) {
      try {

        try {

          RequestLifeCycle.begin(PortalContainer.getInstance());
          begunTx = startTx();
          NodeIterator nodeIter = getIdentityNodes(offset, LIMIT_THRESHOLD);
          batchSize = nodeIter.getSize();

          if (nodeIter == null || batchSize == 0) {
            cont = false;

          } else {

            while (nodeIter.hasNext() && !forkStop) {
              offset++;
              Node identityNode = nodeIter.nextNode();

              LOG.info(String.format("|  \\ START::identity number: %s (%s identity)", offset, identityNode.getName()));
              long t1 = System.currentTimeMillis();

              String jcrid = identityNode.getUUID();
              Identity identity = migrateIdentity(identityNode, jcrid);

              if (identity != null) {
                String newId = identity.getId();
                identity.setId(jcrid);
                broadcastListener(identity, newId);
              }
              numberSuccessful++;
              LOG.info(String.format("|  / END::identity number %s (%s identity) consumed %s(ms)", offset, identityNode.getName(), System.currentTimeMillis() - t1));
            }
          }

        } finally {
          try {
            endTx(begunTx);
          } catch (Exception ex) {
            // If commit was failed, all identities are failed also
            numberFailed += batchSize;
            numberSuccessful -= batchSize;
          }
          RequestLifeCycle.end();
        }
      } catch (Throwable ex) {
        numberFailed ++;
        LOG.error(ex);
      }
    }

    if (numberFailed > 0) {
      LOG.info(String.format("| / END::Identity migration failed for (%s) identity(s)", numberFailed));
    }
    LOG.info(String.format("|// END::Identity migration for (%s) identity(s) consumed %s(ms)", numberSuccessful, System.currentTimeMillis() - t));

    LOG.info("| \\ START::Re-indexing identity(s) ---------------------------------");
    IndexingService indexingService = CommonsUtils.getService(IndexingService.class);
    indexingService.reindexAll(ProfileIndexingServiceConnector.TYPE);
    LOG.info("| / END::Re-indexing identity(s) ---------------------------------");

    RequestLifeCycle.begin(PortalContainer.getInstance());
  }

  @Override
  protected void afterMigration() throws Exception {
    if (forkStop || numberFailed > 0) {
      return;
    }
    MigrationContext.setIdentityDone(true);
  }

  @Override
  public void doRemove() throws Exception {
    LOG.info("| \\ START::cleanup Identities ---------------------------------");
    long t = System.currentTimeMillis();
    long timePerIdentity = System.currentTimeMillis();
    RequestLifeCycle.begin(PortalContainer.getInstance());
    int offset = 0;
    try {
      NodeIterator nodeIter  = getIdentityNodes(offset, LIMIT_THRESHOLD);
      if(nodeIter == null || nodeIter.getSize() == 0) {
        return;
      }

      while (nodeIter.hasNext()) {
        Node node = nodeIter.nextNode();
        LOG.info(String.format("|  \\ START::cleanup Identity number: %s (%s identity)", offset, node.getName()));
        offset++;

        node.remove();

        LOG.info(String.format("|  / END::cleanup (%s identity) consumed time %s(ms)", node.getName(), System.currentTimeMillis() - timePerIdentity));

        timePerIdentity = System.currentTimeMillis();
        if(offset % LIMIT_THRESHOLD == 0) {
          getSession().save();
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          nodeIter = getIdentityNodes(offset, LIMIT_THRESHOLD);
        }
      }
    } finally {
      getSession().save();
      LOG.info(String.format("| / END::cleanup Identities migration for (%s) identity consumed %s(ms)", offset, System.currentTimeMillis() - t));
      RequestLifeCycle.end();
    }
  }

  private Identity migrateIdentity(Node node, String jcrId) throws Exception {
    String providerId = node.getProperty("soc:providerId").getString();
    String remoteId = node.getProperty("soc:remoteId").getString();

    Identity identity = identityStorage.findIdentity(providerId, remoteId);
    if (identity != null) {
      LOG.info("Identity with providerId = " + identity.getProviderId() + " and remoteId=" + identity.getRemoteId() + " has already been migrated.");
      return null;
    }

    identity = new Identity(providerId, remoteId);
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

  private NodeIterator getIdentityNodes(long offset, int limit) {
    if(identityQuery == null) {
      identityQuery = new StringBuilder().append("SELECT * FROM soc:identitydefinition").toString();
    }
    return nodes(identityQuery, offset, limit);
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
