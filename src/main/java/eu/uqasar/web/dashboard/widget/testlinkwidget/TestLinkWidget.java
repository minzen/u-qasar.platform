package eu.uqasar.web.dashboard.widget.testlinkwidget;

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
import eu.uqasar.model.tree.Project;
import eu.uqasar.service.dataadapter.MetricDataService;
import eu.uqasar.service.tree.TreeNodeService;

public class TestLinkWidget extends AbstractWidget {


	/**
	 * 
	 */
	private static final long serialVersionUID = 4495147834793704125L;
	private Project project;

	private static TestLinkFactory chartDataFactory;	


	public TestLinkWidget() {
		super();
		title = "TestLink Metrics";
	}

	@Override
	public void init() {
		if(settings.get("project") != null && !settings.get("project").isEmpty()){
			String name = settings.get("project");
			project = getProject(name);
		}
	}

	@Override
	public WidgetView createView(String viewId) {
		return new TestLinkWidgetView(viewId, new Model<Widget>(this));
	}

	public static TestLinkFactory getChartDataFactory() {
		return chartDataFactory;
	}

	public static void setChartDataFactory(TestLinkFactory chartDataFactory) {
		TestLinkWidget.chartDataFactory = chartDataFactory;
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
		return new TestLinkSettingsPanel(settingsPanelId, 
				new Model<TestLinkWidget>(this));
	}


	/**
	 * Get the measurements
	 * @return
	 */
	public List<MetricMeasurement> getMeasurements(String period) {

		List<MetricMeasurement> measurements = new ArrayList<MetricMeasurement>();

		try {
			InitialContext ic = new InitialContext();
			MetricDataService dataService = (MetricDataService) ic.lookup("java:module/MetricDataService");

			Date latestSnapshotDate = dataService.getLatestDate();
			if (latestSnapshotDate != null) {
			    if (period.compareToIgnoreCase("Latest") == 0){
			        measurements = dataService.getMeasurementsForProjectByLatestDate(project.getAbbreviatedName());
			    } else{
			        measurements = dataService.getMeasurementsForProjectByPeriod(project.getAbbreviatedName(), period);
			    }
			}
		} catch (NamingException e) {
			e.printStackTrace();
		}
		return measurements;
	}

	private Project getProject(String projName) {
		Project pro = null;
		if (projName == null || projName.isEmpty()) {
			projName = "U-QASAR Platform Development";
		}
		TreeNodeService treeNodeService = null;
		try {
			InitialContext ic = new InitialContext();
			treeNodeService = (TreeNodeService) ic.lookup("java:module/TreeNodeService");
			pro = treeNodeService.getProjectByName(projName);		
		} catch (NamingException e) {
			e.printStackTrace();
		}
		return pro;
	}


	/**
	 * 
	 * @param metrics
	 * @return
	 */
	public Options getChartOptions(List<MetricMeasurement> metrics) {

		Options options = new Options();
		ChartOptions chartOptions =  new ChartOptions();
		SeriesType seriesType = SeriesType.PIE; 
		chartOptions.setType(seriesType);    
		Title chartTitle = new Title("Total " + title + ": " + getTotalMetric(metrics));
		options.setTitle(chartTitle);		
		PointSeries series = new PointSeries();	
		series.setType(seriesType);

		// remove TOTAL metric	
		List<MetricMeasurement> noTotal = removeTotalMetric(metrics); 
		if (noTotal != null && !noTotal.isEmpty()) {
			int items = 4;
			if (noTotal.size() < items) {
				items = noTotal.size();
			}
				// we obtain the metrics, sorted by timestamp (descending)
				for (int tlm= 0; tlm < items; tlm++){				
					MetricMeasurement metric = noTotal.get(tlm);				
					if (metric.getMetricType().equals("TEST_P")) {				
						series.addPoint(new Point("Tests Passed", new Double(metric.getValue())));					
					} else if (metric.getMetricType().equals("TEST_F")) {
						series.addPoint(new Point("Tests Failed", new Double(metric.getValue())));
					} else if (metric.getMetricType().equals("TEST_B")) {
						series.addPoint(new Point("Tests Blocking", new Double(metric.getValue())));
					} else {
						series.addPoint(new Point("Tests Not Executed", new Double(metric.getValue())));
					}
				}
			options.addSeries(series);
			options.setChartOptions(chartOptions);					
		}

		return options;
	}


	/**
     * 
     * @param metrics
     * @return
     */
    public Options getChartOptionsDifferently(List<MetricMeasurement> metrics,String individualMetric) {

        Options options = new Options();
        ChartOptions chartOptions =  new ChartOptions();
        SeriesType seriesType = SeriesType.PIE; 
        chartOptions.setType(seriesType);    
        Title chartTitle = new Title("Total " + title + ": " + getTotalMetric(metrics));
        options.setTitle(chartTitle);       
        PointSeries series = new PointSeries(); 
        series.setType(seriesType);

        // remove TOTAL metric  
        List<MetricMeasurement> noTotal = removeExtraMetrics(metrics,individualMetric); 
        if (noTotal != null && !noTotal.isEmpty()) {
            int items = 4;
            if (noTotal.size() < items) {
                items = noTotal.size();
            }
                // we obtain the metrics, sorted by timestamp (descending)
                for (int tlm= 0; tlm < items; tlm++){               
                    MetricMeasurement metric = noTotal.get(tlm);                
                    if (metric.getMetricType().equals("TEST_P")) {              
                        series.addPoint(new Point("Tests Passed", new Double(metric.getValue())));                  
                    } else if (metric.getMetricType().equals("TEST_F")) {
                        series.addPoint(new Point("Tests Failed", new Double(metric.getValue())));
                    } else if (metric.getMetricType().equals("TEST_B")) {
                        series.addPoint(new Point("Tests Blocking", new Double(metric.getValue())));
                    } else {
                        series.addPoint(new Point("Tests Not Executed", new Double(metric.getValue())));
                    }
                }
            options.addSeries(series);
            options.setChartOptions(chartOptions);                  
        }

        return options;
    }
	
	

	/**
	 * 
	 * @param metrics
	 * @return
	 */
	private String getTotalMetric(List<MetricMeasurement> metrics) {
		String total = "";
		for(MetricMeasurement tlm : metrics){
			if(tlm.getMetricType().equals("TEST_TOTAL")){
				total = tlm.getValue();
			}
		}
		return total;
	}

	/**
	 * 
	 * @param metrics
	 * @return
	 */
	private List<MetricMeasurement> removeTotalMetric(List<MetricMeasurement> metrics) {

		List<MetricMeasurement> noTotal = new ArrayList<>();
		if (metrics != null && !metrics.isEmpty()) {
			for (MetricMeasurement tlm : metrics){
				if (!tlm.getMetricType().equals("TEST_TOTAL")){
					noTotal.add(tlm);
				}
			}
		}
		return noTotal;
	}
	
	/** This method removes all the metrics other than given individual Metric
     * 
     * @param metrics
     * @return
     */
    private List<MetricMeasurement> removeExtraMetrics(List<MetricMeasurement> metrics,String individualMetric) {

        List<MetricMeasurement> noTotal = new ArrayList<>();
        if (metrics != null && !metrics.isEmpty()) {
            for (MetricMeasurement tlm : metrics){
                if (tlm.getMetricType().equals(individualMetric)){
                    noTotal.add(tlm);
                }
            }
        }
        return noTotal;
    }

}
