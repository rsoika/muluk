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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.muluk.xml.XMLConfig;
import org.imixs.muluk.xml.XMLObject;

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

	@Resource
	javax.ejb.TimerService timerService;

	@Inject
	@ConfigProperty(name = "muluk.config.file", defaultValue = "config.xml")
	String configFile;

	XMLConfig config;

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
			config = readConfig(bytes);
		} catch (IOException | JAXBException e) {
			logger.severe("Failed to read config file: " + e.getMessage());
		}

		// cancel all timers...
		for (Object obj : timerService.getTimers()) {
			logger.warning("... cancel existing timer - should not happen!");
			Timer timer = (javax.ejb.Timer) obj;
			if (timer != null) {
				XMLObject object = (XMLObject) timer.getInfo();
				logger.info("......cancel  timer - " + object.getTarget());
				timer.cancel();
			}
		}
		// Finally start optional schedulers
		if (config != null) {

			logger.info("......initalizing jobs...");
			startAllJobs();
		}

	}

	private void startAllJobs() {
		// TODO Auto-generated method stub

		XMLObject[] allObjects = config.getMonitor().getObject();
		if (allObjects != null) {
			for (XMLObject obj : allObjects) {
				try {
					startJob(obj);
				} catch (AccessDeniedException | ParseException e) {
					logger.severe("Failed to start job: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
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

	/**
	 * Starts a timerService for a given Object
	 * 
	 * @param configuration
	 * @return
	 * @throws AccessDeniedException
	 * @throws ParseException
	 */
	public void startJob(XMLObject object) throws AccessDeniedException, ParseException {
		Timer timer = null;
		if (object == null)
			return;

		logger.info("......starting new job - " + object.getTarget() + " ...");
		timer = createTimerOnCalendar(object);
		if (timer == null) {
			logger.warning("...failed to start new job!");
		} 

	}

	/**
	 * Create a non-persistent calendar-based timer based on a Job Object.
	 * 
	 * 
	 * Example: <code>
	 *   second=0
	 *   minute=0
	 *   hour=*
	 *   dayOfWeek=
	 *   dayOfMonth=
	 *   month=
	 *   year=*
	 * </code>
	 * 
	 * @param object
	 * @return
	 * @throws ParseException
	 */
	protected Timer createTimerOnCalendar(XMLObject object) throws ParseException {

		TimerConfig timerConfig = new TimerConfig();
		timerConfig.setInfo(object);
		timerConfig.setPersistent(false);
		ScheduleExpression scheduerExpression = new ScheduleExpression();

		logger.info("......creating new timer - scheduler = " + object.getScheduler());
		String[] calendarConfiguationList = object.getScheduler().split(";");
		// try to parse the configuration list....
		for (String confgEntry : calendarConfiguationList) {

			if (confgEntry.startsWith("second=")) {
				scheduerExpression.second(confgEntry.substring(confgEntry.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("minute=")) {
				scheduerExpression.minute(confgEntry.substring(confgEntry.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("hour=")) {
				scheduerExpression.hour(confgEntry.substring(confgEntry.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("dayOfWeek=")) {
				scheduerExpression.dayOfWeek(confgEntry.substring(confgEntry.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("dayOfMonth=")) {
				scheduerExpression.dayOfMonth(confgEntry.substring(confgEntry.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("month=")) {
				scheduerExpression.month(confgEntry.substring(confgEntry.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("year=")) {
				scheduerExpression.year(confgEntry.substring(confgEntry.indexOf('=') + 1));
			}
			if (confgEntry.startsWith("timezone=")) {
				scheduerExpression.timezone(confgEntry.substring(confgEntry.indexOf('=') + 1));
			}

			/* Start date */
			if (confgEntry.startsWith("start=")) {
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
				Date convertedDate = dateFormat.parse(confgEntry.substring(confgEntry.indexOf('=') + 1));
				scheduerExpression.start(convertedDate);
			}

			/* End date */
			if (confgEntry.startsWith("end=")) {
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
				Date convertedDate = dateFormat.parse(confgEntry.substring(confgEntry.indexOf('=') + 1));
				scheduerExpression.end(convertedDate);
			}

		}

		Timer timer = timerService.createCalendarTimer(scheduerExpression, timerConfig);

		return timer;

	}

	/**
	 * This is the method which processes the timeout event depending on the running
	 * timer settings. The method calls the abstract method 'process' which need to
	 * be implemented by a subclass.
	 * 
	 * @param timer
	 * @throws Exception
	 * @throws QueryException
	 */
	@Timeout
	protected void onTimeout(javax.ejb.Timer timer) {
		
		long lProfiler = System.currentTimeMillis();
		XMLObject object = (XMLObject) timer.getInfo();

		if (object == null) {
			logger.severe("...failed to load scheduler configuration for current timer. Timer will be stopped...");
			timer.cancel();
			return;
		}
		logger.info("......executing job - " + object.getTarget());

	}
}
