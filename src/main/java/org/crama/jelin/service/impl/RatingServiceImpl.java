package org.crama.jelin.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.crama.jelin.exception.GameException;
import org.crama.jelin.model.Constants;
import org.crama.jelin.model.SocialUser;
import org.crama.jelin.model.User;
import org.crama.jelin.model.UserDailyStats;
import org.crama.jelin.model.UserStatistics;
import org.crama.jelin.model.json.RatingJson;
import org.crama.jelin.repository.SocialUserRepository;
import org.crama.jelin.repository.UserDailyStatsRepository;
import org.crama.jelin.repository.UserStatisticsRepository;
import org.crama.jelin.service.HttpRequestService;
import org.crama.jelin.service.RatingService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("ratingService")
public class RatingServiceImpl implements RatingService {

	@Autowired 
	private HttpRequestService httpRequestService;
	
	@Autowired
	private UserStatisticsRepository userStatisticsRepository;
	
	@Autowired
	private UserDailyStatsRepository userDailyStatsRepository;
	
	@Autowired
	private SocialUserRepository socialUserRepository;
	
	@Override
	public List<RatingJson> getRating(User player, int time, int people) throws GameException {
		List<RatingJson> result = null;
		switch (people) 
		{
			case 0: if (time == 3)
					{
						result = getAllTimeRating(player, people);
					}
					else
					{
						result = getAllUsersRating(time, player);
					}
					break;
			case 1: if (time == 3)
					{
						result = getAllTimeRating(player, people);
					}
					else
					{
						result = getFriendsRating(player, time);
					
					}
					break;
			case 2: if (time == 3)
					{
						result = getAllTimeRating(player, people);
					}
					else
					{
						result = getFBRating(player, time);
					}
					break;
			case 3: if (time == 3)
					{
						result = getAllTimeRating(player, people);
					}
					else
					{
						result = getVKRating(player, time);
					}
					break;
		}
		
		return result;
	}

	
	private List<RatingJson> getFBRating(User player, int time) throws GameException {
		List<User> friends = getFBFriends(player);
		friends.add(player);
		Date fromDate = getStatsBeginingDate(time);
		
		List<RatingJson> rating = getRatingByTimeAndUsers(friends, fromDate, player);
		return rating;
	}

	private List<RatingJson> getVKRating(User player, int time) throws GameException {
		List<User> friends = getVKFriends(player);
		friends.add(player);
		Date fromDate = getStatsBeginingDate(time);
		
		List<RatingJson> rating = getRatingByTimeAndUsers(friends, fromDate, player);
		return rating;
	}

	private List<RatingJson> getFriendsRating(User player, int time) {
		List<User> friends = new ArrayList<User>();
		friends.addAll(player.getFriendList());
		friends.add(player);
		Date fromDate = getStatsBeginingDate(time);
		
		List<RatingJson> rating = getRatingByTimeAndUsers(friends, fromDate, player);
		return rating;
	}

	private List<RatingJson> getAllTimeRating(User player, int people) throws GameException {
		
		List<RatingJson> rating = null;
		List<User> friends = null;
		
		switch(people)
		{
			case 0: rating = getRating(null, player);
					break;
			case 1: friends = new ArrayList<User>();
					friends.addAll(player.getFriendList());
					rating = getRating(friends, player);
					break;
			case 2: friends = getFBFriends(player);
					rating = getRating(friends, player);
					break;
			case 3: friends = getVKFriends(player);
					rating = getRating(friends, player);
					break;				
		}
						
		return rating;
	}
	
	private List<RatingJson> getAllUsersRating(int time, User player) {
		
		Date fromDate = getStatsBeginingDate(time);
		List<RatingJson> rating = null;
		if (fromDate == null)
		{
			rating = getRating(null, player);
		}
		else
		{
			rating = getRatingByTimeAndUsers(null, fromDate, player);
		}
		
		return rating;
	}
	
	private List<User> getFBFriends(User player) throws GameException
	{
		List<User> result = new ArrayList<User>();
		SocialUser su = socialUserRepository.getSocialUser(player);
		if (su == null || !su.getProviderId().equals(Constants.FB_PROVIDER_ID))
		{
			return null;
		}
		
		String requestURL = Constants.GRAPH_URL  + "me/friends?access_token=" + su.getAccessToken();
		String responseStr = httpRequestService.sendGetRequest(requestURL);
		JSONObject jsonResponse = new JSONObject(responseStr);
		JSONArray items = jsonResponse.getJSONArray("data");
		List<User> currentUsers = getGameUsersFromFBResponse(items);
		result.addAll(currentUsers);
		
		JSONObject paging = jsonResponse.getJSONObject("paging");
		
		while(paging.has("next"))
		{
			requestURL = paging.getString("next");
			responseStr = httpRequestService.sendGetRequest(requestURL);
			jsonResponse = new JSONObject(responseStr);
			
			items = jsonResponse.getJSONArray("data");
			currentUsers = getGameUsersFromFBResponse(items);
			result.addAll(currentUsers);
			
			paging = jsonResponse.getJSONObject("paging");
		}
				
		return result;
	}
	
	private List<User> getGameUsersFromFBResponse(JSONArray items)
	{
		List<User> result = new ArrayList<User>();
		for (int i = 0; i < items.length(); i++)
		{
			JSONObject friendObj = items.getJSONObject(i);
			String providerUserId = friendObj.getString("id");
			SocialUser friend = socialUserRepository.findByProviderIdAndProviderUserId(Constants.FB_PROVIDER_ID, providerUserId);
			if (friend == null)
			{
				continue;
			}
			else
			{
				result.add(friend.getUser());
			}
		}
		
		return result;
	}
	
	private List<User> getVKFriends(User player) throws GameException
	{
		List<User> result = new ArrayList<User>();
		SocialUser su = socialUserRepository.getSocialUser(player);
		if (su == null || !su.getProviderId().equals(Constants.VK_PROVIDER_ID))
		{
			return null;
		}
		
		String requestURL = String.format(Constants.VK_FRIENDS_URL, su.getProviderUserId(), su.getAccessToken());
		String responseStr = httpRequestService.sendGetRequest(requestURL);
		
		JSONObject jsonResponse = new JSONObject(responseStr);
		JSONArray items = jsonResponse.getJSONArray("response");
				
		for (int i = 0; i < items.length(); i++)
		{
			Long longProviderUserId = items.getLong(i);
			String providerUserId = longProviderUserId.toString();
			SocialUser friend = socialUserRepository.findByProviderIdAndProviderUserId(Constants.VK_PROVIDER_ID, providerUserId);
			if (friend == null)
			{
				continue;
			}
			else
			{
				result.add(friend.getUser());
			}
		}
		
		return result;
	}
	
	private Date getStatsBeginingDate(int time)
	{
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0); 
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
	    
		Date result = null;
		switch(time)
		{
			case 0: result = cal.getTime();
					break;
			case 1: cal.add(Calendar.DAY_OF_MONTH, -7);
					result = cal.getTime();
					break;
			case 2: cal.add(Calendar.MONTH, -1);
					result = cal.getTime();
					break;
			case 3: 
					break;
			
		}
		
		return result;
	}
	
	private List<RatingJson> getRatingByTimeAndUsers(List<User> users, Date fromDate, User user)
	{
		Map<User, Integer> userPointsMap = new HashMap<User, Integer>();
		List<UserDailyStats> dailyStats = null;
		if (users == null)
		{
			dailyStats = userDailyStatsRepository.getRatingByTime(fromDate);
		}
		else
		{
			dailyStats = userDailyStatsRepository.getRatingByTimeAndUsers(users, fromDate);
		}
		
		for (UserDailyStats dailyStat: dailyStats)	
		{
			User player = dailyStat.getUser();
			if (userPointsMap.containsKey(player))
			{
				userPointsMap.put(player, userPointsMap.get(player) + dailyStat.getPoints());
			}
			else
			{
				userPointsMap.put(player, dailyStat.getPoints());
			}
		}
		
		List<Map.Entry<User, Integer>> entries = new ArrayList<Map.Entry<User, Integer>>(
				userPointsMap.entrySet()
			);
			
		Collections.sort(
			    entries
			,   new Comparator<Map.Entry<User, Integer>>() {
			        public int compare(Map.Entry<User, Integer> a, Map.Entry<User, Integer> b) {
			            return Integer.compare(b.getValue(), a.getValue());
			        }
			    }
			);
		
		List<RatingJson> rating = new ArrayList<RatingJson>();
		int position = 1;
		for (Map.Entry<User, Integer> entry : entries)
		{
		    RatingJson ratingObj = new RatingJson(position, entry.getValue(), 
		    		entry.getKey().getUserInfo().getAvatar(), entry.getKey().getUsername());
		    rating.add(ratingObj);
		    position++;
		}
		
		List<RatingJson> result = this.excludeOver100(rating, user);
				
		return result;
	}
	
	private List<RatingJson> excludeOver100(List<RatingJson> userRating, User player)
	{
		List<RatingJson> ratings = new ArrayList<RatingJson>();
		boolean alreadyAdded = false;
		for (RatingJson rating: userRating)
		{
			if (alreadyAdded && rating.getNumber() >= 100)
			{
				break;
			}
			
			if (!alreadyAdded && rating.getNumber() >= 100 && rating.getUsername() == player.getUsername())
			{
				ratings.add(rating);
				alreadyAdded = true;
			}
			else if (!alreadyAdded && rating.getNumber() >= 100 && rating.getUsername() != player.getUsername())
			{
				continue;
			}
			
			if (rating.getNumber() < 100)
			{
				ratings.add(rating);
				if (rating.getUsername() == player.getUsername())
				{
					alreadyAdded = true;
				}
			}
						
		}
		
		return ratings;
	}
	
	private List<RatingJson> getRating(List<User> users, User player)
	{
		List<RatingJson> userRating = new ArrayList<RatingJson>();
		List<UserStatistics> userStatistics = null;
		if (users == null)
		{
			userStatistics = userStatisticsRepository.getAllUsersStatistics();
		}
		else
		{
			users.add(player);
			userStatistics = userStatisticsRepository.getUsersStatistics(users);
		}
		
		Collections.sort(userStatistics);
		
		RatingJson rating = null;
		for (int i = 0; i < userStatistics.size(); i++) {
			UserStatistics s = userStatistics.get(i);
			rating = new RatingJson(i + 1, s.getPoints(), 
					s.getUser().getUserInfo().getAvatar(), s.getUser().getUsername());
			userRating.add(rating);
		}
		
		List<RatingJson> result = this.excludeOver100(userRating, player);
		return result;
	}

}
