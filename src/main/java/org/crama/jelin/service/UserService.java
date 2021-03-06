package org.crama.jelin.service;

import java.util.List;

import org.crama.jelin.exception.GameException;
import org.crama.jelin.model.Constants.ProcessStatus;
import org.crama.jelin.model.Game;
import org.crama.jelin.model.GameBot;
import org.crama.jelin.model.SocialUser;
import org.crama.jelin.model.User;
import org.crama.jelin.model.UserModel;

public interface UserService {

	UserModel getUserModel(String username);

	boolean checkUsername(String username);

	boolean saveUser(UserModel model);

	boolean checkEmail(String email);

	User getUserByUsername(String username);
	
	List<User> getUsersShadowAndFree();
	
	List<User> getUsersOnlineAndFree();
	
	List<User> getUsersOnlineAndCalling(User exceptUser);

	void updateUserProcessStatus(User creator, ProcessStatus processStatus);

	User getUser(int userId);

	void checkUserAuthorized(User creator) throws GameException;

	boolean checkUserStatusIsEquals(User creator, ProcessStatus ps) throws GameException;

	List<User> getAllUsers(User user);
	
	User createBot(Game game, GameBot bot);

	void changeNetStatus(User user, int status) throws GameException;

	void changeOthersNetStatus(User user, int status) throws GameException;

	void createUsersInfoAndStatistics();

	void remindPassword(String access) throws GameException;

	User getPrincipal();

	void updateUser(User user);


}
