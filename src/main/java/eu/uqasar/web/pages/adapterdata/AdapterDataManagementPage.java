package eu.uqasar.web.pages.adapterdata;

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
import java.util.Iterator;

import javax.inject.Inject;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.CheckGroupSelector;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameAppender;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.ajax.BootstrapAjaxPagingNavigator;
import eu.uqasar.model.measure.MetricMeasurement;
import eu.uqasar.model.settings.adapter.AdapterSettings;
import eu.uqasar.service.dataadapter.AdapterSettingsService;
import eu.uqasar.service.dataadapter.MetricDataService;
import eu.uqasar.web.components.JSTemplates;
import eu.uqasar.web.components.ModalActionButton;
import eu.uqasar.web.components.NotificationModal;
import eu.uqasar.web.pages.BasePage;
import eu.uqasar.web.provider.EntityProvider;

public class AdapterDataManagementPage extends BasePage {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Inject
	private MetricDataService dataService;
	@Inject 
	private AdapterSettingsService adapterService;
	// how many items do we show per page
	private static final int itemsPerPage = 10;
	private final WebMarkupContainer dataContainer = newDataContainer();
	private final Modal deleteConfirmationModal;
	private final CheckGroup<MetricMeasurement> dataGroup;
	private final AjaxSubmitLink deleteSelectedButton;

	/**
	 * Constructor building the page
	 *
	 * @param parameters
	 */
	public AdapterDataManagementPage(final PageParameters parameters) {

		super(parameters);
		
		Long adapterId = parameters.get("id").toLongObject();
		
		final Form<MetricMeasurement> deleteForm = new Form<>("deleteForm");
		add(deleteForm);

		// add checkgroup for selecting multiple products
		deleteForm.add(dataGroup = newDataCheckGroup());

		// add the container holding list of existing products
		dataGroup.add(dataContainer.setOutputMarkupId(true));

		dataContainer.add(new CheckGroupSelector(
				"dataGroupSelector", dataGroup));

		DataView<MetricMeasurement> measurements = new DataView<MetricMeasurement>(
				"measurements", new DataProvider(adapterId), itemsPerPage) {


					@Override
					protected void populateItem(final Item<MetricMeasurement> item) {
						final MetricMeasurement metricMeasurement = item.getModelObject();

						item.add(new Check<>("dataCheck", item
								.getModel(), dataGroup));

						item.add(new Label("name", new PropertyModel<String>(
								metricMeasurement, "name")));

						item.add(new Label("metric", new PropertyModel<String>(
								metricMeasurement, "metric")));
						
						item.add(new Label("value", new PropertyModel<String>(
								metricMeasurement, "value")));

						item.add(new Label("timeStamp", new PropertyModel<Date>(
								metricMeasurement, "timeStamp")));
					}
				};
		dataContainer.add(measurements);

		// add links for table pagination
		dataContainer.add(new BootstrapAjaxPagingNavigator(
				"navigatorHead", measurements));
		dataContainer.add(new BootstrapAjaxPagingNavigator(
				"navigatorFoot", measurements));

		// add button to delete selected items
		dataContainer
				.add(deleteSelectedButton = newDeleteSelectedButton(dataGroup));

		// add confirmation modal for deleting products
		add(deleteConfirmationModal = newDeleteConfirmationModal());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.uqasar.web.BasePage#getPageTitleModel()
	 */
	@Override
	protected IModel<String> getPageTitleModel() {
		return new StringResourceModel("page.title", this, null);
	}

	/**
	 *
	 * @return
	 */
	private WebMarkupContainer newDataContainer() {
		return new WebMarkupContainer("dataContainer") {

			@Override
			public void renderHead(IHeaderResponse response) {
				super.renderHead(response);
				// add javascript to load tagsinput plugin
				response.render(OnLoadHeaderItem.forScript(String.format(
						JSTemplates.LOAD_TABLE_SORTER, "data-list")));
			}
		;
	}

	;
	}

	/**
	 * 
	 * @return
	 */
	private CheckGroup<MetricMeasurement> newDataCheckGroup() {
		CheckGroup<MetricMeasurement> checkGroup = new CheckGroup<MetricMeasurement>(
				"dataGroup", new ArrayList<MetricMeasurement>());
		checkGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				updateDeleteSelectedButton(target);
			}
		});
		return checkGroup;
	}

	/**
	 *
	 * @param productGroup
	 * @return
	 */
	private AjaxSubmitLink newDeleteSelectedButton(
			final CheckGroup<MetricMeasurement> dataGroup) {
		return new AjaxSubmitLink("deleteSelected") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				// only enabled if at least one Product is selected
				if (dataGroup.getModelObject().isEmpty()) {
					add(new CssClassNameAppender(Model.of("disabled")) {

						// remove css class when component is rendered again
						@Override
						public boolean isTemporary(Component component) {
							return true;
						}
					});
					setEnabled(false);
				} else {
					setEnabled(true);
				}
			}

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				deleteConfirmationModal.appendShowDialogJavaScript(target);
			}
		};
	}
	

	/**
	 *
	 * @return
	 */
	private NotificationModal newDeleteConfirmationModal() {
		final NotificationModal notificationModal = new NotificationModal(
				"deleteConfirmationModal", new StringResourceModel(
						"delete.confirmation.modal.header", this, null),
				new StringResourceModel("delete.confirmation.modal.message",
						this, null), false);
		notificationModal.addButton(new ModalActionButton(notificationModal,
				Buttons.Type.Primary, new StringResourceModel(
						"delete.confirmation.modal.submit.text", this, null),
				true) {
					private static final long serialVersionUID = -8579196626175159237L;

					@Override
					protected void onAfterClick(AjaxRequestTarget target) {
						// confirmed --> delete
						deleteSelectedMeasurements(dataGroup.getModelObject(),
								target);
						// close modal
						closeDeleteConfirmationModal(notificationModal, target);
					}
				});
		notificationModal.addButton(new ModalActionButton(notificationModal,
				Buttons.Type.Default, new StringResourceModel(
						"delete.confirmation.modal.cancel.text", this, null),
				true) {
					private static final long serialVersionUID = 8931306355855637710L;

					@Override
					protected void onAfterClick(AjaxRequestTarget target) {
						// Cancel clicked --> do nothing, close modal
						closeDeleteConfirmationModal(notificationModal, target);
					}
				});
		return notificationModal;
	}

	/**
	 *
	 * @param measurements
	 * @param target
	 */
	private void deleteSelectedMeasurements(
			Collection<MetricMeasurement> measurements, AjaxRequestTarget target) {
		String message = new StringResourceModel("measurement.selected.deleted",
				this, null).getString();
		dataService.delete(measurements);
		getPage().success(message);
		updateFeedbackPanel(target);
		updateDataList(target);
		// update the delete button
		dataGroup.updateModel();
		updateDeleteSelectedButton(target);
	}

	/**
	 *
	 * @param modal
	 * @param target
	 */
	private void closeDeleteConfirmationModal(final Modal modal,
			AjaxRequestTarget target) {
		// close
		modal.appendCloseDialogJavaScript(target);
	}

	/**
	 *
	 * @param target
	 */
	private void updateDeleteSelectedButton(AjaxRequestTarget target) {
		target.add(deleteSelectedButton);
	}

	/**
	 *
	 * @param target
	 */
	private void updateDataList(AjaxRequestTarget target) {
		target.add(dataContainer);
	}

	/**
	 *
	 * @param target
	 */
	private void updateFeedbackPanel(AjaxRequestTarget target) {
		target.add(feedbackPanel);
	}

	/**
	 *
	 * @param Product
	 * @return
	 */
	private static PageParameters forData(MetricMeasurement measurement) {
		return new PageParameters().set("id", measurement.getId());
	}

	private final class DataProvider extends EntityProvider<MetricMeasurement> {

		private AdapterSettings adapter;

		public DataProvider(Long adapterID) {
			adapter = adapterService.getById(adapterID);
		}

		@Override
		public Iterator<? extends MetricMeasurement> iterator(long first, long count) {
			return dataService.getAllByAdapter(
					Long.valueOf(first).intValue(),
					Long.valueOf(count).intValue(), 
					adapter).iterator();
		}

		@Override
		public long size() {
			return dataService.countAllByAdapter(adapter);
		}
	}
}


