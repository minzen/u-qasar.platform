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


import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;

import us.monoid.json.JSONException;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;

import com.google.gson.Gson;

import eu.uqasar.adapter.SystemAdapter;
import eu.uqasar.adapter.exception.uQasarException;
import eu.uqasar.adapter.model.Measurement;
import eu.uqasar.cubes.adapter.CubesAdapter;
import eu.uqasar.gitlab.adapter.GitlabAdapter;
import eu.uqasar.jenkins.adapter.JenkinsAdapter;
import eu.uqasar.jira.adapter.JiraAdapter;
import eu.uqasar.model.measure.JiraMetricMeasurement;
import eu.uqasar.model.measure.MetricMeasurement;
import eu.uqasar.model.measure.MetricMeasurement_;
import eu.uqasar.model.measure.MetricSource;
import eu.uqasar.model.settings.adapter.AdapterSettings;
import eu.uqasar.model.tree.Project;
import eu.uqasar.qualifier.Conversational;
import eu.uqasar.service.AbstractService;
import eu.uqasar.sonar.adapter.SonarAdapter;
import eu.uqasar.testlink.adapter.TestLinkAdapter;
import eu.uqasar.util.UQasarUtil;

/**
 * Methods for obtaining metric data
 * 
 */
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
		
		// SonarQube Adapter
		if (adapterSettings.getMetricSource().equals(MetricSource.StaticAnalysis)) {
			
			if (testConnectionToServer(boundSystemURL, 10000) == false) return;

			adapter = new SonarAdapter();
			metrics = UQasarUtil.getSonarMetricNames();

			// Iterate the metric names and fetch results for each of these
			// create a persistent entity for each measurement
			for (String metric : metrics) {
				List<Measurement> metricMeasurements = 
						adapter.query(boundSystemURL, credentials, metric);

				for (Measurement m : metricMeasurements) {
					// Read the measurement to a JSON array 
					if (m.getMeasurement() != null) {
						String json = m.getMeasurement();
						Gson gson = new Gson();
						MetricMeasurement[] measurement = gson.fromJson(json, 
								MetricMeasurement[].class);
						for (int i = 0; i < measurement.length; i++) {
							// Add a timestamp and metric name to the object
							measurement[i].setMetricSource(MetricSource.StaticAnalysis);
							measurement[i].setTimeStamp(snapshotTimeStamp);
							measurement[i].setMetricType(metric);
							measurement[i].setProject(adapterSettings.getProject());
							measurement[i].setAdapter(adapterSettings);
							// Create an entity
							create(measurement[i]);
						}
					}					
				}
			}
		}
		else if (adapterSettings.getMetricSource().equals(MetricSource.IssueTracker)) {
			
			if (testConnectionToServer(boundSystemURL, 10000) == false) return;

			adapter = new JiraAdapter();
			List<String> metricNames = UQasarUtil.getJiraMetricNames();

			// TODO: Re-implement the adapter to fetch the required content already from there. 
			// Init resty for fetching the JSON content
			Resty resty = new Resty();
			// encoding byte array into base64
			byte[] encoded = Base64.encodeBase64(credentials.getBytes());     
			String encodedString = new String(encoded);
			resty.alwaysSend("Authorization", "Basic " + encodedString);

			// Iterate the metric names and fetch results for each of these
			// create a persistent entity for each measurement 
			for (String metric : metricNames) {
				logger.info("Obtaining JIRA measurements for metric: "+metric);
				List<Measurement> metricMeasurements = adapter.query(boundSystemURL, credentials, metric);
				for (Measurement measurement : metricMeasurements) {
					String json = measurement.getMeasurement();
					System.out.println("JSON: " +json);
					Gson gson = new Gson();
					JiraMetricMeasurement[] jiraMetricMeasurement = gson.fromJson(json, 
							JiraMetricMeasurement[].class);
					for (int i = 0; i < jiraMetricMeasurement.length; i++) {
						// Add a timestamp and metric name to the object
						jiraMetricMeasurement[i].setTimeStamp(snapshotTimeStamp);
						jiraMetricMeasurement[i].setMetricType(metric);
						jiraMetricMeasurement[i].setProject(adapterSettings.getProject());
						jiraMetricMeasurement[i].setAdapter(adapterSettings);

						// Get the url from the measurement for fetching the JSON content 
						String url = jiraMetricMeasurement[i].getSelf();
						System.out.println("Jira URL:  " +url);
						String jsonContent = "";
						try {
							JSONResource res = resty.json(url);
							us.monoid.json.JSONObject jobj = res.toObject();
							jsonContent = jobj.toString();
							jiraMetricMeasurement[i].setContent(jsonContent);
						} catch (IOException e) {
							e.printStackTrace();
						} catch (JSONException e) {
							e.printStackTrace();
						}

						MetricMeasurement metricMeasurement = new MetricMeasurement();
						metricMeasurement.setAdapter(jiraMetricMeasurement[i].getAdapter());
						metricMeasurement.setContent(jiraMetricMeasurement[i].getContent());
						metricMeasurement.setMetricSource(MetricSource.IssueTracker);
						metricMeasurement.setMetricType(jiraMetricMeasurement[i].getMetricType());
						metricMeasurement.setName(jiraMetricMeasurement[i].getName());
						metricMeasurement.setProject(jiraMetricMeasurement[i].getProject());
						metricMeasurement.setAdapter(jiraMetricMeasurement[i].getAdapter());
						metricMeasurement.setTimeStamp(jiraMetricMeasurement[i].getTimeStamp());
						metricMeasurement.setValue(jiraMetricMeasurement[i].getValue());
						
						// persist the measurement
						create(metricMeasurement);
					}
				}
			}
		} 
		else if (adapterSettings.getMetricSource().equals(MetricSource.VersionControl)) {

			if (testConnectionToServer(boundSystemURL, 10000) == false) return;
			
			adapter = new GitlabAdapter();
			// The adapter should maybe provide a list of the metrics supported

			// Iterate the metric names and fetch results for each of these
			// create a persistent entity for each measurement
			for (String metric : UQasarUtil.getGitlabMetricNames()) {

				List<Measurement> metricMeasurements = 
						adapter.query(boundSystemURL, credentials, metric);

				// Iterate the results for a metric
				for (Measurement m : metricMeasurements) {					
					if (m.getMeasurement() != null) {
						MetricMeasurement measurement = new MetricMeasurement();
						measurement.setValue(m.getMeasurement());
						measurement.setTimeStamp(snapshotTimeStamp);
						measurement.setMetricType(metric);
						measurement.setProject(adapterSettings.getProject());
						measurement.setAdapter(adapterSettings);
						// Create an entity
						create(measurement);
					}
				}
			}
		}
		else if (adapterSettings.getMetricSource().equals(MetricSource.ContinuousIntegration)) {

			if (testConnectionToServer(boundSystemURL, 10000) == false) return;

			adapter = new JenkinsAdapter();
			// Iterate the metric names and fetch results for each of these
			// create a persistent entity for each measurement
			for (String metric : UQasarUtil.getJenkinsMetricNames()) {

				List<Measurement> metricMeasurements = 
						adapter.query(boundSystemURL, credentials, metric);

				// Iterate the results for a metric
				for (Measurement m : metricMeasurements) {					
					if (m.getMeasurement() != null) {
						MetricMeasurement measurement = new MetricMeasurement();
						measurement.setValue(m.getMeasurement());
						measurement.setTimeStamp(snapshotTimeStamp);
						measurement.setMetricType(metric);
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
		else if (adapterSettings.getMetricSource().equals(MetricSource.TestingFramework)) {

			if (testConnectionToServer(boundSystemURL, 10000) == false) return;

			TestLinkAdapter tlAdapter = new TestLinkAdapter(boundSystemURL, adapterSettings.getAdapterPassword());

			String testProjectName = adapterSettings.getAdapterProject();
			String testPlanName = adapterSettings.getAdapterTestPlan();

			for (String metric : UQasarUtil.getTestLinkMetricNames()) {

				logger.info("String metric "+ metric);

				Map<String, String> params = new HashMap<>();
				params.put("testProjectName", testProjectName);
				params.put("testPlanName", testPlanName);

				List<Measurement> metricMeasurements = tlAdapter.getMeasurement(metric, params);
				logger.info("metricMeasurments : "+ metricMeasurements.get(0));
				// Iterate the results for a metric
				for (Measurement m : metricMeasurements) {
					// Read the measurement to a JSON array and check whether it contains
					// results for the desired project
					if (m.getMeasurement() != null) {
						logger.info("Testlink measurement: " +m.getMeasurement());
						MetricMeasurement measurement = new MetricMeasurement();
						measurement.setName(testProjectName);
						measurement.setValue(m.getMeasurement());
						measurement.setTimeStamp(snapshotTimeStamp);
						measurement.setMetricType(metric);
						measurement.setProject(adapterSettings.getProject());
						measurement.setAdapter(adapterSettings);
						create(measurement);
					}
				}
			}
		}
		else if (adapterSettings.getMetricSource().equals(MetricSource.CubeAnalysis)) {

			if (testConnectionToServer(boundSystemURL, 10000) == false) return;
			adapter = new CubesAdapter();

			List<String> metricNames = UQasarUtil.getCubesMetricNames();

			// Iterate the metric names and fetch results for each of these
			// create a persistent entity for each measurement 
			for (String metric : metricNames) {
				logger.info("Obtaining CUBES measurements for metric: " + metric);
				List<Measurement> metricMeasurements =  adapter.query(boundSystemURL, credentials ,metric);
				for (Measurement measurement : metricMeasurements) {
					String json = measurement.getMeasurement();
					Gson gson = new Gson();
					MetricMeasurement[] metricMeasurement = gson.fromJson(json, 
							MetricMeasurement[].class);
					for (int i = 0; i < metricMeasurement.length; i++) {
						// Add a timestamp and metric name to the object
						metricMeasurement[i].setTimeStamp(snapshotTimeStamp);
						metricMeasurement[i].setMetricType(metric);
						metricMeasurement[i].setProject(adapterSettings.getProject());
						metricMeasurement[i].setAdapter(adapterSettings);
						create(metricMeasurement[i]);
					}
				}
			}

			// Fetch the actual content in JSON format
			storeJSONPayload(adapterSettings);

			// Update values of all the Cubes based metrics of the adaptor setting Project
		}

		// Set timestamp to the adapter settings to indicate the latest update
		adapterSettings.setLatestUpdate(snapshotTimeStamp);
		settingsService.update(adapterSettings);				
	}
	
	/**
	 * Update the content of a CubesMetricMeasurement that is behind a URL (payload as JSON)
	 * @param settings
	 */
	public void storeJSONPayload(AdapterSettings settings) {
		/*
		logger.info("Get JSON Payload for all CUBES metrics.");
		String creds = settings.getAdapterUsername() +":" +settings.getAdapterPassword();

		//encoding  byte array into base 64
		byte[] encoded = Base64.encodeBase64(creds.getBytes());     
		String encodedString = new String(encoded);
		//		resty.alwaysSend("Authorization", "Basic " + encodedString);

		Date currentDatasetDate = getLatestDate();
		for (String metric : UQasarUtil.getCubesMetricNames()) {
			List<MetricMeasurement> measurements;
			try {
				measurements = getMeasurementsByMetricAndDate(metric, currentDatasetDate);

				// Iterate 
				for (MetricMeasurement MetricMeasurement : measurements) {
					String url = MetricMeasurement.getSelf();
					String jsonContent = "";

					try {
						JSONResource res = getJSON(url);
						us.monoid.json.JSONObject jobj = res.toObject();

						logger.info(jobj.toString());
						jsonContent = jobj.toString();
						logger.info("JSON Content: " +jsonContent);


						// Value that can be select as metric - ONLY for cut
						CubesMetricMeasurement.setValue(jobj.getJSONObject("summary").getString("count"));

						// TBR, JSON with collection of Values will be used within Analytic Workbench
						// This stores all the JSON info, maybe is too much and contains unneeded info
						//CubesMetricMeasurement.setJsonContent(jsonContent);

						// This ONLY stores the cells data,
						CubesMetricMeasurement.setJsonContent(jobj.getJSONArray("cells").toString());

						// Persist in database
						update(CubesMetricMeasurement);

					} catch (IOException e) {
						e.printStackTrace();
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			} catch (uQasarException e) {
				e.printStackTrace();
			}
		}
		*/
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

	public MetricMeasurement getLatestMeasurementByProjectAndMetric(
			Project project, String metric) {

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
	 * Get a list of available Sonar project names 
	 * @return
	 */
	public List<String> getSonarProjects() {
		logger.info("Getting Sonar projects...");
		List<MetricMeasurement> measurements = getAllMetricObjects();
		List<String> projects = new ArrayList<String>();
		for (MetricMeasurement metricMeasurement : measurements) {
			if (metricMeasurement.getAdapter().getMetricSource().equals(MetricSource.StaticAnalysis)) {
				String project = metricMeasurement.getName();
				if (!projects.contains(project)) {
					projects.add(project);
				}
			}
		}		
		return projects;
	}

	
	/**
	 * Get a list of available TestLink project names 
	 * @return
	 */
	public List<String> getTestLinkProjects() {
		logger.info("Getting TestLink projects...");
		List<MetricMeasurement> measurements = getAllTestLinkMetricObjects();
		List<String> projects = new ArrayList<String>();
		for (MetricMeasurement measurement : measurements) {
			String project = measurement.getName();
			if (!projects.contains(project)) {
				projects.add(project);
			}
		}

		return projects;
	}

	/**
	 * 
	 * @return
	 */
	public List<MetricMeasurement> getAllTestLinkMetricObjects() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MetricMeasurement> query = 
				cb.createQuery(MetricMeasurement.class);
		Root<MetricMeasurement> root = query.from(MetricMeasurement.class);
		Predicate condition = cb.equal(root.get(MetricMeasurement_.metric), MetricSource.TestingFramework);
		query.where(condition);		
		List<MetricMeasurement> resultList = 
				em.createQuery(query).getResultList();
		return resultList;
	}	
	
	/**
	 * 
	 * @param project
	 * @param date
	 * @return
	 */
	public List<MetricMeasurement> getMeasurementsForProjectByPeriod(String project, String period) {
		logger.info("Obtaining measurements for the project: " +project +" within the: " + period);
		List<MetricMeasurement> measurements = new ArrayList<>();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MetricMeasurement> query = 
				cb.createQuery(MetricMeasurement.class);
		Root<MetricMeasurement> root = 
				query.from(MetricMeasurement.class);

		Predicate condition = cb.equal(
				root.get(MetricMeasurement_.name), project);

		Date from = getDateForPeriod(period);		
		Date to = DateTime.now().toDate();
		Predicate condition2 = cb.between(root.get(MetricMeasurement_.timeStamp), from, to);

		Predicate condition3 = cb.and(condition, condition2);
		query.where(condition3);
		query.orderBy(cb.desc(root.get(MetricMeasurement_.timeStamp)));
		if (em.createQuery(query).getResultList() != null && !em.createQuery(query).getResultList().isEmpty()) {
			measurements = em.createQuery(query).getResultList();
		}
		return measurements;
	}

	/**
	 * 
	 * @param project
	 * @return
	 */
	public List<MetricMeasurement> getMeasurementsForProjectByLatestDate(String project) {
		List<MetricMeasurement> results = getMeasurementsForProject(project);

		Date newDate = null;
		for(MetricMeasurement tMM : results){

			if (newDate == null ||  tMM.getTimeStamp().compareTo(newDate) > 0){
				newDate = tMM.getTimeStamp();
			}  
		}

		List<MetricMeasurement> resultsLatest = 
				getMeasurementsForProjectByDate(project,newDate);

		return resultsLatest;
	}

	
	/**
	 * Get a date for a time period denoted by the parameter  
	 * @param period
	 * @return
	 */
	private Date getDateForPeriod(String period) {
		DateTime now = DateTime.now();
		Date date;

		if(period.equals("Last Year")){
			date = now.minusMonths(12).toDate(); 
		} else if(period.equals("Last 6 Months")){
			date = now.minusMonths(6).toDate(); 
		} else if(period.equals("Last Month")){
			date = now.minusMonths(1).toDate(); 
		} else if(period.equals("Last Week")){
			date = now.minusWeeks(1).toDate();
		} else{
			date = now.minusYears(5).toDate();
		}
		//System.out.println("datedatedate:"+date);

		return date;
	}

	/**
     * 
     * @param project
     * @return
     */
    public List<MetricMeasurement> getLatestMeasurementByProject(
            String project) {
        List<MetricMeasurement> resultList = getMeasurementsForProject(project);
        Date newestDate = null;
        for(MetricMeasurement smm : resultList){
            if (newestDate == null || smm.getTimeStamp().compareTo(newestDate) > 0){
                newestDate = smm.getTimeStamp();
            }  
        }
        List<MetricMeasurement> resultListLatest = getMeasurementsForProjectByDate(project,newestDate);
        return resultListLatest;               
    }

    
	/**
	 * 
	 * @param metric
	 * @param date
	 * @return
	 * @throws uQasarException
	 */
	public List<MetricMeasurement> getMeasurementsPerProjectByMetricWithinPeriod(Project project, String metric, String period) 
			throws uQasarException {

		List<MetricMeasurement> measurements = new ArrayList<>();
		if (project != null && metric != null && period != null) {
			logger.info("Count measurements for project + " + project.getAbbreviatedName() +" and metric: " +metric);

			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<MetricMeasurement> query = cb.createQuery(MetricMeasurement.class);
			Root<MetricMeasurement> root = query.from(MetricMeasurement.class);

			Predicate condition1 = cb.equal(root.get(MetricMeasurement_.metric), metric);
			Predicate condition2 = cb.equal(root.get(MetricMeasurement_.project), project);

			Date from = getDateForPeriod(period);		
			Date to = DateTime.now().toDate();
			Predicate condition3 = cb.between(root.get(MetricMeasurement_.timeStamp), from, to);
			//		System.out.println("from:"+from);
			//		System.out.println("to:"+to);

			Predicate condition4 = cb.and(condition1, condition2, condition3);
			query.where(condition4);
			query.orderBy(cb.desc(root.get(MetricMeasurement_.timeStamp)));
			measurements = em.createQuery(query).getResultList();
		}

		return measurements;
	}


	/** 
	 * @param metric
	 * @param project
	 * @return
	 * @throws uQasarException
	 */
	public List<MetricMeasurement> getMeasurementsPerProjectByMetricWithLatestDate(Project project, String metric) 
			throws uQasarException {
		logger.info("Count measurements for project + " + project.getAbbreviatedName() +" and metric: " +metric + "with latest date");

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MetricMeasurement> query = cb.createQuery(MetricMeasurement.class);
		Root<MetricMeasurement> root = query.from(MetricMeasurement.class);

		Predicate condition1 = cb.equal(root.get(MetricMeasurement_.metric), metric);
		Predicate condition2 = cb.equal(root.get(MetricMeasurement_.project), project);


		Predicate condition4 = cb.and(condition1, condition2);
		query.where(condition4);
		query.orderBy(cb.desc(root.get(MetricMeasurement_.timeStamp)));
		List<MetricMeasurement> returnResults = em.createQuery(query).getResultList();

		Date newDate = null;
		for (MetricMeasurement jMM : returnResults ){
			if (newDate == null || jMM.getTimeStamp().compareTo(newDate) > 0){
				newDate = jMM.getTimeStamp();
			} 

		}

		List<MetricMeasurement> returnResultsLatest = getMeasurementsByMetricAndDate(metric,newDate);
		return returnResultsLatest;
	}

	/**
	 * 
	 * @param metric
	 * @return
	 * @throws uQasarException
	 */
	public List<MetricMeasurement> getMeasurementsByMetricAndDate(String metric, Date date) 
			throws uQasarException {
		logger.info("Get measurements for metric: " +metric);

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MetricMeasurement> query = cb.createQuery(MetricMeasurement.class);
		Root<MetricMeasurement> root = query.from(MetricMeasurement.class);
		Predicate condition = cb.equal(root.get(MetricMeasurement_.metric), metric);
		Predicate condition2 = cb.equal(root.get(MetricMeasurement_.timeStamp), date);
		Predicate condition3 = cb.and(condition, condition2);
		query.where(condition3);
		query.orderBy(cb.desc(root.get(MetricMeasurement_.timeStamp)));
		return em.createQuery(query).getResultList();
	}
	
	/**
	 * 
	 * @param metric
	 * @param date
	 * @return
	 * @throws uQasarException
	 */
	public int countMeasurementsByMetricAndDate(String metric, Date date) 
			throws uQasarException {
		logger.info("Count measurements for metric: " +metric + " and date: " +date);

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MetricMeasurement> query = cb.createQuery(MetricMeasurement.class);
		Root<MetricMeasurement> root = query.from(MetricMeasurement.class);
		Predicate condition = cb.equal(root.get(MetricMeasurement_.metric), metric);
		Predicate condition2 = cb.equal(root.get(MetricMeasurement_.timeStamp), date);
		Predicate condition3 = cb.and(condition, condition2);
		query.where(condition3);
		query.orderBy(cb.desc(root.get(MetricMeasurement_.timeStamp)));
		Integer res = em.createQuery(query).getResultList().size();
		logger.info("Results' count: " +res);
		return res;
	}

	/**
	 * 
	 * @param metric
	 * @param date
	 * @return
	 * @throws uQasarException
	 */
	public int countMeasurementsPerProjectByMetricWithinPeriod(Project project, String metric, String period) 
			throws uQasarException {
		logger.info("Count measurements for metric: " +metric);

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<MetricMeasurement> query = cb.createQuery(MetricMeasurement.class);
		Root<MetricMeasurement> root = query.from(MetricMeasurement.class);
		Predicate condition1 = cb.equal(root.get(MetricMeasurement_.metric), metric);
		Predicate condition2 = cb.equal(root.get(MetricMeasurement_.project), project);

		Date from = getDateForPeriod(period);		
		Date to = DateTime.now().toDate();
		Predicate condition3 = cb.between(root.get(MetricMeasurement_.timeStamp), from, to);
		Predicate condition4 = cb.and(condition1, condition2, condition3);
		query.where(condition4);
		query.orderBy(cb.desc(root.get(MetricMeasurement_.timeStamp)));
		Integer res = em.createQuery(query).getResultList().size();
		logger.info("Results' count: " +res);
		return res;
	}

	/**
	 * 
	 * @param metric
	 * @param date
	 * @return
	 * @throws uQasarException
	 */
	public int countMeasurementsPerProjectByMetricWithLatestDate(Project project, String metric) 
			throws uQasarException {
		Integer res = getMeasurementsPerProjectByMetricWithLatestDate(project,metric).size();
		logger.info("Results' count: " +res);
		return res;
	}
	
	/**
	 * Obtain a metric value for a specific metric source and metric type (and possibly a project) 
	 * @param metricSource
	 * @param metricType
	 * @param project
	 * @return stored metric value or null if no value exists
	 */
	public Float getMetricValue(MetricSource metricSource, String metricType, Project project) {

		// The value to be stored
		Float storedMetricValue = (float) 0;
		
		MetricMeasurement res = getLatestMeasurementByProjectAndMetric(project, metricType);
		if (res != null) {
			storedMetricValue = Float.valueOf(res.getValue());
		} 

		return storedMetricValue;
	}

	
	/**
	 *  Check if the connection to the provided URL is possible within the provided timeout
	 * 
	 * @param url
	 * @param timeout
	 * @return 	True or False according to the connection success
	 * 			
	 */
	public static boolean testConnectionToServer(String url, int timeout) {
		try {
			URL myUrl = new URL(url);
			URLConnection connection = myUrl.openConnection();
			connection.setConnectTimeout(timeout);
			connection.connect();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}




