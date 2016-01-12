package org.crama.jelin.service;

import java.util.List;

import org.crama.jelin.model.Answer;
import org.crama.jelin.model.GameRound;
import org.crama.jelin.model.Question;
import org.crama.jelin.model.QuestionResult;
import org.crama.jelin.repository.AnswerRepository;
import org.crama.jelin.repository.GameRepository;
import org.crama.jelin.repository.GameRoundRepository;
import org.crama.jelin.repository.QuestionResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("pointsCalculatorService")
public class PointsCalculatorServiceImpl implements PointsCalculatorService {

	@Autowired
	private AnswerRepository answerRepository;
	
	@Autowired
	private GameRepository gameRepository;
	
	@Autowired
	private GameRoundRepository gameRoundRepository;
	
	@Autowired
	private QuestionResultRepository questionResultRepository;
	
	@Override
	public void calculate(GameRound round) {
		
		for (Question question: round.getQuestions())
		{
			List<Answer> answers = answerRepository.getRoundAnswersByQuestion(round, question);
			int orderNumber = 0;
			for (Answer answer: answers)
			{
				QuestionResult result = calculatePoints(round, answer, orderNumber);
				questionResultRepository.saveResult(result);
				round.writeResult(result);
				orderNumber++;
			}
			
			gameRoundRepository.updateRound(round);
		}
		
		
		

	}
	
	private QuestionResult calculatePoints(GameRound round, Answer answer, int orderNumber)
	{
		int playerNumber = answer.getPlayerNumber();
		Question question = answer.getQuestion();
		if (answer.getTime() > question.getTime())
		{
			return new QuestionResult(answer.getVariant(), 0, question, 
							round, playerNumber);
		}
		
		int points = 0;
		int acrons = 0;
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
			
			default: break;
		}
		
		acrons = (int)(points*0.43); // where to save it? 
		
		QuestionResult result = new QuestionResult(answer.getVariant(), points, question, 
														round, playerNumber);
		return result;
	}

}