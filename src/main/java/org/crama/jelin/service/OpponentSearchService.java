package org.crama.jelin.service;

import org.crama.jelin.exception.GameException;
import org.crama.jelin.model.Game;
import org.crama.jelin.model.User;

public interface OpponentSearchService {
	
	User findOpponent(Game game);
	
	User createBot(Game game) throws GameException;
}
