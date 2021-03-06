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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesFactory;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.PodSchema;
import io.fabric8.kubernetes.api.model.ReplicationControllerSchema;
import io.fabric8.kubernetes.api.model.ServiceSchema;
import io.fabric8.utils.JMXUtils;
import io.fabric8.utils.Objects;
import io.fabric8.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides an App view of all the services, controllers, pods
 */
public class AppView implements AppViewMXBean {
    private static final transient Logger LOG = LoggerFactory.getLogger(AppView.class);

    public static ObjectName OBJECT_NAME;
    public static ObjectName KUBERNETES_OBJECT_NAME;

    private final AtomicReference<AppViewSnapshot> snapshotCache = new AtomicReference<>();
    private long pollPeriod = 2000;
    private Timer timer = new Timer();
    private MBeanServer mbeanServer;

    static {
        try {
            OBJECT_NAME = new ObjectName("io.fabric8:type=AppView");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
        try {
            KUBERNETES_OBJECT_NAME = new ObjectName("io.fabric8:type=Kubernetes");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
    }

    private KubernetesClient kubernetes = new KubernetesClient();
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            refreshData();
        }
    };

    public void init() {
        if (pollPeriod > 0) {
            timer.schedule(task, pollPeriod);

            JMXUtils.registerMBean(this, OBJECT_NAME);
        }
    }

    public void destroy() {
        if (timer != null) {
            timer.cancel();
        }
        JMXUtils.unregisterMBean(OBJECT_NAME);
    }


    public long getPollPeriod() {
        return pollPeriod;
    }

    public void setPollPeriod(long pollPeriod) {
        this.pollPeriod = pollPeriod;
    }

    public AppViewSnapshot getSnapshot() {
        return snapshotCache.get();
    }

    public List<AppSummaryDTO> getAppSummaries() {
        List<AppSummaryDTO> answer = new ArrayList<>();
        AppViewSnapshot snapshot = getSnapshot();
        if (snapshot != null) {
            Collection<AppViewDetails> apps = snapshot.getApps().values();
            for (AppViewDetails app : apps) {
                AppSummaryDTO summary = app.getSummary();
                if (summary != null) {
                    answer.add(summary);
                }
            }
        }
        return answer;
    }

    @Override
    public String findAppSummariesJson() throws JsonProcessingException {
        return KubernetesHelper.toJson(getAppSummaries());
    }

    protected void refreshData() {
        try {
            AppViewSnapshot snapshot = createSnapshot();
            if (snapshot != null) {
                snapshotCache.set(snapshot);
            }
        } catch (Exception e) {
            LOG.warn("Failed to create snapshot: " + e, e);
        }
    }

    public AppViewSnapshot createSnapshot() {
        Map<String, ServiceSchema> servicesMap = KubernetesHelper.getServiceMap(kubernetes);
        Map<String, ReplicationControllerSchema> controllerMap = KubernetesHelper.getReplicationControllerMap(kubernetes);
        Map<String, PodSchema> podMap = KubernetesHelper.getPodMap(kubernetes);

        AppViewSnapshot snapshot = new AppViewSnapshot(servicesMap, controllerMap, podMap);
        for (ServiceSchema service : servicesMap.values()) {
            String appPath = getAppPath(service.getId());
            if (appPath != null) {
                AppViewDetails dto = snapshot.getOrCreateAppView(appPath);
                dto.addService(service);
            }
        }
        for (ReplicationControllerSchema controller : controllerMap.values()) {
            String appPath = getAppPath(controller.getId());
            if (appPath != null) {
                AppViewDetails dto = snapshot.getOrCreateAppView(appPath);
                dto.addController(controller);
            }
        }
        return snapshot;
    }

    /**
     * Returns the App path for the given kubernetes service or controller id or null if it cannot be found
     */
    protected String getAppPath(String serviceId) {
        if (Strings.isNullOrBlank(serviceId)) {
            return null;
        }
        MBeanServer beanServer = getMBeanServer();
        Objects.notNull(beanServer, "MBeanServer");
        if (!beanServer.isRegistered(KUBERNETES_OBJECT_NAME)) {
            LOG.warn("No MBean is available for: " + KUBERNETES_OBJECT_NAME);
            return null;
        }
        String branch = "master";
        Object[] params = {
                branch,
                serviceId
        };
        String[] signature = {
                String.class.getName(),
                String.class.getName()
        };
        if (LOG.isDebugEnabled()) {
            LOG.debug("About to invoke " + KUBERNETES_OBJECT_NAME + " appPath" + Arrays.asList(params) + " signature" + Arrays.asList(signature));
        }
        try {
            Object answer = beanServer.invoke(KUBERNETES_OBJECT_NAME, "appPath", params, signature);
            if (answer != null) {
                return answer.toString();
            }
        } catch (Exception e) {
            LOG.warn("Failed to invoke " + KUBERNETES_OBJECT_NAME + " appPath" + Arrays.asList(params) + ". " + e, e);
        }
        return null;
    }

    public MBeanServer getMBeanServer() {
        if (mbeanServer == null) {
            mbeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        return mbeanServer;
    }

    public void setMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }
}
