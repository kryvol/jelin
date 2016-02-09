package org.crama.jelin.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.crama.jelin.exception.GameException;
import org.crama.jelin.model.Constants.NetStatus;
import org.crama.jelin.model.Constants.Readiness;
import org.crama.jelin.model.Constants.UserType;
import org.crama.jelin.model.Game;
import org.crama.jelin.model.GameRound;
import org.crama.jelin.model.Question;
import org.crama.jelin.model.User;
import org.crama.jelin.repository.AnswerRepository;
import org.crama.jelin.repository.GameRepository;
import org.crama.jelin.repository.QuestionResultRepository;
import org.crama.jelin.repository.UserRepository;
import org.crama.jelin.repository.impl.UserRepositoryImpl;
import org.crama.jelin.service.GameService;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OfflinePlayerWorker implements Runnable {

	private Game game;
	private Readiness condition;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private GameService gameService;
	
	@Autowired 
	private GameRepository gameRepository;
			
	public void setUp(Game game, Readiness condition)
	{
		this.game = game;
		this.condition = condition;
		Hibernate.initialize(this.game);
	}
	
	@Override
	public void run() {
		if (gameRepository.getReadiness(game) == condition)
		{
			try 
			{
				List<User> offlineHumans = getFreshOfflineUsers();
				initialOfflineUpdate(condition, offlineHumans);
				
			} catch (GameException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
	
	private List<User> getFreshOfflineUsers()
	{
		Hibernate.initialize(game);
		List<User> offlineUsers = new ArrayList<User>();
		List<User> users = null;
		
		
		switch(condition)
		{
			case CATEGORY:	User host = game.getRound().getHost();
							System.out.println("[CATEGORY] User " + host.getUsername() + " is host and he is offline. Adding him to offline users list.");
							offlineUsers.add(host); 
						    break;
			case QUESTION:	users = userRepository.getPlayersNotWithReadiness(Readiness.QUESTION, game);
							offlineUsers.addAll(users);
			
							System.out.println("[QUESTION] Offline users: ");
							System.out.println(offlineUsers.toString());
							
							break;
			case ANSWER:	users = userRepository.getPlayersNotWithReadiness(Readiness.ANSWER, game);
							offlineUsers.addAll(users);
			
							System.out.println("[QUESTION] Offline users: ");
							System.out.println(offlineUsers.toString());
				
							break;
			case RESULT:	users = userRepository.getPlayersNotWithReadiness(Readiness.RESULT, game);
							offlineUsers.addAll(users);
			
							System.out.println("[QUESTION] Offline users: ");
							System.out.println(offlineUsers.toString());
				
							break;
			case SUMMARY:   break;
			default: break;
		}
		
		return offlineUsers;
		
	}
	
	private void initialOfflineUpdate(Readiness condition, List<User> offlineUsers) throws GameException
	{
		Hibernate.initialize(game);
		switch(condition)
		{
			case CATEGORY:	User host = game.getRound().getHost();
							host.setNetStatus(NetStatus.OFFLINE);
							userRepository.updateNetStatus(host, NetStatus.OFFLINE);
							gameService.setRandomCategory(game);
							System.out.println("[CATEGORY] Set up random round because the host " + host.getUsername()+ " is offline.");
							break;
			case QUESTION:	for (User player: offlineUsers)
							{
								player.setNetStatus(NetStatus.OFFLINE);
								userRepository.updateNetStatus(player, NetStatus.OFFLINE);
								gameService.updateGame(game);
								
								gameService.processQuestion(game, player);
								System.out.println("[QUESTION] Processing next question for user " + player.getUsername()+ ". He is offline.");								
							}
							break;
			case ANSWER:	for (User player: offlineUsers)
							{
								player.setNetStatus(NetStatus.OFFLINE);
								userRepository.updateNetStatus(player, NetStatus.OFFLINE);
								gameService.updateGame(game);
								
								gameService.processAnswer(game, player, -1, 0);
								System.out.println("[ANSWER] Processing answer for user " + player.getUsername()+ ". He is offline.");
								
							}
			
							break;
			case RESULT:	for (User player: offlineUsers)
							{
								player.setNetStatus(NetStatus.OFFLINE);
								userRepository.updateNetStatus(player, NetStatus.OFFLINE);
								gameService.updateGame(game);
								
								gameService.processResult(game, player);									
								System.out.println("[RESULT] Processing result for user " + player.getUsername()+ ". He is offline.");
							}
							break;
			case SUMMARY:	gameService.finishGame(game);
							break;
			default: break;
			
		}
		
		gameService.updateGame(game);
	}
	
}
