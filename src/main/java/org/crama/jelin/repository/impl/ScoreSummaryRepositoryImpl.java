package org.crama.jelin.repository.impl;

import java.util.List;

import org.crama.jelin.model.Game;
import org.crama.jelin.model.ScoreSummary;
import org.crama.jelin.model.User;
import org.crama.jelin.model.Constants.NetStatus;
import org.crama.jelin.model.Constants.ProcessStatus;
import org.crama.jelin.repository.ScoreSummaryRepository;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository("scoreSummaryRepository")
public class ScoreSummaryRepositoryImpl implements ScoreSummaryRepository {

	@Autowired
	private SessionFactory sessionFactory;
	
	@Override
	@Transactional
	public void saveSummary(ScoreSummary summary) {
		Session session = sessionFactory.getCurrentSession();	
		session.save(summary);

	}
	
	@Override
	@Transactional
	public void updateSummary(ScoreSummary summary) {
		Session session = sessionFactory.getCurrentSession();	
		session.update(summary);

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ScoreSummary> getSummaryByGame(Game game) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(ScoreSummary.class);
		criteria.add(Restrictions.eq("game", game));
		
		return criteria.list();
	}
	
	@Override
	public ScoreSummary getSummaryByGameAndUser(Game game, User player) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(ScoreSummary.class);
		criteria.add(Restrictions.eq("game", game));
		criteria.add(Restrictions.eq("user", player));
		
		ScoreSummary summary = (ScoreSummary) criteria.uniqueResult();
		return summary;
	}

}
