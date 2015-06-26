package org.exoplatform.social.addons.updater;

import java.util.Collection;
import java.util.Iterator;

import org.exoplatform.commons.api.event.EventManager;
import org.exoplatform.commons.api.jpa.EntityManagerService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.social.addons.storage.dao.ProfileItemDAO;
import org.exoplatform.social.addons.storage.dao.RelationshipDAO;
import org.exoplatform.social.addons.storage.dao.jpa.GenericDAOImpl;
import org.exoplatform.social.addons.storage.entity.Connection;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.entity.RelationshipEntity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.storage.api.IdentityStorage;

@Managed
@ManagedDescription("Social migration relationships from JCR to MYSQl service.")
@NameTemplate({@Property(key = "service", value = "social"), @Property(key = "view", value = "migration-relationships") })
public class RelationshipMigrationService extends AbstractMigrationService<Relationship> {
  public static final String EVENT_LISTENER_KEY = "SOC_RELATIONSHIP_MIGRATION";
  private final RelationshipDAO relationshipDAO;
  private final ProfileItemDAO profileItemDAO;
  private static int number = 0;
  

  public RelationshipMigrationService(InitParams initParams,
                                      IdentityStorage identityStorage,
                                      RelationshipDAO relationshipDAO,
                                      ProfileItemDAO profileItemDAO,
                                      ProfileMigrationService profileMigration,
                                      EventManager<Relationship, String> eventManager,
                                      EntityManagerService entityManagerService) {

    super(initParams, identityStorage, eventManager, entityManagerService);
    this.relationshipDAO = relationshipDAO;
    this.profileItemDAO = profileItemDAO;
    this.LIMIT_THRESHOLD = getInteger(initParams, LIMIT_THRESHOLD_KEY, 200);
  }

  @Override
  protected void beforeMigration() throws Exception {
    isDone = false;
  }

  @Override
  @Managed
  @ManagedDescription("Manual to start run miguration data of relationships from JCR to MYSQL.")
  public void doMigration() throws Exception {
      boolean begunTx = GenericDAOImpl.startTx();
      if (relationshipDAO.count() > 0) {
        isDone = true;
        return;
      }
      number = 0;
      LOG.info("Stating to migration relationships from JCR to MYSQL........");
      Collection<IdentityEntity> allIdentityEntity  = getAllIdentityEntity(OrganizationIdentityProvider.NAME).values();
      long t = System.currentTimeMillis();
      int count = 0, size = allIdentityEntity.size();
      Iterator<IdentityEntity> iter = allIdentityEntity.iterator();
      while (iter.hasNext()) {
        if(forkStop) {
          return;
        }
        IdentityEntity identityEntity = (IdentityEntity) iter.next();
        //
        LOG.info("Migration relationship for user: " + identityEntity.getRemoteId());
        long t1 = System.currentTimeMillis();
        int c2 = 0;
        Identity identityFrom = new Identity(OrganizationIdentityProvider.NAME, identityEntity.getRemoteId());
        identityFrom.setId(identityEntity.getId());
        //
        Iterator<RelationshipEntity> it = identityEntity.getRelationship().getRelationships().values().iterator();
        c2 += migrateRelationshipEntity(begunTx, it, identityFrom, false, Relationship.Type.CONFIRMED);
        //
        it = identityEntity.getSender().getRelationships().values().iterator();
        c2 += migrateRelationshipEntity(begunTx, it, identityFrom, false, Relationship.Type.OUTGOING);
        //
        it = identityEntity.getReceiver().getRelationships().values().iterator();
        c2 += migrateRelationshipEntity(begunTx, it, identityFrom, true, Relationship.Type.INCOMING);
        //
        ++count;
        processLog("Relationships migration", size, count);
        LOG.info(String.format("Done to migration %s relationships for user %s from JCR to MYSQL on %s(ms)", c2, identityEntity.getRemoteId(), (System.currentTimeMillis() - t1)));
      }
      
      GenericDAOImpl.endTx(begunTx);
      LOG.info(String.format("Done to migration relationships of %s users from JCR to MYSQL on %s(ms)", count, (System.currentTimeMillis() - t)));
  }
  
  private int migrateRelationshipEntity(boolean begunTx, Iterator<RelationshipEntity> it, Identity owner, boolean isIncoming, Relationship.Type status) {
    int c2 = 0;
    while (it.hasNext()) {
      RelationshipEntity relationshipEntity = it.next();
      String receiverId = relationshipEntity.getTo().getId().equals(owner.getId()) ? relationshipEntity.getFrom().getId() : relationshipEntity.getTo().getId();
      Identity receiver = identityStorage.findIdentityById(receiverId);
      //
      Connection entity = new Connection();
      entity.setSenderId(isIncoming ? receiver.getId() : owner.getId());
      entity.setReceiverId(isIncoming ? owner.getId() : receiver.getId());
      entity.setStatus(status);
      entity.setReceiver(profileItemDAO.findProfileItemByIdentityId(isIncoming ? owner.getId() : receiver.getId()));
      //
      relationshipDAO.create(entity);
      ++c2;
      ++number;
      if(number % LIMIT_THRESHOLD == 0) {
        GenericDAOImpl.endTx(begunTx);
        entityManagerService.endRequest(PortalContainer.getInstance());
        entityManagerService.startRequest(PortalContainer.getInstance());
        begunTx = GenericDAOImpl.startTx();
      }
    }
    return c2;
  }

  @Override
  protected void afterMigration() throws Exception {
    if(forkStop) {
      return;
    }
    isDone = true;
    LOG.info("Done to migration relationships from JCR to MYSQL");
  }
  
  private void removeRelationshipEntity(Collection<RelationshipEntity> entities) {
    Iterator<RelationshipEntity> it = entities.iterator();
    while (it.hasNext()) {
      RelationshipEntity relationshipEntity = it.next();
      getSession().remove(relationshipEntity);
      ++number;
      if (number % LIMIT_THRESHOLD == 0) {
        LOG.info("Session save ....");
        sessionSave();
      }
    }
    //
    sessionSave();
  }

  private void sessionSave() {
    try {
      number = 0;
      getSession().save();
    } catch (Exception e) {
      LOG.warn("Session save error: " + e.getMessage());
    }
  }

  public void doRemove() throws Exception {
    LOG.info("Start to remove relationships from JCR");
    number = 0;
    LOG.info("Remove main relationships ...");
    long t = System.currentTimeMillis(), t1 = t, t2;
    RequestLifeCycle.begin(PortalContainer.getInstance());
    Collection<IdentityEntity> identityEntities = getAllIdentityEntity(OrganizationIdentityProvider.NAME).values();
    int count = 0, next = 0, size = identityEntities.size();
    Iterator<IdentityEntity> allIdentityEntity = identityEntities.iterator();
    while (allIdentityEntity.hasNext()) {
      IdentityEntity entity = allIdentityEntity.next();
      ++next;
      if(next < count) {
        continue;
      }
      String remoteId = entity.getRemoteId();
      Collection<RelationshipEntity> entities = entity.getRelationship().getRelationships().values();
      int entitiesSize = entities.size();
      if (entitiesSize > 0) {
        removeRelationshipEntity(entities);
      }
      //
      ++count;
      if(count % LIMIT_THRESHOLD == 0) {
        RequestLifeCycle.end();
        RequestLifeCycle.begin(PortalContainer.getInstance());
        //
        allIdentityEntity = getAllIdentityEntity(OrganizationIdentityProvider.NAME).values().iterator();
        next = 0;
      }
      processLog(String.format("Removed %s confirm of user %s", entitiesSize, remoteId), size, count);
    }
    //
    RequestLifeCycle.end();
    LOG.info(String.format("Done to remove %s main relationships on %s(ms)", number, (t2 = System.currentTimeMillis()) - t1));

    LOG.info("Remove sender relationships ...");
    RequestLifeCycle.begin(PortalContainer.getInstance());
    number = 0; count = 0;
    allIdentityEntity = getAllIdentityEntity(OrganizationIdentityProvider.NAME).values().iterator();
    while (allIdentityEntity.hasNext()) {
      IdentityEntity entity = allIdentityEntity.next();
      Collection<RelationshipEntity> entities = entity.getSender().getRelationships().values();
      int entitiesSize = entities.size();
      removeRelationshipEntity(entities);
      //
      ++count;
      processLog(String.format("Removed %s sender of user %s", entitiesSize, entity.getRemoteId()), size, count);
    }
    //
    RequestLifeCycle.end();
    LOG.info(String.format("Done to remove %s sender relationships on %s(ms)", number, (t1 = System.currentTimeMillis()) - t2));

    LOG.info("Remove receiver relationships ...");
    RequestLifeCycle.begin(PortalContainer.getInstance());
    number = 0; count = 0;
    allIdentityEntity = getAllIdentityEntity(OrganizationIdentityProvider.NAME).values().iterator();
    while (allIdentityEntity.hasNext()) {
      IdentityEntity entity = allIdentityEntity.next();
      Collection<RelationshipEntity> entities = entity.getReceiver().getRelationships().values();
      int entitiesSize = entities.size();
      removeRelationshipEntity(entities);
      ++count;
      processLog(String.format("Removed %s receiver of user %s", entitiesSize, entity.getRemoteId()), size, count);
    }
    //
    RequestLifeCycle.end();
    LOG.info(String.format("Done to remove %s receiver relationships on %s(ms)", number, (t2 = System.currentTimeMillis()) - t1));
    LOG.info("Done all removed relationships from JCR on " + (t2 - t) + "(ms)");
  }

  @Override
  @Managed
  @ManagedDescription("Manual to stop run miguration data of relationships from JCR to MYSQL.")
  public void stop() {
    super.stop();
  }

  protected String getListenerKey() {
    return EVENT_LISTENER_KEY;
  }
}
