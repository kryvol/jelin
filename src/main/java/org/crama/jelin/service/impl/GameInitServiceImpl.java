package org.crama.jelin.service.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.crama.jelin.exception.GameException;
import org.crama.jelin.model.Category;
import org.crama.jelin.model.Constants.GameState;
import org.crama.jelin.model.Constants.InviteStatus;
import org.crama.jelin.model.Constants.NetStatus;
import org.crama.jelin.model.Constants.NotificationType;
import org.crama.jelin.model.Constants.ProcessStatus;
import org.crama.jelin.model.Difficulty;
import org.crama.jelin.model.Game;
import org.crama.jelin.model.GameOpponent;
import org.crama.jelin.model.Settings;
import org.crama.jelin.model.User;
import org.crama.jelin.model.json.UserJson;
import org.crama.jelin.repository.GameInitRepository;
import org.crama.jelin.repository.UserRepository;
import org.crama.jelin.service.GameInitService;
import org.crama.jelin.service.PushNotificationService;
import org.crama.jelin.service.SettingsService;
import org.crama.jelin.service.UserService;
import org.crama.jelin.util.DateConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("gameInitService")
public class GameInitServiceImpl implements GameInitService {

	private static final Logger logger = LoggerFactory.getLogger(GameInitServiceImpl.class);
	
	@Autowired
	private GameInitRepository gameInitRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private SettingsService settingsService;
	
	@Autowired
	private PushNotificationService pushNotificationService;
		
	
	
	@Override
	public boolean initGame(User creator, Category theme, boolean random) {
		
		Game game = new Game(theme, random);
		game.setGameState(GameState.CREATED);
		
		game.setCreator(creator);
		game.setInitDate(new Date());
		
		return gameInitRepository.saveGame(game);
	}
	
	@Override
	public void initGame(User creator, Difficulty difficulty) {
		Game game = new Game(difficulty);
		game.setGameState(GameState.CREATED);
		game.setCreator(creator);
		game.setInitDate(new Date());
		
		gameInitRepository.saveGame(game);
	}


	@Override
	public boolean updateDifficulty(Game game, Difficulty difficulty) {
		game.setDifficulty(difficulty);
		return gameInitRepository.updateGame(game);
	}

	@Override
	public void updateCategory(Game game, Category category, boolean random) {
		game.setTheme(category);
		game.setRandom(random);
		gameInitRepository.updateGame(game);
	}
	
	@Override
	public Game getCreatedGame(User creator) {
		Game game = gameInitRepository.getGame(creator, GameState.CREATED);
		return game;
	}

	@Override
	public void removeOpponent(Game game, int userId) throws GameException {
		// update user status to free
		User user = userRepository.getUser(userId);
		userService.checkUserAuthorized(user);
		user.setProcessStatus(ProcessStatus.FREE);
		userRepository.updateUser(user);
		
		gameInitRepository.removeGameOpponent(game, user);
		
	}

	@Override
	public Set<UserJson> getGameOpponents(Game game) {
		Set<GameOpponent> opponents = game.getGameInvitationOpponents();
		Set<UserJson> acceptedOpponents = new HashSet<UserJson>();
		logger.info("Op: " + opponents + ", " + opponents.size());
		for (GameOpponent o: opponents) {
			if (o.getInviteStatus().equals(InviteStatus.ACCEPTED)) {
				logger.info("Accepted opponent: " + o);
				acceptedOpponents.add(new UserJson(o.getUser()));
			}
		}
		return acceptedOpponents;
	}

	@Override
	public InviteStatus inviteUser(Game game, User creator, User opponent, boolean isRandom) {
		opponent.setProcessStatus(ProcessStatus.INVITING);
		userRepository.updateUser(opponent);
		
		Set<GameOpponent> opponents = game.getGameInvitationOpponents();
		InviteStatus inviteStatus = InviteStatus.OPEN;
		GameOpponent newOpponent = new GameOpponent(opponent, game, inviteStatus);
		
		boolean isNewOpponent = true;
		for (GameOpponent o: opponents) {
			
			 //if (o.getUser().equals(opponent)) {
			 if (o.getUser().getId() == opponent.getId()) {
				 
				 logger.info("User was already invited to the game. Update invitatioon with OPEN status");
				 o.setInviteStatus(inviteStatus);
				 isNewOpponent = false;
				 break;
			 }
		}
		logger.info("Is new opponent: " + isNewOpponent);
		if (isNewOpponent) {
			opponents.add(newOpponent);
			game.setGameInvitationOpponents(opponents);
			
			if (isRandom)
			{
				pushNotificationService.sendNotificationMessage(opponent, NotificationType.ACCEPT_RANDOM, creator, game.getTheme());
			}
			else
			{
				// Send push notification to invited user from the room
				Set<UserJson> opponentsInvited = getGameOpponents(game);
				if (opponentsInvited.size() == 0)
				{
					pushNotificationService.sendNotificationMessage(opponent, NotificationType.ACCEPT_FRIEND, creator, game.getTheme());
				}
				else
				{
					pushNotificationService.sendNotificationMessage(opponent, NotificationType.ACCEPT_FRIENDS, creator, game.getTheme(), opponentsInvited.size());
				}
			}
		}
		gameInitRepository.updateGame(game);
		
		Game updatedGame = null;
		
		Settings settings = settingsService.getSettings();
		
		boolean invitationHandled = false;
		int inviteCheckTimeout = settings.getInviteCheckTimeout();
		int numOfChecks = settings.getInviteTimeout() / inviteCheckTimeout;
		for (int i = 0; i < numOfChecks; i++) {
			// check status every CHECK_TIMEOUT seconds
			try {
				//1000 milliseconds is one second
			    Thread.sleep(1000 * inviteCheckTimeout);                 
			} catch(InterruptedException ex) {
				logger.info("Interrupted Exception");
			    Thread.currentThread().interrupt();
			}
			
			logger.info("Check invitation: " + i);
			
			gameInitRepository.clearSession();
			
			updatedGame = getCreatedGame(creator);
			
			if (!checkInviteStatus(updatedGame)) {
				logger.info("Invitation handled. Return from the method");
				invitationHandled = true;
				
				GameOpponent go = gameInitRepository.getGameOpponent(updatedGame, opponent);
				return go.getInviteStatus(); 
				
			}
		}
		
		
		
		//after TIMEOUT
		if (!invitationHandled) {
			logger.info("Invitation expired!");
			InviteStatus statusExpired = InviteStatus.EXPIRED;
			 			 			 
			 gameInitRepository.clearSession();
			 updatedGame = getCreatedGame(creator);
			 
			 		
			 int missedGames = opponent.getUserStatistics().getExpiredInvites() + 1; 
			 opponent.getUserStatistics().setExpiredInvites(missedGames);
			 			 
			 pushNotificationService.sendNotificationMessage(opponent, NotificationType.MISSED_GAMES, missedGames);
						 
			 for (GameOpponent o: updatedGame.getGameInvitationOpponents()) {
				 if (o.getUser().getId() == opponent.getId()) {
					 
					 logger.info("Update status for user: " + o.getUser().getUsername() + " " + statusExpired);
					 o.setInviteStatus(statusExpired);
					 
					 gameInitRepository.updateGame(updatedGame);
					 
					 User op = o.getUser();
					 
					 op.setProcessStatus(ProcessStatus.FREE);
					 userRepository.updateUser(op);
					 
					 return statusExpired;
					 
				 }
			}
		}
		
		return null;
		
	}

	@Override
	public Game getInviteGame(User user) {
		
		return gameInitRepository.getInviteGame(user, GameState.CREATED);
	}

	@Override
	public Game getInviteInProgressGame(User user) {
		
		return gameInitRepository.getInviteGame(user, GameState.IN_PROGRESS);
	}
	
	@Override
	public void confirmInvite(Game game, User user) {
	
		boolean is2Free = true;
		boolean is3Free = true;
		boolean is4Free = true;
		
		InviteStatus statusAccepted = InviteStatus.ACCEPTED;
		Set<GameOpponent> opponents = game.getGameInvitationOpponents();
		
		//check free player numbers
		for (GameOpponent o: opponents) {
			if (o.getInviteStatus().equals(InviteStatus.ACCEPTED)) {
				int playerNum = o.getPlayerNum();
				if (playerNum == 2) {
					is2Free = false;
				}
				else if (playerNum == 3) {
					is3Free = false;
				}
				else if (playerNum == 4) {
					is4Free = false;
				}
			}
		}
		
		 for (GameOpponent o: opponents) {
			 
			 logger.info("Confirm invite: " + o.getUser().getUsername() + ", " + o.getInviteStatus());
			 
			 if (o.getUser().getId() == user.getId()) {
			 //if (o.getUser().equals(user)) {
				 
				 //accept invitation
				 o.setInviteStatus(statusAccepted);
				 if (is2Free) {
					 o.setPlayerNum(2);
				 }
				 else if (is3Free) {
					 o.setPlayerNum(3);
				 }
				 else if (is4Free) {
					 o.setPlayerNum(4);
				 }
				 gameInitRepository.updateGame(game);
				 
				 user.setProcessStatus(ProcessStatus.WAITING);
				 userRepository.updateUser(user);
				 
				 break;
				 
			 }
		}
	}

	@Override
	public void refuseInvite(Game game, User user) {
		InviteStatus statusRejected = InviteStatus.REJECTED;
		Set<GameOpponent> opponents = game.getGameInvitationOpponents();
		 
		 for (GameOpponent o: opponents) {
			 if (o.getUser().equals(user)) {
				 
				 //reject invitation
				 o.setInviteStatus(statusRejected);
				 gameInitRepository.updateGame(game);
				 
				 user.setProcessStatus(ProcessStatus.FREE);
				 userRepository.updateUser(user);
				 
				 break;
				 
			 }
		}
	}

	@Override
	public boolean checkInviteStatus(Game game) {
		Set<GameOpponent> opponents = game.getGameInvitationOpponents();
		for (GameOpponent o: opponents) {
			if (o.getInviteStatus().equals(InviteStatus.OPEN)) {
				//there is open invite status
				logger.info("OPEN invitation: " + o.getUser().getUsername() + ", " + o.getInviteStatus());
				return true;
			}
		}
		
		return false;
	}

	@Override
	public void checkGameCreated(Game game) throws GameException {
		if (game == null) {
			throw new GameException(401, "Game is not created");
		}
	}

	@Override
	public void checkGameRandom(Game game) throws GameException {
		if (game.getRandom()) {
			throw new GameException(402, "Game is random");
		}
	}

	@Override
	public void checkNumOfOpponents(Game game) throws GameException {
		if (getGameOpponents(game).size() >=3) {
			throw new GameException(701, "There are already 3 opponents that have accepted game offer. Can't invite more opponents");
		}
	}

	@Override
	public void closeGame(Game game) {
		game.setGameState(GameState.ENDED);
		game.getCreator().setProcessStatus(ProcessStatus.FREE);
		
		gameInitRepository.updateGame(game);
		Set<GameOpponent> opponents = game.getGameInvitationOpponents();
		for (GameOpponent go: opponents) {
			User o = go.getUser();
			o.setProcessStatus(ProcessStatus.FREE);
			userRepository.updateUser(o);
		}
	}

	@Override
	public void addGameOpponent(Game game, User user) {
		GameOpponent opponent = new GameOpponent(user, game, InviteStatus.ACCEPTED);
		game.addGameInvitationOpponent(opponent);
		gameInitRepository.updateGame(game);
		
	}

	@Override
	public Game getGame(User creator, GameState state) {
		if (state == null) {
			Game game = gameInitRepository.getGame(creator, GameState.CREATED);
			if (game == null) {
				game = gameInitRepository.getGame(creator, GameState.IN_PROGRESS);
			}
			return game;
		}
		else {
			return gameInitRepository.getGame(creator, state);
		}
	}

	@Override
	public boolean checkLastRejectTime(User opponent) {
		
		InviteStatus lastResponse = opponent.getUserActivity().getLastInviteResponse();
		if (lastResponse == null)
		{
			return true;
		}
		
		if (lastResponse.equals(InviteStatus.ACCEPTED)) {
			return true;
		}
		
		Date lastInviteDate = opponent.getUserActivity().getLastInvite();
		
		if (lastInviteDate == null) {
			return true;
		}
		
		LocalDateTime lastInviteLocalTime = DateConverter.toLocalDateTime(lastInviteDate); 
		
		Settings settings = settingsService.getSettings();
		
		NetStatus opponentStatus = opponent.getNetStatus();
		int timeoutMin = 0;
		if (opponentStatus.equals(NetStatus.ONLINE)) {
			timeoutMin = settings.getNextInviteActiveTimeout();
		}
		else if (opponentStatus.equals(NetStatus.SHADOW)) {
			timeoutMin = settings.getNextInviteShadowTimeout();
		}
		
		LocalDateTime now = LocalDateTime.now();
		long minutes = lastInviteLocalTime.until(now, ChronoUnit.MINUTES);
		logger.info("minutes since last invite: " + minutes + ", timeoutMin: " + timeoutMin + "for user: " + opponent.getUsername());
		if (minutes < timeoutMin) {
			return false;
		}
		else {
			return true;
		}
		
	}

	
	
}
