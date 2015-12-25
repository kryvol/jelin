package org.crama.jelin.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.crama.jelin.model.Game;
import org.crama.jelin.model.GameState;
import org.crama.jelin.model.User;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("gameInitRepository")
public class GameInitRepositoryImpl implements GameInitRepository {

	@Autowired
	private SessionFactory sessionFactory;
	
	public static final String GET_CREATED_GAME_SQL = "SELECT * FROM game " +
			"WHERE STATE_ID = ( " +
				"SELECT STATE_ID FROM gamestate " +
				"WHERE STATE = :state) " +
			"AND ID = ( " +
				"SELECT GAME_ID FROM gameuser " +
				"WHERE USER_ID = :userId);";
			
	public static final String GET_CREATED_GAME = "FROM Game " +
			"WHERE creator = :creator " +
			"AND gameState = :gameState";
	
	public static final String GET_GAME_STATE = "FROM GameState " +
			"WHERE state = :state ";
			
	@Override
	@Transactional
	public boolean saveGame(Game game) {
		
		Session session = sessionFactory.getCurrentSession();
		session.save(game);
		
		return true;
		
		/*Game game = new Game(theme, random);
		Session session = sessionFactory.getCurrentSession();
		
		try
		{	
			session.beginTransaction();
			session.save(game);
			session.getTransaction().commit();			
		}
		catch (HibernateException e) {
			e.printStackTrace();
			session.getTransaction().rollback();
			return false;
		}
		
		return true;*/
	}

	@Override
	@Transactional
	public boolean updateGame(Game game) {
		Session session = sessionFactory.getCurrentSession();		
		
		if (game == null) return false;
		
		session.update(game);
		return true;
	}
	/*@Override
	@Transactional
	public boolean updateDifficulty(int gameId, Difficulty difficulty) {
		Session session = sessionFactory.getCurrentSession();		
		
		Game game = (Game)session.get(Game.class, gameId);
		if (game == null) return false;
		System.out.println(difficulty);
		game.setDifficulty(difficulty);
		session.update(game);
		return true;
		
		try
		{	
			session.beginTransaction();
			Game game = (Game)session.get(Game.class, gameId);
			if (game == null) return false;
			System.out.println(difficulty);
			game.setDifficulty(difficulty);
			session.update(game);
			session.getTransaction().commit();			
		}
		catch (HibernateException e) {
			e.printStackTrace();
			session.getTransaction().rollback();
			return false;
		}
		
		return true;
	}*/

	@Override
	public Game getCreatedGame(User creator) {
		
		Query query = sessionFactory.getCurrentSession().createQuery(GET_CREATED_GAME);
		
		query.setParameter("creator", creator);
		GameState gs = getGameState(GameState.CREATED);
		query.setParameter("gameState", gs);
		Game game = (Game)query.uniqueResult();
		System.out.println(game);
		return game;
		
		/*Query query = sessionFactory.getCurrentSession().getNamedQuery("getCreatedGameSQL");
		query.setParameter("state", GameState.CREATED);
		query.setParameter("userId", creator.getId());
		Game game = (Game)query.uniqueResult();
		System.out.println(game);
		return game;*/
		
	}

	@Override
	public GameState getGameState(String state) {
		Query query = sessionFactory.getCurrentSession().createQuery(GET_GAME_STATE);
		query.setParameter("state", state);
		GameState gameState = (GameState)query.uniqueResult();
		return gameState;
	}


}