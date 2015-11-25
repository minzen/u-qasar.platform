package eu.uqasar.model.measure;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.search.annotations.Indexed;

import eu.uqasar.model.AbstractEntity;
import eu.uqasar.model.settings.adapter.AdapterSettings;
import eu.uqasar.model.tree.Project;

/**
 *
 * A general class for measurement results
 *
 */
@Entity
@XmlRootElement
@Table(name = "metricmeasurement")
@Indexed
public class MetricMeasurement extends AbstractEntity implements
		IMetricMeasurement {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String name;
	private String metric;
	@Lob
	private String value;
	@ManyToOne(fetch = FetchType.LAZY)
	// To which adapter the measurement belongs
	private AdapterSettings adapter; 
	@ManyToOne
	private Project project;
	private Date timeStamp;
	// Extra content that is desired to be stored
	@Lob
	private String content;
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getMetric() {
		return metric;
	}

	@Override
	public void setMetric(String metric) {
		this.metric = metric;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public void setValue(String value) {
		this.value = value;

	}

	@Override
	public AdapterSettings getAdapter() {
		return adapter;
	}

	@Override
	public void setAdapter(AdapterSettings adapter) {
		this.adapter = adapter;
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public void setProject(Project proj) {
		this.project = proj;
	}

	@Override
	public Date getTimeStamp() {
		return timeStamp;
	}

	@Override
	public void setTimeStamp(Date ts) {
		this.timeStamp = ts;
	}
	
	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	@Override
	public String toString() {
		return "MetricMeasurement [name=" +this.name
				+", metric=" +this.metric  
				+", value=" +this.value
				+", adapter=" +this.adapter
				+", project=" +this.project
				+", timeStamp=" +this.timeStamp
				+"]";
	}

}
