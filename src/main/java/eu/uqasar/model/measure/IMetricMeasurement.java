package eu.uqasar.model.measure;

import java.util.Date;

import eu.uqasar.model.settings.adapter.AdapterSettings;
import eu.uqasar.model.tree.Project;

public interface IMetricMeasurement {

	public String getName();

	public void setName(String name);

	public MetricSource getMetricSource();

	public void setMetricSource(MetricSource metricSource);
	
	public String getMetricType();
	
	public void setMetricType(String metricType);
		
	public String getValue();

	public void setValue(String value);
	
	public AdapterSettings getAdapter();
	
	public void setAdapter(AdapterSettings adapter);
	
	public Project getProject();
	
	public void setProject(Project proj);
	
	public Date getTimeStamp();
	
	public void setTimeStamp(Date ts); 
	
}
