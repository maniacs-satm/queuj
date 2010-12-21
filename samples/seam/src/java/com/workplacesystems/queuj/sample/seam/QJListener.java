/*
 * Copyright 2010 Workplace Systems PLC (http://www.workplacesystems.com/).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.workplacesystems.queuj.sample.seam;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.xml.DOMConfigurator;

/**
 *
 * @author Dave Oxley <dave@workplacesystems.co.uk>
 */
public class QJListener implements ServletContextListener {

    private final static Log log = LogFactory.getLog(QJListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String log4jXml = sce.getServletContext().getRealPath("/WEB-INF/log4j.xml");
        DOMConfigurator.configureAndWatch(log4jXml);

        log.info("Starting queujSeamSample");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Stopping queujSeamSample");
    }
}
