package org.exoplatform.social.addons.storage.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@ExoEntity
@Table(name = "SOC_PROFILES", uniqueConstraints = @UniqueConstraint(columnNames = { "IDENTITY_ID" }))
public class Profile {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name="PROFILE_ID")
  private Long id;

  @Column(length = 36, name="IDENTITY_ID")
  private String identityId;
  private String fullName;
  private String firstName;
  private String lastName;
  //join all value of experiences in to one value, separate by comma
  private String skills;
  private String positions;
  private String organizations;
  private String jobsDescription;

  public Profile() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getIdentityId() {
    return identityId;
  }

  public void setIdentityId(String identityId) {
    this.identityId = identityId;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getSkills() {
    return skills;
  }

  public void setSkills(String skills) {
    this.skills = skills;
  }

  public String getPositions() {
    return positions;
  }

  public void setPositions(String positions) {
    this.positions = positions;
  }

  public String getOrganizations() {
    return organizations;
  }

  public void setOrganizations(String organizations) {
    this.organizations = organizations;
  }

  public String getJobsDescription() {
    return jobsDescription;
  }

  public void setJobsDescription(String jobsDescription) {
    this.jobsDescription = jobsDescription;
  }
}
