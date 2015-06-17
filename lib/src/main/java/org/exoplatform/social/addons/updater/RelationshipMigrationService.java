package org.exoplatform.social.addons.updater;

import java.util.Iterator;

import javax.jcr.Node;

import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.social.addons.storage.dao.ProfileItemDAO;
import org.exoplatform.social.addons.storage.dao.RelationshipDAO;
import org.exoplatform.social.addons.storage.dao.jpa.GenericDAOImpl;
import org.exoplatform.social.addons.storage.entity.RelationshipItem;
import org.exoplatform.social.addons.storage.session.SocialSessionLifecycle;
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

  private final RelationshipDAO relationshipDAO;
  private final ProfileItemDAO profileItemDAO;

  public RelationshipMigrationService(IdentityStorage identityStorage,
                                      RelationshipDAO relationshipDAO,
                                      ProfileMigrationService profileMigration,
                                      ProfileItemDAO profileItemDAO) {
    super(identityStorage);
    this.relationshipDAO = relationshipDAO;
    this.profileItemDAO = profileItemDAO;
  }

  @Override
  protected void beforeMigration() throws Exception {
  }

  @Override
  @Managed
  @ManagedDescription("Manual to start run miguration data of relationships from JCR to MYSQL.")
  public void doMigration() throws Exception {
    if (relationshipDAO.count() > 0) {
      return;
    }
    LOG.info("Stating to migration relationships from JCR to MYSQL........");
    long t = System.currentTimeMillis();
    int count = 0;
    SocialSessionLifecycle sessionLifecycle = GenericDAOImpl.lifecycleLookup();
    Iterator<IdentityEntity> allIdentityEntity = getAllIdentityEntity().values().iterator();
    while (allIdentityEntity.hasNext()) {
      if(forkStop) {
        return;
      }
      IdentityEntity identityEntity = (IdentityEntity) allIdentityEntity.next();
      //
      LOG.info("Migration relationship for user: " + identityEntity.getRemoteId());
      long t1 = System.currentTimeMillis();
      int c2 = 0;
      Identity identityFrom = new Identity(OrganizationIdentityProvider.NAME, identityEntity.getRemoteId());
      identityFrom.setId(identityEntity.getId());
      //
      Iterator<RelationshipEntity> it = identityEntity.getRelationship().getRelationships().values().iterator();
      c2 =+ migrateRelationshipEntity(it, identityFrom, false, Relationship.Type.CONFIRMED);
      //
      it = identityEntity.getSender().getRelationships().values().iterator();
      c2 =+ migrateRelationshipEntity(it, identityFrom, false, Relationship.Type.OUTGOING);
      //
      it = identityEntity.getReceiver().getRelationships().values().iterator();
      c2 =+ migrateRelationshipEntity(it, identityFrom, true, Relationship.Type.INCOMING);
      //
      sessionLifecycle.flush();
      ++count;
      LOG.info(String.format("Done to migration %s relationships for user %s from JCR to MYSQL on %s(ms)", c2, identityEntity.getRemoteId(), (System.currentTimeMillis() - t1)));
    }
    LOG.info(String.format("Done to migration relationships of %s users from JCR to MYSQL on %s(ms)", count,  (System.currentTimeMillis() - t)));
  }
  
  private int migrateRelationshipEntity(Iterator<RelationshipEntity> it, Identity owner, boolean isIncoming, Relationship.Type status) {
    int c2 = 0;
    while (it.hasNext()) {
      RelationshipEntity relationshipEntity = it.next();
      String receiverId = relationshipEntity.getTo().getId().equals(owner.getId()) ? relationshipEntity.getFrom().getId() : relationshipEntity.getTo().getId();
      Identity receiver = identityStorage.findIdentityById(receiverId);
      //
      RelationshipItem entity = new RelationshipItem();
      entity.setSenderId(isIncoming ? receiver.getId() : owner.getId());
      entity.setReceiverId(isIncoming ? owner.getId() : receiver.getId());
      entity.setStatus(status);
      entity.setReceiver(profileItemDAO.findProfileItemByIdentityId(isIncoming ? owner.getId() : receiver.getId()));
      //
      relationshipDAO.create(entity);
      ++c2;
    }
    return c2;
  }

  @Override
  protected void afterMigration() throws Exception {
    Iterator<IdentityEntity> allIdentityEntity = getAllIdentityEntity().values().iterator();
    while (allIdentityEntity.hasNext()) {
      IdentityEntity identityEntity = (IdentityEntity) allIdentityEntity.next();
      Node identityNode = (Node) getSession().getJCRSession().getItem(identityEntity.getPath());
      identityNode.getNode("soc:relationship").remove();
      identityNode.getNode("soc:receiver").remove();
      identityNode.getNode("soc:sender").remove();
      getSession().getJCRSession().save();
    }
  }
  
  @Override
  @Managed
  @ManagedDescription("Manual to stop run miguration data of relationships from JCR to MYSQL.")
  public void stop() {
    super.stop();
  }
}
