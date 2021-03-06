package org.crama.jelin.repository.impl;

import java.util.List;

import org.crama.jelin.controller.UserStatisticController;
import org.crama.jelin.model.Answer;
import org.crama.jelin.model.Game;
import org.crama.jelin.model.GameRound;
import org.crama.jelin.model.Question;
import org.crama.jelin.model.User;
import org.crama.jelin.repository.AnswerRepository;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository("answerRepository")
public class AnswerRepositoryImpl implements AnswerRepository {

	@Autowired
	private SessionFactory sessionFactory;
	
	@Override
	@Transactional
	public void saveAnswer(Answer answer) {
		Session session = sessionFactory.getCurrentSession();	
		session.save(answer);

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Answer> getRoundAnswers(GameRound round) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Answer.class);
		criteria.add(Restrictions.eq("round", round));
		
		return criteria.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Answer> getRoundAnswersByQuestion(GameRound round, Question question) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Answer.class);
		Criterion q = Restrictions.eq("question", question);
		Criterion r = Restrictions.eq("round", round);
		criteria.add(Restrictions.and(q, r));
		criteria.addOrder(Order.asc("time"));
		
		return criteria.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Answer> getAnswersByGame(Game game) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Answer.class);
		criteria.add(Restrictions.eq("game", game));
		
		return criteria.list();
	}

	@Override
	public void update(Answer answer) {
		Session session = sessionFactory.getCurrentSession();	
		session.update(answer);
		
	}

}
