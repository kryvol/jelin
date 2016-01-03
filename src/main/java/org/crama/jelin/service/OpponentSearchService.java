package org.crama.jelin.service;

import org.crama.jelin.model.Game;
import org.crama.jelin.model.GameBot;
import org.crama.jelin.model.User;

public interface OpponentSearchService {
	
	User findOpponent(Game game);
	
	GameBot getBot(Game game);
}
