package eu.uqasar.service.meta;

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


import java.util.List;

import javax.ejb.Stateless;

import eu.uqasar.model.meta.SoftwareType;
import eu.uqasar.model.qmtree.QMBaseIndicator_;
import eu.uqasar.model.tree.Project;
import eu.uqasar.model.tree.Project_;
import eu.uqasar.model.user.User;
import eu.uqasar.model.user.User_;

/**
 *
 *
 */
@Stateless
public class SoftwareTypeService extends MetaDataService<SoftwareType> {

    public SoftwareTypeService() {
        super(SoftwareType.class);
    }

    @Override
    public boolean isInUse(SoftwareType entity) {
        long usersWithTool = countUsersWithMetaData(entity, User_.softwareTypes);
        long projectwithTool = countprojectsWithMetaData(entity, Project_.softwareTypes);
        long qowithTool = countQOWithMetaData(entity, QMBaseIndicator_.qModelTagData);
        long qiwithTool = countQIWithMetaData(entity, QMBaseIndicator_.qModelTagData);
        long qmetricwithTool = countQMetricWithMetaData(entity, QMBaseIndicator_.qModelTagData);
        return (usersWithTool > 0 || projectwithTool >0 ||  qowithTool>0 || qiwithTool>0 || qmetricwithTool>0);
    }

    @Override
    public void delete(SoftwareType entity) {
        removeFromUserSkills(entity);
        removefromQM(entity);
        removeFromProject(entity);
        super.delete(entity);
    }

    private void removeFromUserSkills(SoftwareType entity) {
        List<User> users = getUsersWithMetaData(entity, User_.softwareTypes);
        for (User user : users) {
            user.getSoftwareTypes().remove(entity);
            em.merge(user);
        }
    }
    
    private void removeFromProject(SoftwareType entity) {
        List<Project> projects = getProjectsWithMetaData(entity, Project_.softwareTypes);
        for (Project project : projects) {
        	project.getSoftwareTypes().remove(entity);
            em.merge(project);
        }
    }
}
