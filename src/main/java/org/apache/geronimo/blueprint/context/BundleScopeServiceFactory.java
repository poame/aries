/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.blueprint.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.xbean.recipe.ObjectGraph;
import org.apache.xbean.recipe.ObjectRecipe;
import org.apache.xbean.recipe.Recipe;
import org.apache.xbean.recipe.Repository;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/** 
 * TODO: javadoc
 */
public class BundleScopeServiceFactory implements ServiceFactory {

    private ModuleContextImpl moduleContext;
    private Recipe serviceRecipe;
    private Map<Bundle, Entry> instanceMap = Collections.synchronizedMap(new HashMap<Bundle, Entry>()); 
    
    public BundleScopeServiceFactory(ModuleContextImpl moduleContext, Recipe serviceRecipe) {
        this.moduleContext = moduleContext;
        this.serviceRecipe = serviceRecipe;
    }
    
    public Object getService(Bundle bundle, ServiceRegistration registration) {
        Entry entry;
        synchronized(bundle) {        
            entry = instanceMap.get(bundle);
            if (entry == null) {
                entry = new Entry(createInstance());
                System.out.println("Created service instance for bundle: " + bundle + " " + entry.getServiceInstance().hashCode());
                instanceMap.put(bundle, entry);                
            }       
            entry.addServiceRegistration(registration);
        }
        return entry.getServiceInstance();
    }

    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        synchronized(bundle) {        
            Entry entry = instanceMap.get(bundle);
            if (entry != null) {
                entry.removeServiceRegistration(registration);
                if (!entry.hasServiceRegistrations()) {
                    destroyInstance(entry.getServiceInstance());
                    System.out.println("Destroyed service instance for bundle: " + bundle);
                    instanceMap.remove(bundle);
                }
            }
        }
    }
  
    private Object createInstance() {
        Repository objectRepository = moduleContext.getObjectGraph().getRepository();
        ScopedRepository repository = new ScopedRepository((ScopedRepository)objectRepository);
        repository.set(serviceRecipe.getName(), serviceRecipe);
        ObjectGraph graph = new ObjectGraph(repository);
        return graph.create(serviceRecipe.getName());
    }
    
    private void destroyInstance(Object instance) {
        // TODO: call destroy method if any
    }
    
    protected Class getServiceClass() {
        return ((ObjectRecipe) serviceRecipe).getType();
    }
    
    private static class Entry {
        Object serviceInstance;
        Set<ServiceRegistration> registrations = new HashSet<ServiceRegistration>();
        
        public Entry(Object serviceInstance) {
            this.serviceInstance = serviceInstance;
        }
        
        public Object getServiceInstance() {
            return this.serviceInstance;
        }
        
        public boolean hasServiceRegistrations() {
            return !registrations.isEmpty();
        }
        
        public void addServiceRegistration(ServiceRegistration registration) {
            registrations.add(registration);
        }
        
        public void removeServiceRegistration(ServiceRegistration registration) {
            registrations.remove(registration);
        }
        
    }
    
}
