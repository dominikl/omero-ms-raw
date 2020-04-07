/*
 * Copyright (C) 2018 University of Dundee & Open Microscopy Environment.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.openmicroscopy;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class RawDataService {

    /**
     * @param argv filename(s) from which to read configuration beyond current Java system properties
     * @throws IOException if the configuration could not be loaded
     */
    public static void main(String[] argv) throws IOException {
        /* set system properties from named configuration files */
        final Properties propertiesSystem = System.getProperties();
        for (final String filename : argv) {
            final Properties propertiesNew = new Properties();
            try (final InputStream filestream = new FileInputStream(filename)) {
                propertiesNew.load(filestream);
            }
            propertiesSystem.putAll(propertiesNew);
        }
        /* start up OMERO.server's data layer and obtain its dataSource bean for JDBC */
        final AbstractApplicationContext context = new ClassPathXmlApplicationContext("context.xml");
        final ApplicationContext omeroContext = context.getBean("data", ApplicationContext.class);
        final DataSource dataSource = omeroContext.getBean("nonXaDataSource", DataSource.class);
        /* deploy the verticle which uses OMERO's data source */
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new RawDataVerticle(dataSource), (AsyncResult<String> result) -> {
            context.close();
        });
    }
}
