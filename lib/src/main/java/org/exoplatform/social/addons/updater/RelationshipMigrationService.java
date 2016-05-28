package org.exoplatform.social.addons.updater;

import java.util.Collection;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

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
import org.exoplatform.social.addons.storage.dao.ConnectionDAO;
import org.exoplatform.social.addons.storage.dao.IdentityDAO;
import org.exoplatform.social.addons.storage.entity.Connection;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.entity.RelationshipEntity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.storage.impl.IdentityStorageImpl;

@Managed
@ManagedDescription("Social migration relationships from JCR to RDBMS.")
@NameTemplate({@Property(key = "service", value = "social"), @Property(key = "view", value = "migration-relationships") })
public class RelationshipMigrationService extends AbstractMigrationService<Relationship> {
  public static final String EVENT_LISTENER_KEY = "SOC_RELATIONSHIP_MIGRATION";
  private final ConnectionDAO connectionDAO;
  private final IdentityDAO identityDAO;
  private static final int LIMIT_REMOVED_THRESHOLD = 10;

  public RelationshipMigrationService(InitParams initParams,
                                      IdentityStorageImpl identityStorage,
                                      ConnectionDAO connectionDAO,
                                      IdentityDAO identityDAO,
                                      EventManager<Relationship, String> eventManager,
                                      EntityManagerService entityManagerService) {

    super(initParams, identityStorage, eventManager, entityManagerService);
    this.connectionDAO = connectionDAO;
    this.identityDAO = identityDAO;
    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 200);
  }

  @Override
  protected void beforeMigration() throws Exception {
    MigrationContext.setConnectionDone(false);
    numberFailed = 0;
  }

  @Override
  @Managed
  @ManagedDescription("Manual to start run migration data of relationships from JCR to RDBMS.")
  public void doMigration() throws Exception {
    RequestLifeCycle.end();

    boolean cont = true;
    long offset = 0;
    int total = 0;
    long t = System.currentTimeMillis();

    while(cont && !forkStop) {
      RequestLifeCycle.begin(PortalContainer.getInstance());
      boolean begunTx = startTx();

      try {
        LOG.info("| \\ START::Relationships migration ---------------------------------");
        NodeIterator nodeIter = getIdentityNodes(offset, LIMIT_THRESHOLD);
        if (nodeIter == null || nodeIter.getSize() == 0) {
          cont = false;
        } else {

          int relationshipNo;
          Node identityNode;
          while (nodeIter.hasNext()) {
            if (forkStop) {
              break;
            }
            relationshipNo = 0;
            offset++;
            identityNode = nodeIter.nextNode();
            LOG.info(String.format("|  \\ START::user number: %s (%s user)", offset, identityNode.getName()));
            long t1 = System.currentTimeMillis();

            Node relationshipNode = identityNode.getNode("soc:relationship");
            if (relationshipNode != null) {
              NodeIterator rIt = relationshipNode.getNodes();
              long size = rIt.getSize();
              LOG.info("|     - CONFIRMED:: size = " + size);
              if (size > 0) {
                relationshipNo += migrateRelationshipEntity(rIt, identityNode.getName(), false, Relationship.Type.CONFIRMED);
              }
            }

            relationshipNode = identityNode.getNode("soc:sender");
            if (relationshipNode != null) {
              NodeIterator rIt = relationshipNode.getNodes();
              long size = rIt.getSize();
              LOG.info("|     - SENDER:: size = " + size);
              if (size > 0) {
                relationshipNo += migrateRelationshipEntity(rIt, identityNode.getName(), false, Relationship.Type.OUTGOING);
              }
            }

            relationshipNode = identityNode.getNode("soc:receiver");
            if (relationshipNode != null) {
              NodeIterator rIt = relationshipNode.getNodes();
              long size = rIt.getSize();
              LOG.info("|     - RECEIVER:: size = " + size);
              if (size > 0) {
                relationshipNo += migrateRelationshipEntity(rIt, identityNode.getName(), true, Relationship.Type.INCOMING);
              }
            }
            //
            total += relationshipNo;
            if (offset % LIMIT_THRESHOLD == 0) {
              try {
                endTx(begunTx);
              } catch (Exception ex) {
                numberFailed += LIMIT_THRESHOLD;
              }
              RequestLifeCycle.end();
              RequestLifeCycle.begin(PortalContainer.getInstance());
              begunTx = startTx();
              nodeIter = getIdentityNodes(offset, LIMIT_THRESHOLD);
            }

            LOG.info(String.format("|  / END::user number %s (%s user) with %s relationship(s) user consumed %s(ms)", relationshipNo, identityNode.getName(), relationshipNo, System.currentTimeMillis() - t1));
          }
        }

      } finally {
        try {
          endTx(begunTx);
        } catch (Exception ex) {
          numberFailed += LIMIT_THRESHOLD;
        }

        RequestLifeCycle.end();
      }
    }

    LOG.info(String.format("| / END::Relationships migration for (%s) user(s) with %s relationship(s) consumed %s(ms)", offset, total, System.currentTimeMillis() - t));
    RequestLifeCycle.begin(PortalContainer.getInstance());

    LOG.info("| \\ START::Re-indexing identity(s) ---------------------------------");
    IndexingService indexingService = CommonsUtils.getService(IndexingService.class);
    indexingService.reindexAll(ProfileIndexingServiceConnector.TYPE);
    LOG.info("| / END::Re-indexing identity(s) ---------------------------------");
  }
  
  private int migrateRelationshipEntity(NodeIterator it, String userName, boolean isIncoming, Relationship.Type status) throws RepositoryException {
    int doneConnectionNo = 0;
    startTx();
    while (it.hasNext()) {
      Node relationshipNode = it.nextNode();
      String receiverId = relationshipNode.getProperty("soc:to").getString();
      LOG.debug("|     - FROM ID = " + receiverId);
      String senderId = relationshipNode.getProperty("soc:from").getString();
      LOG.debug("|     - TO ID = " + senderId);
      long lastUpdated = System.currentTimeMillis();
      if (relationshipNode.hasProperty("exo:lastModifiedDate")) {
        lastUpdated = relationshipNode.getProperty("exo:lastModifiedDate").getDate().getTimeInMillis();
      }
      LOG.debug("|     - LAST UPDATED = " + lastUpdated);
      //handle the duplicate connection key by catch exception
      try {
        //check the sender
        //Identity sender = new Identity(isIncoming ? senderId : receiverId);
        //Identity receiver = new Identity(isIncoming ? receiverId : senderId);

        IdentityEntity senderIdentity = _findById(IdentityEntity.class, senderId);
        IdentityEntity receiverIdentity = _findById(IdentityEntity.class, receiverId);

        org.exoplatform.social.addons.storage.entity.IdentityEntity sender = identityDAO.findByProviderAndRemoteId(OrganizationIdentityProvider.NAME, senderIdentity.getRemoteId());
        org.exoplatform.social.addons.storage.entity.IdentityEntity receiver = identityDAO.findByProviderAndRemoteId(OrganizationIdentityProvider.NAME, receiverIdentity.getRemoteId());

        Connection exist = connectionDAO.getConnection(sender.getId(), receiver.getId());
        if (exist == null) {
          exist = connectionDAO.getConnection(receiver.getId(), sender.getId());
        }
        if (exist == null) {
          Connection entity = new Connection(sender, receiver);
          entity.setStatus(status);
          entity.setLastUpdated(lastUpdated);
          //
          connectionDAO.create(entity);
          ++doneConnectionNo;
        }       
      } catch(Exception e) {
        LOG.warn(e.getMessage());
        continue;
      }
      
      if(doneConnectionNo % LIMIT_THRESHOLD == 0) {
        LOG.info(String.format("|     - BATCH MIGRATION::relationship number: %s (%s user)", doneConnectionNo,  userName));
        endTx(true);
        entityManagerService.endRequest(PortalContainer.getInstance());
        entityManagerService.startRequest(PortalContainer.getInstance());
        startTx();
      }
    }
    return doneConnectionNo;
  }

  @Override
  protected void afterMigration() throws Exception {
    if(forkStop || numberFailed > 0) {
      return;
    }
    MigrationContext.setConnectionDone(true);
  }

  public void doRemove() throws Exception {
    LOG.info("| \\ START::cleanup Relationships ---------------------------------");
    long t = System.currentTimeMillis();
    long timePerUser = System.currentTimeMillis();
    RequestLifeCycle.begin(PortalContainer.getInstance());
    int offset = 0;
    try {
      NodeIterator nodeIter  = getIdentityNodes(offset, LIMIT_THRESHOLD);
      if(nodeIter == null || nodeIter.getSize() == 0) {
        return;
      }
      Node node = null;
      
      while (nodeIter.hasNext()) {
        node = nodeIter.nextNode();
        LOG.info(String.format("|  \\ START::cleanup Relationship of user number: %s (%s user)", offset, node.getName()));
        IdentityEntity identityEntity = _findById(IdentityEntity.class, node.getUUID());
        offset++;
        
        Collection<RelationshipEntity> entities = identityEntity.getRelationship().getRelationships().values();
        removeRelationshipEntity(entities);
        // 
        entities = identityEntity.getSender().getRelationships().values();
        removeRelationshipEntity(entities);
        //
        entities = identityEntity.getReceiver().getRelationships().values();
        removeRelationshipEntity(entities);
        
        LOG.info(String.format("|  / END::cleanup (%s user) consumed time %s(ms)", node.getName(), System.currentTimeMillis() - timePerUser));
        
        timePerUser = System.currentTimeMillis();
        if(offset % LIMIT_THRESHOLD == 0) {
          RequestLifeCycle.end();
          RequestLifeCycle.begin(PortalContainer.getInstance());
          nodeIter = getIdentityNodes(offset, LIMIT_THRESHOLD);
        }
      }
      LOG.info(String.format("| / END::cleanup Relationships migration for (%s) user consumed %s(ms)", offset, System.currentTimeMillis() - t));
    } finally {
      RequestLifeCycle.end();
    }
  }
  
  
  private void removeRelationshipEntity(Collection<RelationshipEntity> entities) {
    try {
      int offset = 0;
      Iterator<RelationshipEntity> it = entities.iterator();
      while (it.hasNext()) {
        RelationshipEntity relationshipEntity = it.next();
        getSession().remove(relationshipEntity);
        ++offset;
        if (offset % LIMIT_REMOVED_THRESHOLD == 0) {
          LOG.info(String.format("|     - BATCH CLEANUP::relationship number: %s", offset));
          getSession().save();
        }
      }
    } finally {
      getSession().save();
    }
  }

  

  @Override
  @Managed
  @ManagedDescription("Manual to stop run miguration data of relationships from JCR to RDBMS.")
  public void stop() {
    super.stop();
  }

  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
}
