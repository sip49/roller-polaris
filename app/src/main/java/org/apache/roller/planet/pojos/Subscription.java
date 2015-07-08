/*
 * Copyright 2005 Sun Microsystems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Source file modified from the original ASF source; all changes made
 * are also under Apache License.
 */
package org.apache.roller.planet.pojos;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.apache.roller.util.UUIDGenerator;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;


/**
 * Planet Subscription.
 */
@Entity
@Table(name="planet_subscription")
@NamedQueries({
        @NamedQuery(name="Subscription.getAll",
                query="SELECT p FROM Subscription p"),
        @NamedQuery(name="Subscription.getAllOrderByFeedURL",
                query="SELECT p FROM Subscription p ORDER BY p.feedURL DESC"),
        @NamedQuery(name="Subscription.getAllOrderByInboundBlogsDesc",
                query="SELECT p FROM Subscription p ORDER BY p.inboundblogs DESC"),
        @NamedQuery(name="Subscription.getByGroupOrderByInboundBlogsDesc",
                query="SELECT p FROM Subscription p JOIN p.groups g WHERE g.handle = ?1 ORDER BY p.inboundblogs DESC"),
        @NamedQuery(name="Subscription.getByFeedURL",
                query="SELECT p FROM Subscription p WHERE p.feedURL = ?1")
})
public class Subscription implements Serializable, Comparable<Subscription> {
    
    // attributes
    private String id = UUIDGenerator.generateUUID();
    private String title;
    private String author;
    private String feedUrl;
    private String siteUrl;
    private Date lastUpdated;
    private int inboundlinks = 0;
    private int inboundblogs = 0;

    // associations
    private Set<PlanetGroup> groups = new HashSet<PlanetGroup>();
    private Set<SubscriptionEntry> entries = new HashSet<SubscriptionEntry>();
    
    
    public Subscription() {}
    
    @Id
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    @Basic(optional=false)
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }

    @Column(name="feed_url", nullable=false)
    public String getFeedURL() {
        return feedUrl;
    }
    
    public void setFeedURL(String feedUrl) {
        this.feedUrl = feedUrl;
    }

    @Column(name="site_url")
    public String getSiteURL() {
        return siteUrl;
    }
    
    public void setSiteURL(String siteUrl) {
        this.siteUrl = siteUrl;
    }


    @Column(name="last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    public Date getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Column(name="inbound_links")
    public int getInboundlinks() {
        return inboundlinks;
    }

    public void setInboundlinks(int inboundlinks) {
        this.inboundlinks = inboundlinks;
    }

    @Column(name="inbound_blogs")
    public int getInboundblogs() {
        return inboundblogs;
    }

    public void setInboundblogs(int inboundblogs) {
        this.inboundblogs = inboundblogs;
    }


    @ManyToMany
    @JoinTable(name="planet_group_subscription",
            joinColumns = { @JoinColumn(name="subscription_id", nullable=false)},
            inverseJoinColumns = { @JoinColumn(name="group_id", nullable=false)})
    public Set<PlanetGroup> getGroups() {
        return groups;
    }
    
    // private because there is no need for people to do this
    private void setGroups(Set<PlanetGroup> groups) {
        this.groups = groups;
    }


    @OneToMany(targetEntity=org.apache.roller.planet.pojos.SubscriptionEntry.class,
            cascade=CascadeType.ALL, mappedBy="subscription")
    public Set<SubscriptionEntry> getEntries() {
        return entries;
    }
    
    // private because there is no need for people to do this
    private void setEntries(Set<SubscriptionEntry> entries) {
        this.entries = entries;
    }
    
    
    /**
     * Add a SubscriptionEntry to this Subscription.
     */
    public void addEntry(SubscriptionEntry entry) {
        // bi-directional one-to-many
        entry.setSubscription(this);
        this.getEntries().add(entry);
    }
    
    /**
     * Add a collection of SubscriptionEntry to this Subscription.
     */
    public void addEntries(Collection<SubscriptionEntry> newEntries) {
        // bi-directional one-to-many
        for (SubscriptionEntry entry : newEntries) {
            entry.setSubscription(this);
        }
        this.getEntries().addAll(newEntries);
    }

    // for backwards compatability?
    @Transient
    public String getName() {
        return getTitle();
    }
    
    // for backwards compatability?
    @Transient
    public String getURL() {
        return siteUrl;
    }

    /**
     * Compares subscriptions based on concatenation of title and feed URL.
     * This ensures that feeds are sorted by title, but that identical titles
     * don't make feeds equal.
     */
    public int compareTo(Subscription other) {
        String otherString = other.getTitle() + other.getFeedURL();
        String thisString = getTitle() + getFeedURL();
        return thisString.compareTo(otherString);
    }

    /**
     * Determines if subscriptions are equal by comparing feed URLs.
     */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Subscription)) {
            return false;
        }
        final Subscription that = (Subscription) other;
        return this.feedUrl.equals(that.getFeedURL());
    }

    public int hashCode() {
        return this.feedUrl.hashCode();
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("{");
        buf.append(getFeedURL()).append(", ");
        buf.append(getSiteURL()).append(", ");
        buf.append(getTitle()).append(", ");
        buf.append(getAuthor()).append(", ");
        buf.append(getLastUpdated());
        buf.append("}");

        return buf.toString();
    }

}
