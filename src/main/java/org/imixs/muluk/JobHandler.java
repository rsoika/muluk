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

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;

import org.imixs.muluk.web.WebClient;
import org.imixs.muluk.xml.XMLConfig;
import org.imixs.muluk.xml.XMLObject;

/**
 * The Job Handler starts and executes the monitor jobs.
 * 
 * @author rsoika
 * @version 1.0
 */
@Startup
@Singleton
public class JobHandler {
	public static int DEFAULT_INTERVAL = 60;
	public static int INITIAL_DELAY = 10000;

	private static Logger logger = Logger.getLogger(JobHandler.class.getName());

	@Resource
	javax.ejb.TimerService timerService;
	
	private XMLConfig config;

	/**
	 * This method starts all jobs defined in the current monitor configuration.
	 */
	public void startAllJobs(XMLConfig config) {
		this.config=config;
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

	/**
	 * Create a non-persistent calendar-based timer based on a Job Object.
	 * 
	 * 
	 * @param configuration
	 * @return
	 * @throws AccessDeniedException
	 * @throws ParseException
	 */
	protected void startJob(XMLObject object) throws AccessDeniedException, ParseException {
		Timer timer = null;
		if (object == null)
			return;

		logger.info("......starting new job (⚙ " + object.getInterval() + " sec ▸ '" + object.getTarget() + "') ...");
		// create a new non persistent timer object
		TimerConfig timerConfig = new TimerConfig();
		timerConfig.setInfo(object);
		timerConfig.setPersistent(false);

		long interval = object.getInterval();
		if (interval <= 0) {
			// default interval = 60 seconds
			interval = DEFAULT_INTERVAL;
		}
		// intial delay 10 seconds
		timer = timerService.createIntervalTimer(INITIAL_DELAY, interval * 1000, timerConfig);
		if (timer == null) {
			logger.warning("...failed to start new job!");
		}

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
			logger.severe("...invalid object configuration! Timer will be stopped...");
			timer.cancel();
			return;
		}
		
		if ( object.getPattern() == null || object.getPattern().isEmpty()) {
			logger.severe("...invalid object configuration - missing tag 'pattern'...");
			timer.cancel();
			return;
		}
		
		
		logger.info("......executing job - " + object.getTarget());

		String target = object.getTarget();
		if (target.toLowerCase().startsWith("http")) {
			try {
				WebClient webClient = new WebClient();
				String result = webClient.get(target);
				//logger.info(result);

				Pattern p = Pattern.compile(object.getPattern()); // the pattern to search for
				Matcher m = p.matcher(result);

				// now try to find at least one match
				if (m.find()) {
					logger.info("......OK");
					object.setStatus("OK");
					object.setLastSuccess(new Date());
					config.addPing();;
					
				} else {
					logger.info("......FAILED - pattern not found!");
					object.setStatus("FAILED");
					object.setLastFailure(new Date());
					config.addErrors();
				}
			} catch (IOException e) {
				logger.severe("FAILED to request target - " + e.getMessage());
				object.setStatus("FAILED");
				object.setLastFailure(new Date());
				config.addErrors();
			}

		}
	}
}
