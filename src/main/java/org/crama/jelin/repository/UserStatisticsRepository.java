package org.crama.jelin.repository;

import java.util.List;

import org.crama.jelin.model.UserStatistics;

public interface UserStatisticsRepository {

	List<UserStatistics> getAllUsersStatistics();

}