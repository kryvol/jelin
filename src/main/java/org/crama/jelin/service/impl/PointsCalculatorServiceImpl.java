package org.crama.jelin.service.impl;

import java.util.List;

import org.crama.jelin.model.Answer;
import org.crama.jelin.model.GameRound;
import org.crama.jelin.model.Question;
import org.crama.jelin.model.QuestionResult;
import org.crama.jelin.model.User;
import org.crama.jelin.repository.AnswerRepository;
import org.crama.jelin.repository.GameRoundRepository;
import org.crama.jelin.repository.QuestionResultRepository;
import org.crama.jelin.service.PointsCalculatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("pointsCalculatorService")
public class PointsCalculatorServiceImpl implements PointsCalculatorService {

	@Autowired
	private AnswerRepository answerRepository;
		
	@Autowired
	private QuestionResultRepository questionResultRepository;
	
	private QuestionResult calculatePoints(GameRound round, Answer answer, int orderNumber)
	{
		User player = answer.getPlayer();
		Question question = answer.getQuestion();
				
		int points = 0;
		switch(orderNumber)
		{
			case 0: points = 85;
					break;
			case 1: points = 78;
					break;
			case 2: points = 75;
					break;
			case 3: points = 72;
					break;
			
			default: points = 0; 
					 break;
		}
		
		QuestionResult result = new QuestionResult(answer.getVariant(), points, question, 
														round, player);
		
		return result;
	}

	@Override
	public void calculateQuestion(GameRound round, Question question) {
		List<Answer> answers = answerRepository.getRoundAnswersByQuestion(round, question);
		int orderNumber = 0;
		for (Answer answer: answers)
		{
			QuestionResult result = null;
			
			if (answer.getTime() > question.getTime() || 
					answer.getVariant() != question.getAnswer())
			{
				result = calculatePoints(round, answer, -1);
			}
			else
			{
				result = calculatePoints(round, answer, orderNumber);
				orderNumber++;
			}
			
			questionResultRepository.saveResult(result);
			round.writeResult(result);
			
		}
		
	}

}
