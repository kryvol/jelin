package org.crama.jelin.controller;

import org.crama.jelin.model.Settings;
import org.crama.jelin.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SettingsController {
	
	@Autowired
	private SettingsService settingsService;
	
	@RequestMapping(value="/api/settings", method=RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
    public Settings getSettings() {
		
		return settingsService.getSettings();
		
	}
}
