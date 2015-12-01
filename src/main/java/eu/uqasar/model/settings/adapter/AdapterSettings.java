package eu.uqasar.model.settings.adapter;

/*
 * #%L
 * U-QASAR
 * %%
 * Copyright (C) 2012 - 2015 U-QASAR Consortium
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.jasypt.util.text.BasicTextEncryptor;
import org.jboss.solder.logging.Logger;

import eu.uqasar.adapter.exception.uQasarException;
import eu.uqasar.model.AbstractEntity;
import eu.uqasar.model.measure.MetricMeasurement;
import eu.uqasar.model.measure.MetricSource;
import eu.uqasar.model.tree.Project;
import eu.uqasar.service.dataadapter.MetricDataService;
import eu.uqasar.util.UQasarUtil;

@Entity
@XmlRootElement
@Table(name = "adaptersettings")
@Indexed
public class AdapterSettings extends AbstractEntity {

	private static final long serialVersionUID = -3535301965200581906L;
	private String name; // Name of the settings shown to the user
	private MetricSource metricSource; // Type of the metric source
	private String url; // Address where the adapter resides
	private String adapterUsername; // Username for the service used by the adapter
	private String adapterPassword; // Password for the service used by the adapter
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
	private Date latestUpdate; // Timestamp of the last update
    @Nullable
	@ManyToOne
	@IndexedEmbedded
	private Project project;
	private static Logger logger = Logger.getLogger(AdapterSettings.class);
    private String adapterProject;
    private String adapterTestPlan;
	@OneToMany(cascade = CascadeType.ALL, mappedBy="adapter", orphanRemoval=true)
    private List<MetricMeasurement> measurements = new ArrayList<>();

	
	/**
	 * 
	 * @return the settingsName
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the settingsName to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the metricSource
	 */
	public MetricSource getMetricSource() {
		return metricSource;
	}
	
	/**
	 * @param metricSource the metricSource to set
	 */
	public void setMetricSource(MetricSource metricSource) {
		this.metricSource = metricSource;
	}
	
	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}
	
	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	
	/**
	 * @return the username
	 */
	public String getAdapterUsername() {
		return adapterUsername;
	}

	/**
	 * @param adapterUsername the username to set
	 */
	public void setAdapterUsername(String adapterUsername) {
		this.adapterUsername = adapterUsername;
	}
	
	/**
	 * @return the project
	 */
	public Project getProject() {
		return project;
	}

	/**
	 * @param project the project to set
	 */
	public void setProject(Project project) {
		this.project = project;
	}

	/**
	 * Returns the decrypted (decryption executed by using jasypt) adapter password 
	 * @return the adapterPassword
	 */
	public String getAdapterPassword() {
		BasicTextEncryptor textDecryptor = new BasicTextEncryptor();
		String decryptionPwd = UQasarUtil.getEncDecPwd();
		textDecryptor.setPassword(decryptionPwd);
		String decrypted = textDecryptor.decrypt(this.adapterPassword);
		return decrypted;
	}

	/**
	 * Setter for the password (the password is first encrypted by using jasypt)  
	 * @param adapterPassword the adapterPassword to set
	 */
	public void setAdapterPassword(String adapterPassword) {
		BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
		String encryptionPwd = UQasarUtil.getEncDecPwd();
		textEncryptor.setPassword(encryptionPwd);
		String encrypted = textEncryptor.encrypt(adapterPassword);
		this.adapterPassword = encrypted;
	}

	/**
	 * @return the latestUpdate
	 */
	public Date getLatestUpdate() {
		return latestUpdate;
	}

	/**
	 * @param latestUpdate the latestUpdate to set
	 */
	public void setLatestUpdate(Date latestUpdate) {
		this.latestUpdate = latestUpdate;
	}
	
    /**
     * 
     * @return 
     */
    public String getAdapterProject() {
        return adapterProject;
    }

    /**
     * 
     * @param adapterProject 
     */
    public void setAdapterProject(String adapterProject) {
        this.adapterProject = adapterProject;
    }

    /**
     * Get TestPlan property for TestLink adapter.
     * @return TestPlan
     */
    public String getAdapterTestPlan() {
        return adapterTestPlan;
    }

    /**
     * Set TestPlan property for TestLink adapter.
     * @param adapterTestPlan 
     */
    public void setAdapterTestPlan(String adapterTestPlan) {
        this.adapterTestPlan = adapterTestPlan;
    }

    
	public List<MetricMeasurement> getMeasurements() {
		return measurements;
	}

	public void setMeasurements(List<MetricMeasurement> measurements) {
		this.measurements = measurements;
	}

	/**
	 * Get a new snapshot of adapter data
	 */
	public void updateAdapterData() {
        logger.debug("AdapterSettings::updateAdapterData()");
        try {
            InitialContext ic = new InitialContext();
            
            MetricDataService dataService = (MetricDataService) ic.lookup("java:module/MetricDataService");
            dataService.updateAdapterData(this);            
            
            // Update the project tree
            UQasarUtil.updateTree(this.getProject());
            
        } catch (NamingException | uQasarException e) {
            e.printStackTrace();
        }

    }

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "AdapterSettings [name=" + name + ", metricSource="
				+ metricSource + ", url=" + url + ", latestUpdate=" + latestUpdate + ", project=" + project
				+ ", adapterProject=" + adapterProject + "]";				
	}
}
