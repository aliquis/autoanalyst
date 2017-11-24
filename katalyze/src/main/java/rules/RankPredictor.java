package rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import model.*;

public class RankPredictor extends StateComparingRuleBase implements SolutionSubmittedEvent{
	final static ScoreTableComparer comparator = new ScoreTableComparer();
	
	final int rankThreshold;

	public RankPredictor(int rankThreshold) {
		this.rankThreshold = rankThreshold;
	}
	
	public void onSolutionSubmitted(StandingsAtSubmission standingsAtSubmission) {
		Contest contest = standingsAtSubmission.before.getContest();
		Standings standingsBefore = standingsAtSubmission.before;
		InitialSubmission submission = standingsAtSubmission.submission;
		
		Team team = submission.team;
		
		Score teamScore = standingsBefore.scoreOf(team);
		if (teamScore.isSolved(submission.getProblem())) {
			String message = "Despite already having solved it, {team} submitted a solution for {problem}";
			LoggableEvent event = new LoggableEvent(contest, message, EventImportance.Whatever, standingsAtSubmission.submission, null);
			notify(event);
			return;
		}
		ScoreTableEntry fakeScore = FakeScore.PretendProblemSolved(teamScore, submission.problem, submission.minutesFromStart);
		
		ArrayList<ScoreTableEntry> scoresAbove = new ArrayList<ScoreTableEntry>();

		int currentRank = standingsBefore.rankOf(team);
		
		for (ScoreTableEntry candidate : standingsBefore) {
			if (candidate != teamScore) {
				scoresAbove.add(candidate);
			} else {
				break;
			}
		}
		
		int fakeIndex = scoresAbove.size()-1;
		
		while (fakeIndex>=0 && comparator.compare(fakeScore, scoresAbove.get(fakeIndex))<=0) {
			fakeIndex--;
		}
		int potentialRank = fakeIndex+2;
		
		if (potentialRank <= rankThreshold) {
			String message = String.format("{team} submitted solution for {problem}. If correct, they will get rank %d (%d)",
					potentialRank, currentRank);
			Map<String, String> supplements = new HashMap<String, String>();
			supplements.put("currentRank", Integer.toString(currentRank));
			supplements.put("potentialRank", Integer.toString(potentialRank));
			LoggableEvent event = new LoggableEvent(contest, message, EventImportance.Normal, standingsAtSubmission.submission, supplements);
			notify(event);
		}
		
	}


}
