package eu.uqasar.web.dashboard.widget.sonarqualitywidget;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import ro.fortsoft.wicket.dashboard.AbstractWidget;
import ro.fortsoft.wicket.dashboard.Widget;
import ro.fortsoft.wicket.dashboard.web.WidgetView;

import com.googlecode.wickedcharts.highcharts.options.ChartOptions;
import com.googlecode.wickedcharts.highcharts.options.Options;
import com.googlecode.wickedcharts.highcharts.options.SeriesType;
import com.googlecode.wickedcharts.highcharts.options.Title;
import com.googlecode.wickedcharts.highcharts.options.series.Point;
import com.googlecode.wickedcharts.highcharts.options.series.PointSeries;

import eu.uqasar.model.measure.MetricMeasurement;
import eu.uqasar.service.PlatformSettingsService;
import eu.uqasar.service.dataadapter.MetricDataService;

/**
 * A sample widget fetching sample data from a Sonar instance. 
 * The used Sonar installation can be changed in the settings. 
 *
 */
public class SonarQualityWidget extends AbstractWidget {

	/**
	 * 
	 */
	private static final long serialVersionUID = -105357652483079696L;
	private String project; 
	private static SonarQualityFactory chartDataFactory;

	public SonarQualityWidget() {
		super();
		title = "Source code quality";
	}
	
	@Override
	public void init() {
		
		try {
			InitialContext ic = new InitialContext();
			PlatformSettingsService service = (PlatformSettingsService) ic.lookup("java:module/PlatformSettingsService");
			project = service.getValueByKey("sonar_project");
		} catch (NamingException e) {
			e.printStackTrace();
		}
		
		if (!settings.containsKey("project")) {
			settings.put("project", "U-QASAR");
		}
		this.setTitle("Source code quality");
	}

	@Override
	public WidgetView createView(String viewId) {
		return new SonarQualityWidgetView(viewId, new Model<Widget>(this));
	}
	
	public static SonarQualityFactory getChartDataFactory() {
		return chartDataFactory;
	}

	public static void setChartDataFactory(SonarQualityFactory chartDataFactory) {
		SonarQualityWidget.chartDataFactory = chartDataFactory;
	}

	public String getChartData() {
		if (chartDataFactory == null) {
			throw new RuntimeException("ChartDataFactory cannot be null. Use ChartWidget.setChartDataFactory(...)");
		}

		return chartDataFactory.createChart(this);
	}
	
	@Override
	public boolean hasSettings() {
		return true;
	}
	

	@Override
	public Panel createSettingsPanel(String settingsPanelId) {
		return new SonarQualityWidgetSettingsPanel(settingsPanelId, 
				new Model<SonarQualityWidget>(this));
	}

	
	/**
	 * Get the measurements
	 * @return
	 */
	public List<MetricMeasurement> getMeasurements(String period) {

		List<MetricMeasurement> measurements = new ArrayList<MetricMeasurement>();

		if (settings.get("project") != null) {
			project = settings.get("project");
		}
		
		try {
			InitialContext ic = new InitialContext();
			MetricDataService dataService = (MetricDataService) ic.lookup("java:module/MetricDataService");
			
			Date latestSnapshotDate = dataService.getLatestDate();
			if (latestSnapshotDate != null) {
			    if(period.compareToIgnoreCase("Latest") == 0){
			        measurements = dataService.getLatestMeasurementByProject(project);
			    }else{
			        measurements = dataService.getMeasurementsForProjectByPeriod(project, period);
			    }
			}
		} catch (NamingException e) {
			e.printStackTrace();
		}
		return measurements;
	}
	
	
	/**
	 * 
	 * @param 
	 * @return
	 */
	public Options getChartOptions(List<MetricMeasurement> metrics ){			
			
			String group;
			if (settings.get("metric") == null) {
				group = "Code Lines related";
			} else {
				group = settings.get("metric");
			}
			
			// get latest
			List<MetricMeasurement> latestMetrics = getLatestMetricDataSet(metrics);
			List<MetricMeasurement> metricGroup = getMetricsForGroup(group, latestMetrics);
		
			Options options = new Options();
			ChartOptions chartOptions =  new ChartOptions();
			SeriesType seriesType = SeriesType.COLUMN; 
			chartOptions.setType(seriesType);    
			Title chartTitle = new Title(title + " of " + group + " Metrics");
			options.setTitle(chartTitle);

			for(MetricMeasurement metric: metricGroup){				
				PointSeries series = new PointSeries();	
				series.setType(seriesType);
				series.addPoint(new Point(metric.getMetricType(), new Double(metric.getValue())));
				series.setName(metric.getMetricType());
				options.addSeries(series);
			}
			options.setChartOptions(chartOptions);	
			
			return options;
	}
	
	
    public Options getChartOptionsDifferently(List<MetricMeasurement> metrics,String individualMetric) {

        String group;
        if (settings.get("metric") == null) {
            group = "Code Lines related";
        } else {
            group = settings.get("metric");
        }

        // get latest
        //List<MetricMeasurement> latestMetrics = getLatestMetricDataSet(metrics);
       // List<MetricMeasurement> metricGroup = getMetricsForGroup(group, latestMetrics);

        Options options = new Options();
        ChartOptions chartOptions = new ChartOptions();
        SeriesType seriesType = SeriesType.COLUMN;
        chartOptions.setType(seriesType);
        Title chartTitle = new Title(title + " of " + group + " Metrics");
        options.setTitle(chartTitle);

        for (MetricMeasurement metric : metrics) {
            
            if(metric.getMetricType().compareToIgnoreCase(individualMetric) == 0){
            PointSeries series = new PointSeries();
            series.setType(seriesType);
            series.addPoint(new Point(metric.getMetricType(), new Double(metric.getValue())));
            series.setName(metric.getMetricType());
            options.addSeries(series);
            }
        }
        options.setChartOptions(chartOptions);

        return options;
    }

	
	

	private List<MetricMeasurement> getLatestMetricDataSet(List<MetricMeasurement> metrics) {
		// incoming metrics are already sorted by timestamp descending (newest at beginning)
		List<MetricMeasurement> latestMetrics = new ArrayList<>();
		if (metrics != null && !metrics.isEmpty()) {
			for (int i = 0; i < metrics.size(); i++) {
				latestMetrics.add(metrics.get(i));
			}
		}
		return latestMetrics;
	}

	private List<MetricMeasurement> getMetricsForGroup(String group, List<MetricMeasurement> metrics) {

		List<MetricMeasurement> 	lines = new ArrayList<>(),
										complex = new ArrayList<>(),
										struct = new ArrayList<>(),
										density = new ArrayList<>(),
										test = new ArrayList<>();
		
		if (metrics != null && !metrics.isEmpty()) {
			if (group.equals("Code Lines related")){
				for (MetricMeasurement m: metrics){
					if (m.getMetricType().contains("LINE") && !m.getMetricType().contains("DENSITY") || m.getMetricType().equals("NCLOC") || m.getMetricType().equals("STATEMENTS") ){
						lines.add(m);
					}
				}
				return lines;
			} else if (group.equals("Complexity related")){
				for (MetricMeasurement m: metrics){
					if(m.getMetricType().contains("COMPLEXITY") && !m.getMetricType().contains("DENSITY")){
						complex.add(m);
					}
				}
				return complex;
			} else if (group.equals("Test related")){
				for (MetricMeasurement m: metrics){
					if (m.getMetricType().contains("TEST") && !m.getMetricType().contains("DENSITY")){
						test.add(m);
					}
				}
				return test;
			} else if (group.equals("Density related")){
				for (MetricMeasurement m: metrics){
					if (m.getMetricType().contains("DENSITY")){
						System.out.println(m.getMetricType().contains("DENSITY"));
						System.out.println(m.getMetricType());
						density.add(m);
					}
				}
				return density;
			} else {
				for (MetricMeasurement m: metrics){
					if (!m.getMetricType().contains("LINE") && 
							!m.getMetricType().contains("COMPLEXITY") &&
							!m.getMetricType().contains("TEST") && 
							!m.getMetricType().contains("DENSITY") &&
							!m.getMetricType().equals("NCLOC") && 
							!m.getMetricType().equals("STATEMENTS")){
						struct.add(m);
					}
				}
				return struct;
			}
		}
		return new ArrayList<MetricMeasurement>();
	}
	
	/**
	 * 
	 * @param list
	 * @return
	 */
	private List<MetricMeasurement> removeDuplicates(List<MetricMeasurement> list){
		Set<MetricMeasurement> set = new LinkedHashSet<>(list);
		list.clear();
		list.addAll(set);
		return list;
	}	
}
