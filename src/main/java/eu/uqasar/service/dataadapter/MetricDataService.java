package eu.uqasar.service.dataadapter;

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
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import eu.uqasar.adapter.SystemAdapter;
import eu.uqasar.adapter.exception.uQasarException;
import eu.uqasar.adapter.model.Measurement;
import eu.uqasar.model.measure.MetricMeasurement;
import eu.uqasar.model.measure.MetricMeasurement_;
import eu.uqasar.model.measure.MetricSource;
import eu.uqasar.model.settings.adapter.AdapterSettings;
import eu.uqasar.qualifier.Conversational;
import eu.uqasar.service.AbstractService;
import eu.uqasar.sonar.adapter.SonarAdapter;
import eu.uqasar.util.UQasarUtil;

@Stateless
@Conversational
public class MetricDataService extends AbstractService<MetricMeasurement> {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Inject
	private AdapterSettingsService settingsService;
	
	public MetricDataService() {
		super(MetricMeasurement.class);
	}

	/**
	 * 
	 * @return
	 */
	public List<MetricMeasurement> getAllMetricObjects() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MetricMeasurement> query = 
				cb.createQuery(MetricMeasurement.class);
		query.from(MetricMeasurement.class);
		List<MetricMeasurement> resultList = 
				em.createQuery(query).getResultList();
		return resultList;
	}	

	/**
	 * 
	 * @param processes
	 */
	public void delete(Collection<MetricMeasurement> metrics) {
		for (MetricMeasurement m : metrics) {
			delete(m);
		}
	}

	/**
	 * 
	 * @param first
	 * @param count
	 * @return
	 */
	public List<MetricMeasurement> getAllByAscendingName(int first, 
			int count) {
		logger.infof("loading all MetricMeasurement ordered by " 
				+"ascending name ...");
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MetricMeasurement> criteria = 
				cb.createQuery(MetricMeasurement.class);
		Root<MetricMeasurement> root = 
				criteria.from(MetricMeasurement.class);
		criteria.orderBy(cb.asc(root.get(MetricMeasurement_.name)));
		return em.createQuery(criteria).setFirstResult(first)
				.setMaxResults(count).getResultList();
	}	
	
	/**
	 * 
	 * @param first
	 * @param count
	 * @param adapter
	 * @return
	 */
	public List<MetricMeasurement> getAllByAdapter(int first, int count, AdapterSettings adapter) {
		logger.info("Get measurements for adapter: " +adapter);

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MetricMeasurement> query = cb.createQuery(MetricMeasurement.class);
		Root<MetricMeasurement> root = query.from(MetricMeasurement.class);
		Predicate condition = cb.equal(root.get(MetricMeasurement_.adapter), adapter);
		query.orderBy(cb.asc(root.get(MetricMeasurement_.name)));
		query.where(condition);
		return em.createQuery(query).setFirstResult(first)
				.setMaxResults(count).getResultList();
	}
	
	/**
	 * Count the measurement results belonging to a specific adapter
	 * @param adapter
	 * @return
	 */
	public int countAllByAdapter(AdapterSettings adapter) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MetricMeasurement> query = cb.createQuery(MetricMeasurement.class);
		Root<MetricMeasurement> root = query.from(MetricMeasurement.class);
		Predicate condition = cb.equal(root.get(MetricMeasurement_.adapter), adapter);
		query.where(condition);		
		return em.createQuery(query).getResultList().size();
	}	


	/**
	 * 
	 * @param project
	 * @return
	 */
	public List<MetricMeasurement> getMeasurementsForProject(
			String project) {
		logger.info("Obtaining measurements for the project: " +project);
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MetricMeasurement> query = 
				cb.createQuery(MetricMeasurement.class);
		Root<MetricMeasurement> root = 
				query.from(MetricMeasurement.class);
		Predicate condition = cb.equal(
				root.get(MetricMeasurement_.name), project);
		query.where(condition);
		query.orderBy(cb.desc(root.get(MetricMeasurement_.timeStamp)));
		return em.createQuery(query).getResultList();
	}


	/**
	 * 
	 * @param project
	 * @param date
	 * @return
	 */
	public List<MetricMeasurement> getMeasurementsForProjectByDate(
			String project, Date date) {
		logger.info("Obtaining measurements for the project: " +project);
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MetricMeasurement> query = 
				cb.createQuery(MetricMeasurement.class);
		Root<MetricMeasurement> root = 
				query.from(MetricMeasurement.class);
		Predicate condition = cb.equal(
				root.get(MetricMeasurement_.name), project);
		Predicate condition2 = cb.equal(
				root.get(MetricMeasurement_.timeStamp), date);
		Predicate condition3 = cb.and(condition, condition2);
		query.where(condition3);
		query.orderBy(cb.desc(root.get(MetricMeasurement_.timeStamp)));
		return em.createQuery(query).getResultList();
	}

	/**
	 * 
	 * @param project
	 * @return
	 */
	public MetricMeasurement getLatestMeasurementByProjectAndMetric(
			String project, String metric) {
		logger.info("Obtaining measurements for the project: " +project 
				+" and metric: " +metric);

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MetricMeasurement> query = 
				cb.createQuery(MetricMeasurement.class);
		Root<MetricMeasurement> root = 
				query.from(MetricMeasurement.class);
		Predicate condition = cb.equal(root.get(MetricMeasurement_.name), 
				project);
		Predicate condition2 = cb.equal(root.get(
				MetricMeasurement_.metric), metric);
		Predicate condition3 = cb.and(condition, condition2);
		query.where(condition3);
		query.orderBy(cb.desc(root.get(MetricMeasurement_.timeStamp)));
		List<MetricMeasurement> measurements = 
				em.createQuery(query).getResultList();
		MetricMeasurement measurement = null;
		if (measurements != null && measurements.size() > 0 
				&& measurements.get(0) != null) {
			measurement = measurements.get(0); // Get the most "fresh" result 
		}
		return measurement;
	}
	
	
	/**
	 * Get the latest date of measurement snapshots
	 * @return
	 */
	public Date getLatestDate() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MetricMeasurement> query = 
				cb.createQuery(MetricMeasurement.class);
		Root<MetricMeasurement> root = 
				query.from(MetricMeasurement.class);
		query.select(root);
		query.orderBy(cb.desc(root.get(MetricMeasurement_.timeStamp)));
		Date latest;
		try {
			MetricMeasurement m = em.createQuery(query).setMaxResults(1).getSingleResult(); 
			latest = m.getTimeStamp();
		} catch (NoResultException nre) {
			latest = null;
		}
		
		return latest;
	}
	
	
	/**
	 * Fetch an updated set of adapter data.
	 * @param adapterSettings Settings of the adapter to be used
	 * @throws uQasarException
	 */
	public void updateAdapterData(AdapterSettings adapterSettings) 
			throws uQasarException {		
		Date snapshotTimeStamp = new Date();
		logger.info("Get measurements for all " +adapterSettings.getMetricSource().name() +" metrics at " 
		+snapshotTimeStamp +adapterSettings.toString());
		// Timestamp for the measurement snapshot
		String boundSystemURL = adapterSettings.getUrl();
		String username = null;
		String password = null;
		String credentials = ""; 
		if (adapterSettings.getAdapterUsername() != null) {
			username = adapterSettings.getAdapterUsername();
		} 
		if (adapterSettings.getAdapterPassword() != null) {
			password = adapterSettings.getAdapterPassword();
		}
		if (username != null && password != null) {
			if (!username.isEmpty() && !password.isEmpty()) {
				credentials = username +":" +password;
			}
		}

		// Select which adapter is to be used
		SystemAdapter adapter = null;
		List<String> metrics = new ArrayList<>();
		if (adapterSettings.getMetricSource().equals(MetricSource.StaticAnalysis)) {
			adapter = new SonarAdapter();
			metrics = UQasarUtil.getSonarMetricNames();
		} 

		if (adapter != null) {
			// Iterate the metric names and fetch results for each of these
			// create a persistent entity for each measurement
			for (String metric : metrics) {
				
				List<Measurement> metricMeasurements = 
						adapter.query(boundSystemURL, credentials, metric);
				
				// Iterate the results for a metric
				for (Measurement m : metricMeasurements) {
					
					logger.info("Measurement " +m);
					if (m.getMeasurement() != null) {
						logger.info("measurement: " +m.getMeasurement());
						MetricMeasurement measurement = new MetricMeasurement();
		                measurement.setValue(m.getMeasurement());
						measurement.setTimeStamp(snapshotTimeStamp);
						measurement.setMetric(metric);
						measurement.setProject(adapterSettings.getProject());
						measurement.setAdapter(adapterSettings);
						// Create an entity
						create(measurement);
					}
			   		adapterSettings.setLatestUpdate(snapshotTimeStamp);
			        settingsService.update(adapterSettings);
				}
			}			
		}
	}

	/**
	 * Get all measurements belonging to an adapter
	 * @param settings
	 * @return
	 */
	public List<MetricMeasurement> getAllByAdapter(AdapterSettings settings) {
		logger.info("Get measurements for adapter: " +settings);

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MetricMeasurement> query = cb.createQuery(MetricMeasurement.class);
		Root<MetricMeasurement> root = query.from(MetricMeasurement.class);
		Predicate condition = cb.equal(root.get(MetricMeasurement_.adapter), settings);
		query.where(condition);
		return em.createQuery(query).getResultList();
	}
}

