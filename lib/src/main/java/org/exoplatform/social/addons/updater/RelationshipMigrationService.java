package org.exoplatform.social.addons.updater;

import java.util.Iterator;

import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.social.addons.storage.dao.RelationshipDAO;
import org.exoplatform.social.core.chromattic.entity.IdentityEntity;
import org.exoplatform.social.core.chromattic.entity.RelationshipEntity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.api.RelationshipStorage;

@Managed
@ManagedDescription("Social migration relationships from JCR to MYSQl service.")
@NameTemplate({@Property(key = "service", value = "social"), @Property(key = "view", value = "migration-relationships") })
public class RelationshipMigrationService extends AbstractMigrationService<Relationship> {

  private final RelationshipDAO relationshipDAO;
  private final RelationshipStorage relationshipStorage;

  public RelationshipMigrationService(IdentityStorage identityStorage,
                                      RelationshipDAO relationshipDAO,
                                      RelationshipStorage relationshipStorage,
                                      ProfileMigrationService profileMigration) {
    super(identityStorage);
    this.relationshipDAO = relationshipDAO;
    this.relationshipStorage = relationshipStorage;
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
    Iterator<IdentityEntity> allIdentityEntity = getAllIdentityEntity().values().iterator();
    while (allIdentityEntity.hasNext()) {
      if(forkStop) {
        return;
      }
      IdentityEntity identityEntity = (IdentityEntity) allIdentityEntity.next();
      //
      LOG.info("Migration relationship for user: " + identityEntity.getRemoteId());
      Identity identityFrom = new Identity(OrganizationIdentityProvider.NAME, identityEntity.getRemoteId());
      identityFrom.setId(identityEntity.getId());
      //
      Iterator<RelationshipEntity> it = identityEntity.getRelationship().getRelationships().values().iterator();
      while (it.hasNext()) {
        RelationshipEntity relationshipEntity = it.next();
        Identity identityTo = identityStorage.findIdentityById(relationshipEntity.getTo().getId());
        Relationship relationship = new Relationship(identityFrom, identityTo);
        //
        if (SENDER.equals(relationshipEntity.getParent().getName())
            || RECEIVER.equals(relationshipEntity.getParent().getName())) {
          relationship.setStatus(Relationship.Type.PENDING);
        } else {
          relationship.setStatus(Relationship.Type.CONFIRMED);
        }
        //
        relationshipStorage.saveRelationship(relationship);
      }
    }
  }

  @Override
  protected void afterMigration() throws Exception {
  }

  @Override
  @Managed
  @ManagedDescription("Manual to stop run miguration data of relationships from JCR to MYSQL.")
  public void stop() {
    super.stop();
  }
}
