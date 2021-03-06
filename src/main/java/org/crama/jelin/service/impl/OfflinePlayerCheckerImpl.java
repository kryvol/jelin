package org.crama.jelin.service.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

import org.crama.jelin.model.Constants;
import org.crama.jelin.model.Constants.Readiness;
import org.crama.jelin.model.Game;
import org.crama.jelin.model.Settings;
import org.crama.jelin.repository.GameRepository;
import org.crama.jelin.repository.UserRepository;
import org.crama.jelin.service.OfflinePlayerChecker;
import org.crama.jelin.service.SettingsService;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("offlinePlayerChecker")
public class OfflinePlayerCheckerImpl implements OfflinePlayerChecker {

	private static final Logger logger = LoggerFactory.getLogger(OfflinePlayerCheckerImpl.class);
	
	private static int countdownDelay = 5;
	private static int countdownPeriod = 3;
	
	@Autowired
	private SettingsService settingsService;
	
	@Autowired
	private OfflinePlayerWorker offlinePlayerWorker;
	
	@Autowired 
	private GameRepository gameRepository;
	
	@Override
	public void setUpTimeout(Game game, Readiness condition) {
		Settings settings = settingsService.getSettings();
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		offlinePlayerWorker.setUp(game, condition);
		
		ScheduledFuture<?> offlineWorker =
	    		scheduler.schedule(offlinePlayerWorker, settings.getOfflineTimeout(condition),
	    				TimeUnit.SECONDS);  
		
		
		Runnable countDownRunnable = new Runnable()
		{
			@Override
			public void run()
			{
				if (!offlineWorker.isDone()) 
				{   
					if (gameRepository.getReadiness(game) != condition)
		            {
		              	offlineWorker.cancel(false);
		              	logger.debug("[Game " + game.getId() + "]: All users who were online before are online now! Canceled offlineWorker task. Shutdowning scheduler...");
		               	scheduler.shutdown();	
		            }
					else
					{
						logger.debug("[Game " + game.getId() + "] Readiness is still " + condition.toString() +". Will check again in " + countdownPeriod + " seconds.");
					}
				}
				else
				{
					logger.debug("[Game " + game.getId() + "] Offline users processed. Shutdowning scheduler...");					
					scheduler.shutdown();
					
				}
				
			}
		};
    
	    ScheduledFuture<?> countdown =
	    		scheduler.scheduleAtFixedRate(countDownRunnable, countdownDelay, countdownPeriod, TimeUnit.SECONDS); 
	    
   
	    
	}

}
