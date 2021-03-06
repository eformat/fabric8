/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api.mbeans;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a snapshot of the entire system and how they relate to apps
 */
public class AppViewSnapshot {
    private final Map<String, AppViewDetails> apps = new HashMap<>();
    private final Map<String, ServiceSchema> servicesMap;
    private final Map<String, ReplicationControllerSchema> controllerMap;
    private final Map<String, PodSchema> podMap;

    public AppViewSnapshot(Map<String, ServiceSchema> servicesMap, Map<String, ReplicationControllerSchema> controllerMap, Map<String, PodSchema> podMap) {
        this.servicesMap = servicesMap;
        this.controllerMap = controllerMap;
        this.podMap = podMap;
    }


    public AppViewDetails getOrCreateAppView(String appPath) {
        AppViewDetails answer = apps.get(appPath);
        if (answer == null) {
            answer = new AppViewDetails(this, appPath);
            apps.put(appPath, answer);
        }
        return answer;
    }

    public Map<String, AppViewDetails> getApps() {
        return apps;
    }

    public Map<String, ReplicationControllerSchema> getControllerMap() {
        return controllerMap;
    }

    public Map<String, PodSchema> getPodMap() {
        return podMap;
    }

    public Map<String, ServiceSchema> getServicesMap() {
        return servicesMap;
    }

    public List<PodSchema> podsForReplicationController(ReplicationControllerSchema controller) {
        return KubernetesHelper.getPodsForReplicationController(controller, podMap.values());
    }
}