package org.exoplatform.social.addons.updater;

import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Value;

import org.chromattic.ext.ntdef.NTFile;

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
import org.exoplatform.social.addons.search.SpaceIndexingServiceConnector;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.entity.ProviderEntity;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.storage.exception.NodeNotFoundException;
import org.exoplatform.social.core.storage.impl.IdentityStorageImpl;

@Managed
@ManagedDescription("Social migration Spaces from JCR to RDBMS.")
@NameTemplate({@Property(key = "service", value = "social"), @Property(key = "view", value = "migration-spaces") })
public class SpaceMigrationService extends AbstractMigrationService<Space> {
  public static final String EVENT_LISTENER_KEY = "SOC_SPACES_MIGRATION";
  private String spaceQuery;
  private SpaceStorage spaceStorage;

  public SpaceMigrationService(InitParams initParams, SpaceStorage spaceStorage,
                                      IdentityStorageImpl identityStorage,
                                      EventManager<Space, String> eventManager,
                                      EntityManagerService entityManagerService) {

    super(initParams, identityStorage, eventManager, entityManagerService);
    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 200);
    this.spaceStorage = spaceStorage;
  }

  @Override
  protected void beforeMigration() throws Exception {
    MigrationContext.setSpaceDone(false);
    numberFailed = 0;
  }

  @Override
  @Managed
  @ManagedDescription("Manual to start run migration data of spaces from JCR to RDBMS.")
  public void doMigration() throws Exception {
    RequestLifeCycle.end();

    LOG.info("| \\ START::Spaces migration ---------------------------------");

    long t = System.currentTimeMillis();
    long offset = 0;
    long numberSuccessful = 0;
    boolean cont = true;

    while (cont && !forkStop) {
      long batchSize = 0;
      RequestLifeCycle.begin(PortalContainer.getInstance());
      boolean begunTx = startTx();
      try {
        NodeIterator nodeIter  = getSpaceNodes(offset, LIMIT_THRESHOLD);
        batchSize = nodeIter == null ? 0 : nodeIter.getSize();
        if(batchSize == 0) {
          cont = false;

        } else {
          Node spaceNode = null;
          while (nodeIter.hasNext()) {
            if(forkStop) {
              break;
            }

            offset++;
            spaceNode = nodeIter.nextNode();
            LOG.info(String.format("|  \\ START::space number: %s (%s space)", offset, spaceNode.getName()));
            long t1 = System.currentTimeMillis();

            Space space = migrateSpace(spaceNode);
            broadcastListener(space, space.getId());
            numberSuccessful++;
            LOG.info(String.format("|  / END::space number %s (%s space) consumed %s(ms)", offset, spaceNode.getName(), System.currentTimeMillis() - t1));
          }
        }

      } catch (Exception ex) {
        LOG.error("Error while migrate the space", ex);
        numberFailed++;

      } finally {
        try {
          endTx(begunTx);
        } catch (Exception ex) {
          // Commit transaction failed
          numberFailed += batchSize;
        }
        RequestLifeCycle.end();
      }
    }

    RequestLifeCycle.begin(PortalContainer.getInstance());

    if (numberFailed > 0) {
      LOG.info(String.format("|   Space migration failed for (%s) space(s)", numberFailed));
    }
    LOG.info(String.format("| / END::Space migration for (%s) space(s) consumed %s(ms)", numberSuccessful, System.currentTimeMillis() - t));

    LOG.info("| \\ START::Re-indexing space(s) ---------------------------------");
    //To be sure all of the space will be indexed in ES after migrated
    IndexingService indexingService = CommonsUtils.getService(IndexingService.class);
    indexingService.reindexAll(SpaceIndexingServiceConnector.TYPE);
    LOG.info("| / END::Re-indexing space(s) ---------------------------------");
  }
  
  private Space migrateSpace(Node spaceNode) throws Exception {
    Space space = new Space();
    space.setApp(this.getProperty(spaceNode, "soc:app"));

    IdentityEntity identity = findIdentityEntity(SpaceIdentityProvider.NAME, this.getProperty(spaceNode, "soc:name"));
    if (identity != null) {
      NTFile avatar = identity.getProfile().getAvatar();
      if (avatar != null) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(avatar.getContentResource().getData());
        AvatarAttachment attach = new AvatarAttachment(avatar.getName(), avatar.getName(), 
                                                       avatar.getContentResource().getMimeType(), 
                                                       inputStream, null, avatar.getLastModified().getTime());
        space.setAvatarAttachment(attach);
        space.setAvatarLastUpdated(avatar.getLastModified().getTime());
        space.setAvatarUrl(getSession().getPath(avatar));
      }
    }
    
    space.setCreatedTime(spaceNode.getProperty("soc:createdTime").getLong());
    space.setDescription(this.getProperty(spaceNode, "soc:description"));
    space.setDisplayName(this.getProperty(spaceNode, "soc:displayName"));
    space.setGroupId(this.getProperty(spaceNode, "soc:groupId"));
    space.setInvitedUsers(this.getProperties(spaceNode, "soc:invitedMembersId"));
    space.setManagers(this.getProperties(spaceNode, "soc:managerMembersId"));
    space.setMembers(this.getProperties(spaceNode, "soc:membersId"));
    space.setPendingUsers(this.getProperties(spaceNode, "soc:pendingMembersId"));
    space.setPrettyName(this.getProperty(spaceNode, "soc:name"));
    space.setPriority(this.getProperty(spaceNode, "soc:priority"));
    space.setRegistration(this.getProperty(spaceNode, "soc:registration"));
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setUrl(this.getProperty(spaceNode, "soc:url"));
    space.setVisibility(this.getProperty(spaceNode, "soc:visibility"));    
    
    spaceStorage.saveSpace(space, true);
    return space;
  }

  @Override
  protected void afterMigration() throws Exception {
    if(forkStop || numberFailed > 0) {
      return;
    }
    MigrationContext.setSpaceDone(true);
  }

  public void doRemove() throws Exception {
    LOG.info("| \\ START::cleanup Spaces ---------------------------------");
    long t = System.currentTimeMillis();
    long timePerSpace = System.currentTimeMillis();
    RequestLifeCycle.begin(PortalContainer.getInstance());
    int offset = 0;
    try {
      NodeIterator nodeIter  = getSpaceNodes(offset, LIMIT_THRESHOLD);
      if(nodeIter == null || nodeIter.getSize() == 0) {
        return;
      }

      while (nodeIter.hasNext()) {
        Node node = nodeIter.nextNode();
        LOG.info(String.format("|  \\ START::cleanup Space number: %s (%s space)", offset, node.getName()));
        offset++;

        node.remove();

        LOG.info(String.format("|  / END::cleanup (%s space) consumed time %s(ms)", node.getName(), System.currentTimeMillis() - timePerSpace));
        
        timePerSpace = System.currentTimeMillis();
        if(offset % LIMIT_THRESHOLD == 0) {
          getSession().save();
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          nodeIter = getSpaceNodes(offset, LIMIT_THRESHOLD);
        }
      }
      LOG.info(String.format("| / END::cleanup Spaces migration for (%s) space consumed %s(ms)", offset, System.currentTimeMillis() - t));
    } finally {
      getSession().save();
      RequestLifeCycle.end();
    }
  }

  @Override
  @Managed
  @ManagedDescription("Manual to stop run miguration data of spaces from JCR to RDBMS.")
  public void stop() {
    super.stop();
  }

  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
  
  private IdentityEntity findIdentityEntity(final String providerId, final String remoteId) throws NodeNotFoundException {
    ProviderEntity providerEntity;
    try {
      providerEntity = getProviderRoot().getProviders().get(providerId);
    } catch (Exception ex) {
      lifeCycle.getProviderRoot().set(null);
      providerEntity = getProviderRoot().getProviders().get(providerId);
    }

    if (providerEntity == null) {
      throw new NodeNotFoundException("The node " + providerId + " doesn't exist");
    }

    IdentityEntity identityEntity = providerEntity.getIdentities().get(remoteId);

    if (identityEntity == null) {
      throw new NodeNotFoundException("The node " + providerId + "/" + remoteId + " doesn't exist");
    }

    return identityEntity;

  }
  
  private NodeIterator getSpaceNodes(long offset, int lIMIT_THRESHOLD) {
    if(spaceQuery == null) {
      spaceQuery = new StringBuffer().append("SELECT * FROM soc:spacedefinition").toString();
    }
    return nodes(spaceQuery, offset, LIMIT_THRESHOLD);
  }
  
  private String getProperty(Node spaceNode, String propName) throws Exception {
    try {
      return spaceNode.getProperty(propName).getString();
    } catch (Exception ex) {
      return null;
    }
  }
  
  private String[] getProperties(Node spaceNode, String propName) throws Exception {
    List<String> values = new LinkedList<>();
    try {
      for (Value val : spaceNode.getProperty(propName).getValues()) {
        values.add(val.getString());
      }
      return values.toArray(new String[values.size()]);
    } catch (Exception ex) {
      return null;
    }
  }
}
