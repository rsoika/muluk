/*  
 *  Imixs-Workflow 
 *  
 *  Copyright (C) 2001-2020 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *      https://www.imixs.org
 *      https://github.com/imixs/imixs-workflow
 *  
 *  Contributors:  
 *      Imixs Software Solutions GmbH - Project Management
 *      Ralph Soika - Software Developer
 */

package org.imixs.muluk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.muluk.xml.XMLConfig;

/**
 * 
 * @author rsoika
 * @version 1.0
 */
@Startup
@Singleton
public class MonitorService {
	public static String SETUP_OK = "OK";
	public static String MODEL_INITIALIZED = "MODEL_INITIALIZED";

	private static Logger logger = Logger.getLogger(MonitorService.class.getName());

	Date started = null;

	@Inject
	@ConfigProperty(name = "muluk.config.file", defaultValue = "config.xml")
	String configFile;

	XMLConfig readConfig;

	/**
	 * This method start the system setup during deployment
	 * 
	 * @throws AccessDeniedException
	 */
	@PostConstruct
	public void startup() {

		started = new Date(System.currentTimeMillis());

		// created with linux figlet
		logger.info(" __  __       _       _    ");
		logger.info("|  \\/  |_   _| |_   _| | __");
		logger.info("| |\\/| | | | | | | | | |/ /");
		logger.info("| |  | | |_| | | |_| |   < ");
		logger.info("|_|  |_|\\__,_|_|\\__,_|_|\\_\\   V0.0.1");

		// load Config from file
		logger.info("......read configuration...");
		try {
			byte[] bytes = Files.readAllBytes(Paths.get(configFile));
			readConfig = readConfig(bytes);
		} catch (IOException | JAXBException e) {
			logger.severe("Failed to read config file: " + e.getMessage());
		}

		// Finally start optional schedulers
		if (readConfig!=null) {
			logger.info("......initalizing jobs...");
			startAllJobs();
		}

	}

	private void startAllJobs() {
		// TODO Auto-generated method stub

	}

	public Date getStarted() {
		return started;
	}

	public static XMLConfig readConfig(byte[] byteInput) throws JAXBException, IOException {

		if (byteInput == null || byteInput.length == 0) {
			return null;
		}

		XMLConfig ecol = null;

		JAXBContext context = JAXBContext.newInstance(XMLConfig.class);
		Unmarshaller m = context.createUnmarshaller();

		ByteArrayInputStream input = new ByteArrayInputStream(byteInput);
		Object jaxbObject = m.unmarshal(input);
		if (jaxbObject == null) {
			throw new RuntimeException("readCollection error - wrong xml file format - unable to read content!");
		}

		ecol = (XMLConfig) jaxbObject;

		return ecol;

	}

}
